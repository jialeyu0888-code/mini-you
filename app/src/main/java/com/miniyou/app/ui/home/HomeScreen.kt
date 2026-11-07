package com.miniyou.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.babySizeFor
import com.miniyou.app.data.cn
import com.miniyou.app.data.scheduleFor
import com.miniyou.app.data.shortCn
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.checkin.toggleCheckin
import com.miniyou.app.ui.knowledge.KnowledgeTab
import com.miniyou.app.ui.open
import com.miniyou.app.ui.theme.ContentWidth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val Paper = Color(0xFFFCF0E8)
private val Ink = Color(0xFF5C4038)
private val InkSoft = Color(0xFF8A6E66)
private val Rose = Color(0xFFE98998)
private val RoseDeep = Color(0xFFD46B7C)
private val CardWhite = Color(0xFFFFFBF8)
private val PeachCard = Color(0xFFFDE3D8)
private val Lavender = Color(0xFFE9DFF6)
private val LavenderInk = Color(0xFF5E4D78)

private data class Shortcut(val art: Int, val title: String, val detail: String, val route: String)

@Composable
fun HomeScreen(nav: NavController) {
    val app = com.miniyou.app.ui.components.app()
    val settings by app.settings.state.collectAsState()
    val checkups by app.db.checkups().all().collectAsState(initial = emptyList())
    val items by app.db.checkin().items().collectAsState(initial = emptyList())
    val records by app.db.checkin().records().collectAsState(initial = emptyList())
    val dark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val info = settings.lmp?.let { PregnancyInfo(it, today) }
    val nextCheckup = checkups.mapNotNull { it.nextCheckupDate }.filter { it >= today.toEpochDay() }.minOrNull()?.toDate()
    val doneToday = remember(records) { records.filter { it.date == today.toEpochDay() }.map { it.itemId }.toSet() }
    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }
    val name = settings.nickname.ifBlank { "准妈妈" }

    AppScaffold { pad ->
        ContentWidth(maxWidth = 560.dp, modifier = Modifier.padding(pad)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
            ) {
                Header(
                    greeting = greeting,
                    name = name,
                    date = "${today.cn()} ${today.weekdayCn()}",
                    dark = dark,
                    showDot = nextCheckup != null && ChronoUnit.DAYS.between(today, nextCheckup) <= 7,
                    onBell = { nav.open(Routes.CHECKUPS) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (info != null) PregnancyHero(info, dark)
                    info?.let { babySizeFor(it.weeks) }?.let { baby ->
                        Surface(onClick = { nav.navigate(Routes.SCHEDULE) }, shape = RoundedCornerShape(22.dp), color = CardWhite, shadowElevation = 0.dp) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(if (dark) R.drawable.home_bulb_dark else R.drawable.home_bulb), null, Modifier.size(36.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("宝宝现在大约像一个${baby.fruit}", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text("身长 ${baby.length}，体重 ${baby.weight}", color = InkSoft, fontSize = 12.sp)
                                }
                                Text("›", color = InkSoft, fontSize = 22.sp)
                            }
                        }
                    }
                    NextCheckup(nextCheckup, today, onAdd = { nav.navigate(Routes.checkupEdit()) }, onOpen = { nav.open(Routes.CHECKUPS) })
                    info?.let { scheduleFor(it.weeks) }?.let { entry ->
                        Surface(onClick = { nav.navigate(Routes.SCHEDULE) }, shape = RoundedCornerShape(22.dp), color = Lavender) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(if (dark) R.drawable.home_plan_dark else R.drawable.home_plan), null, Modifier.size(36.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("本次产检计划", color = LavenderInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text("${entry.range} · ${entry.title}", color = LavenderInk, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(entry.items.take(2).joinToString("；"), color = LavenderInk.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("›", color = LavenderInk, fontSize = 22.sp)
                            }
                        }
                    }
                    DailyCheckins(items, doneToday, onManage = { nav.navigate(Routes.CHECKIN) }) { item ->
                        scope.launch { toggleCheckin(app, item.id, today) }
                    }
                    Shortcuts(nav)
                }
                Image(
                    painterResource(R.drawable.home_meadow),
                    null,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(72.dp),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

@Composable
private fun Header(
    greeting: String,
    name: String,
    date: String,
    dark: Boolean,
    showDot: Boolean,
    onBell: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(168.dp)) {
        Image(
            painterResource(R.drawable.home_couple),
            null,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 8.dp).height(132.dp),
            contentScale = ContentScale.Fit,
        )
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(if (dark) R.drawable.home_wordmark_dark else R.drawable.home_wordmark),
                    "Mini YOU",
                    modifier = Modifier.height(32.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = onBell) { Icon(Icons.Outlined.Notifications, "提醒", tint = Ink) }
                    if (showDot) {
                        Box(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp).size(8.dp).clip(CircleShape).background(RoseDeep))
                    }
                }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "设置", tint = Ink) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 120.dp)) {
                Text("$greeting，$name", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Image(painterResource(R.drawable.home_greeting_heart), null, Modifier.size(22.dp))
            }
            Text(date, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PregnancyHero(info: PregnancyInfo, dark: Boolean) {
    Surface(shape = RoundedCornerShape(28.dp), color = PeachCard, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.padding(16.dp)) {
            Column(Modifier.padding(end = 108.dp)) {
                Surface(color = Color.White.copy(alpha = 0.7f), shape = RoundedCornerShape(50)) {
                    Text(info.trimesterLabel, color = RoseDeep, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("孕 ", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                    Text("${info.weeks}", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text(" 周 + ${info.days} 天", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                }
                val dueText = if (info.daysToDue >= 0) "距离预产期还有 " else "已超过预产期 "
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(dueText, color = InkSoft, fontSize = 14.sp)
                    Text("${kotlin.math.abs(info.daysToDue)}", color = RoseDeep, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(" 天", color = InkSoft, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { info.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Rose,
                    trackColor = Color.White.copy(alpha = 0.8f),
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("已陪伴 ${info.totalDays} 天", color = InkSoft, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("预产期 ${info.dueDate.cn()}", color = InkSoft, fontSize = 12.sp)
                }
            }
            Image(
                painterResource(R.drawable.home_ultrasound),
                null,
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = 8.dp).height(118.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun NextCheckup(next: LocalDate?, today: LocalDate, onAdd: () -> Unit, onOpen: () -> Unit) {
    Surface(onClick = onOpen, shape = RoundedCornerShape(22.dp), color = CardWhite) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.home_medical), null, Modifier.size(42.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (next != null) {
                    val days = ChronoUnit.DAYS.between(today, next)
                    Text("下次产检 · ${next.shortCn()} ${next.weekdayCn()}", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(if (days == 0L) "就是今天，记得带好产检本" else "还有 $days 天，会提前一天提醒你", color = InkSoft, fontSize = 12.sp)
                } else {
                    Text("还没有下次产检安排", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("可以提前安排多次产检", color = InkSoft, fontSize = 12.sp)
                }
            }
            if (next == null) {
                Surface(onClick = onAdd, shape = RoundedCornerShape(50), color = Color(0xFFF8D7DF)) {
                    Text("添加安排", color = RoseDeep, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyCheckins(
    items: List<com.miniyou.app.data.db.CheckinItem>,
    done: Set<Long>,
    onManage: () -> Unit,
    onToggle: (com.miniyou.app.data.db.CheckinItem) -> Unit,
) {
    Surface(shape = RoundedCornerShape(22.dp), color = CardWhite) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.home_checkin), null, Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("今日打卡", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("${done.count { id -> items.any { it.id == id } }}/${items.size}", color = RoseDeep, fontWeight = FontWeight.Bold)
                TextButton(onClick = onManage) { Text("管理 ›", color = InkSoft) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    val on = item.id in done
                    FilterChip(
                        selected = on,
                        onClick = { onToggle(item) },
                        label = { Text("${item.emoji} ${item.name}${if (on) " ✓" else ""}") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFFFF3EE),
                            selectedContainerColor = Rose,
                            selectedLabelColor = Color.White,
                            labelColor = Ink,
                        ),
                        border = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun Shortcuts(nav: NavController) {
    val main = listOf(
        Shortcut(R.drawable.home_shortcut_checkup, "产检记录", "AI识别产检单", Routes.CHECKUPS),
        Shortcut(R.drawable.home_shortcut_diary, "孕期日记", "记录心情点滴", Routes.DIARY),
        Shortcut(R.drawable.home_shortcut_shopping, "购物清单", "待产包不遗漏", Routes.SHOPPING),
        Shortcut(R.drawable.home_shortcut_checkin, "每日打卡", "营养补充不漏掉", Routes.CHECKIN),
    )
    val second = listOf(
        Shortcut(R.drawable.home_shortcut_remedies, "缓解攻略", "孕反不适怎么办", Routes.knowledge(KnowledgeTab.REMEDIES)),
        Shortcut(R.drawable.home_shortcut_classes, "宝宝课堂", "提前学习育儿", Routes.knowledge(KnowledgeTab.CLASSES)),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            main.forEach { ShortcutItem(it, Modifier.weight(1f)) { nav.open(it.route) } }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(0.56f), horizontalArrangement = Arrangement.SpaceEvenly) {
            second.forEach { ShortcutItem(it, Modifier.weight(1f)) { nav.open(it.route) } }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            TextButton(onClick = { nav.navigate(Routes.BELLY) }) { Text("孕肚记录", color = InkSoft) }
            TextButton(onClick = { nav.open(Routes.knowledge(KnowledgeTab.MINE)) }) { Text("我的攻略", color = InkSoft) }
        }
    }
}

@Composable
private fun ShortcutItem(item: Shortcut, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = onClick, color = Color.Transparent) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(item.art), item.title, Modifier.size(64.dp), contentScale = ContentScale.Fit)
                Text(item.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(item.detail, color = InkSoft, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}
