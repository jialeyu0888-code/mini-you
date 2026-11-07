package com.miniyou.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.HeroGradient
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(nav: NavController) {
    val app = app()
    val settings by app.settings.state.collectAsState()
    val checkups by app.db.checkups().all().collectAsState(initial = emptyList())
    val items by app.db.checkin().items().collectAsState(initial = emptyList())
    val records by app.db.checkin().records().collectAsState(initial = emptyList())
    val layout = LocalWindowLayout.current
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

    AppScaffold { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState())
                .padding(horizontal = layout.horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$greeting，${settings.nickname.ifBlank { "准妈妈" }}", style = MaterialTheme.typography.headlineSmall)
                    Text("${today.cn()} ${today.weekdayCn()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Outlined.Settings, "设置") }
            }

            val left: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (info != null) PregnancyHero(info)
                    NextCheckupCard(info, nextCheckup, today, onRecord = { nav.navigate(Routes.CHECKUPS) }, onSchedule = { nav.navigate(Routes.SCHEDULE) })
                }
            }
            val right: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SoftCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("今日打卡", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("${doneToday.count { id -> items.any { it.id == id } }}/${items.size}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { nav.navigate(Routes.CHECKIN) }) { Text("管理") }
                        }
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items.forEach { item ->
                                val done = item.id in doneToday
                                FilterChip(
                                    selected = done,
                                    onClick = { scope.launch { toggleCheckin(app, item.id, today) } },
                                    label = { Text("${item.emoji} ${item.name}${if (done) " ✓" else ""}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }
                    QuickEntries(nav)
                }
            }

            if (layout.expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { left() }
                    Box(Modifier.weight(1f)) { right() }
                }
            } else {
                left(); right()
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PregnancyHero(info: PregnancyInfo) {
    val baby = babySizeFor(info.weeks)
    Surface(shape = RoundedCornerShape(28.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.background(HeroGradient).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Surface(color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(50)) {
                        Text(info.trimesterLabel, color = Color.White, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("孕 ", color = Color.White, fontSize = 18.sp)
                        Text("${info.weeks}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        Text(" 周 + ${info.days} 天", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(
                        if (info.daysToDue >= 0) "距离预产期还有 ${info.daysToDue} 天" else "已超过预产期 ${-info.daysToDue} 天",
                        color = Color.White, style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (baby != null) {
                    Box(Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                        Text(baby.emoji, fontSize = 44.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { info.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color.White, trackColor = Color.White.copy(alpha = 0.35f), drawStopIndicator = {},
            )
            Spacer(Modifier.height(6.dp))
            Row {
                Text("已陪伴 ${info.totalDays} 天", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("预产期 ${info.dueDate.cn()}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
            }
            if (baby != null) {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("宝宝现在大约像一个${baby.fruit}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("身长 ${baby.length} · 体重 ${baby.weight}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("💡 ${baby.tip}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun NextCheckupCard(info: PregnancyInfo?, next: LocalDate?, today: LocalDate, onRecord: () -> Unit, onSchedule: () -> Unit) {
    SoftCard(onClick = onRecord) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🩺", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (next != null) {
                    val days = ChronoUnit.DAYS.between(today, next)
                    Text("下次产检 · ${next.shortCn()} ${next.weekdayCn()}", style = MaterialTheme.typography.titleMedium)
                    Text(if (days == 0L) "就是今天，记得带好产检本～" else "还有 $days 天，会提前一天提醒你",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("还没有下次产检安排", style = MaterialTheme.typography.titleMedium)
                    Text("拍下产检单，AI 自动识别复诊日期", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
        val entry = info?.let { scheduleFor(it.weeks) }
        if (entry != null) {
            Spacer(Modifier.height(10.dp))
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(14.dp), onClick = onSchedule) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("📅 ${entry.range} · ${entry.title}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(entry.items.take(2).joinToString("；"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("查看完整产检时间表 ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

private data class Entry(val emoji: String, val title: String, val sub: String, val color: Color, val route: String)

@Composable
private fun QuickEntries(nav: NavController) {
    val entries = listOf(
        Entry("📋", "产检记录", "AI 识别产检单", Color(0xFFFDE2E7), Routes.CHECKUPS),
        Entry("📔", "孕期日记", "记录心情点滴", Color(0xFFFFEBDD), Routes.DIARY),
        Entry("🛍️", "购物清单", "待产包不遗漏", Color(0xFFE6F4EE), Routes.SHOPPING),
        Entry("✅", "每日打卡", "叶酸钙片不漏吃", Color(0xFFECE7FB), Routes.CHECKIN),
        Entry("🌿", "缓解攻略", "孕期不适怎么办", Color(0xFFE3F1FB), Routes.knowledge(0)),
        Entry("👶", "准爸妈课堂", "新生儿照护", Color(0xFFFFF3D6), Routes.knowledge(1)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("常用功能", style = MaterialTheme.typography.titleMedium)
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { e ->
                    Surface(
                        onClick = { nav.navigate(e.route) },
                        color = e.color, shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(76.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(e.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(e.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF4A3439))
                                Text(e.sub, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A6266))
                            }
                        }
                    }
                }
            }
        }
    }
}
