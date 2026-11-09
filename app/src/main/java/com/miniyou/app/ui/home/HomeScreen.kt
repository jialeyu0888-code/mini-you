package com.miniyou.app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.BabySize
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.babySizeFor
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.CheckinItem
import com.miniyou.app.data.db.CheckupPlan
import com.miniyou.app.data.scheduleFor
import com.miniyou.app.data.shortCn
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.checkin.toggleCheckin
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.knowledge.KnowledgeTab
import com.miniyou.app.ui.open
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private data class HomeColors(
    val paper: Color,
    val ink: Color,
    val secondaryInk: Color,
    val accent: Color,
    val card: Color,
    val tip: Color,
    val peach: Color,
    val lavender: Color,
    val lavenderInk: Color,
    val outline: Color,
    val selected: Color,
)

private val LightHome = HomeColors(
    Color(0xFFFBF1E8), Color(0xFF562B1E), Color(0xFF775149), Color(0xFFA84135),
    Color(0xFFFFFAF7), Color(0xFFFFF5E4), Color(0xFFFFD0BE), Color(0xFFF2EAF9),
    Color(0xFF513471), Color(0xFFEBCFC0), Color(0xFFFFD9D4),
)
private val DarkHome = HomeColors(
    Color(0xFF211815), Color(0xFFFFE5D8), Color(0xFFE0BDB2), Color(0xFFFFAA9D),
    Color(0xFF30221E), Color(0xFF392B20), Color(0xFF542D23), Color(0xFF382A45),
    Color(0xFFEFDAFF), Color(0xFF624238), Color(0xFF613B38),
)
private val LocalHomeColors = compositionLocalOf { LightHome }
private val WarmShadow = Color(0xFFB88170).copy(alpha = 0.58f)
private val DarkIllustration = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.72f, 0.62f, 0.58f, 1f) })

@Composable
fun HomeScreen(nav: NavController) {
    val app = app()
    val settings by app.settings.state.collectAsState()
    val plans by app.checkupPlans.all.collectAsState(initial = emptyList())
    val items by app.db.checkin().items().collectAsState(initial = emptyList())
    val records by app.db.checkin().records().collectAsState(initial = emptyList())
    val layout = LocalWindowLayout.current
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkHome else LightHome
    val today = LocalDate.now()
    val info = settings.lmp?.let { PregnancyInfo(it, today) }
    val baby = info?.let { babySizeFor(it.weeks) }
    val nextCheckup = plans.firstOrNull { it.date >= today.toEpochDay() }
    val doneToday = remember(records, today) {
        records.filter { it.date == today.toEpochDay() }.map { it.itemId }.toSet()
    }
    var showBaby by rememberSaveable { mutableStateOf(false) }
    var showReminders by rememberSaveable { mutableStateOf(false) }
    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }

    CompositionLocalProvider(LocalHomeColors provides colors) {
        AppScaffold(containerColor = colors.paper) { pad ->
            val direction = LocalLayoutDirection.current
            Box(Modifier.fillMaxSize()) {
                if (!dark) {
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                        0f to Color(0xFFFFF3E7), 0.62f to Color(0xFFFCEEEB), 1f to Color(0xFFFFF7EA),
                    )))
                    PaperTexture(R.drawable.home_paper, Modifier.matchParentSize(), 0.32f)
                }
                Column(
                    Modifier.fillMaxSize().padding(
                        start = pad.calculateStartPadding(direction),
                        top = pad.calculateTopPadding(),
                        end = pad.calculateEndPadding(direction),
                        bottom = pad.calculateBottomPadding(),
                    ).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 1040.dp).fillMaxWidth()) {
                        HomeHeader(
                            greeting = "$greeting，${settings.nickname.ifBlank { "准妈妈" }}",
                            date = "${today.cn()}  ${today.weekdayCn()}",
                            showReminderDot = nextCheckup != null,
                            onReminders = { showReminders = true },
                            onSettings = { nav.navigate(Routes.SETTINGS) },
                        )
                        val overview: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                if (info != null) PregnancyHero(info, dark)
                                if (baby != null) BabyTip(baby) { showBaby = true }
                                NextCheckupCard(
                                    info, nextCheckup, today,
                                    onEdit = { nav.navigate(Routes.checkupPlanEdit(nextCheckup?.id ?: -1L)) },
                                    onPlans = { nav.navigate(Routes.CHECKUP_PLANS) },
                                    onSchedule = { nav.navigate(Routes.SCHEDULE) },
                                )
                            }
                        }
                        val daily: @Composable () -> Unit = {
                            DailyCheckins(
                                items, doneToday, onManage = { nav.navigate(Routes.CHECKIN) },
                                onToggle = { item -> scope.launch { toggleCheckin(app, item.id, today) } },
                            )
                        }
                        val margin = if (layout.expanded) 22.dp else 14.dp
                        if (layout.expanded) {
                            Row(Modifier.padding(horizontal = margin), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(Modifier.weight(1.08f)) { overview() }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    daily()
                                    HomeShortcuts(nav)
                                }
                            }
                        } else {
                            Column(Modifier.padding(horizontal = margin), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                overview()
                                daily()
                                HomeShortcuts(nav)
                            }
                        }
                        HomeMeadow(layout.expanded, dark)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        if (showBaby && baby != null) {
            AlertDialog(
                onDismissRequest = { showBaby = false },
                title = { Text("第 ${info?.weeks} 周的宝宝") },
                text = { Text("大小大约像${baby.fruit}。身长${baby.length}，体重${baby.weight}。\n\n${baby.tip}") },
                confirmButton = { TextButton(onClick = { showBaby = false }) { Text("知道了") } },
            )
        }
        if (showReminders) {
            AlertDialog(
                onDismissRequest = { showReminders = false },
                title = { Text("提醒通知") },
                text = {
                    Text(if (Reminders.canNotify(app)) "通知已开启。你可以在产检计划和每日打卡里查看提醒安排。"
                    else "通知权限尚未开启，可以前往设置开启。")
                },
                dismissButton = {
                    TextButton(onClick = {
                        showReminders = false
                        nav.navigate(Routes.CHECKUP_PLANS)
                    }) { Text("产检计划") }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showReminders = false
                        nav.navigate(Routes.SETTINGS)
                    }) { Text("去设置") }
                },
            )
        }
    }
}

@Composable
private fun HomeMeadow(expanded: Boolean, dark: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(if (expanded) 94.dp else 42.dp)) {
        val width = maxWidth
        val height = if (expanded) width * (121f / 849f) else 62.dp
        Image(
            painterResource(R.drawable.home_meadow), null,
            Modifier.align(Alignment.BottomCenter).offset(y = if (expanded) 0.dp else (-11).dp)
                .wrapContentSize(Alignment.BottomCenter, unbounded = true).size(width, height),
            contentScale = ContentScale.FillBounds, alpha = if (dark) 0.38f else 1f,
        )
    }
}

@Composable
private fun PaperTexture(@DrawableRes res: Int, modifier: Modifier, alpha: Float) {
    val texture = ImageBitmap.imageResource(res)
    val brush = remember(texture) { ShaderBrush(ImageShader(texture, TileMode.Mirror, TileMode.Mirror)) }
    Box(modifier.background(brush, alpha = alpha))
}

@Composable
private fun HomeText(
    text: String, size: Int, color: Color, modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal, maxLines: Int = Int.MAX_VALUE, center: Boolean = false,
) {
    Text(
        text, modifier,
        style = TextStyle(color = color, fontSize = size.sp, lineHeight = (size + 4).sp, fontWeight = weight),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
private fun HomeHeader(
    greeting: String, date: String, showReminderDot: Boolean,
    onReminders: () -> Unit, onSettings: () -> Unit,
) {
    val colors = LocalHomeColors.current
    val dark = isSystemInDarkTheme()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val artWidth = (maxWidth * 0.49f).coerceAtMost(218.dp)
        val artInset = (maxWidth * 0.064f).coerceAtMost(35.dp)
        Box(Modifier.matchParentSize()) {
            Image(
                painterResource(R.drawable.home_couple), null,
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = artInset)
                    .wrapContentSize(Alignment.BottomEnd, unbounded = true)
                    .size(artWidth, artWidth * (278f / 414f)),
                alpha = if (dark) 0.78f else 1f,
                colorFilter = if (dark) DarkIllustration else null,
            )
        }
        Column(Modifier.heightIn(min = 129.dp).padding(start = 24.dp, top = 8.dp, bottom = 17.dp)) {
            Image(homePainter(R.drawable.home_wordmark, R.drawable.home_wordmark_dark), "Mini YOU", Modifier.width(125.dp).height(35.dp))
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth(0.55f), verticalAlignment = Alignment.CenterVertically) {
                HomeText(greeting, 19, colors.ink, Modifier.weight(1f, fill = false), FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.width(4.dp))
                Image(painterResource(R.drawable.home_greeting_heart), null, Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            HomeText(date, 13, colors.secondaryInk, Modifier.fillMaxWidth(0.53f), maxLines = 2)
        }
        Row(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 7.dp)) {
            IconButton(onClick = onReminders) {
                Box {
                    Icon(Icons.Outlined.Notifications, "提醒通知", tint = colors.ink, modifier = Modifier.size(26.dp))
                    if (showReminderDot) {
                        Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFFF66D5D)))
                    }
                }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, "设置", tint = colors.ink, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun PregnancyHero(info: PregnancyInfo, dark: Boolean) {
    val colors = LocalHomeColors.current
    val shape = RoundedCornerShape(23.dp)
    val largeText = LocalDensity.current.fontScale > 1.18f
    Surface(Modifier.homeCard(shape), shape = shape, color = colors.peach) {
        BoxWithConstraints {
            val showArt = !largeText && maxWidth >= 300.dp
            if (!dark) {
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0xFFFFCDB9), Color(0xFFFFDED1)))))
                PaperTexture(R.drawable.home_peach, Modifier.matchParentSize(), 0.44f)
            }
            if (showArt) {
                val artWidth = maxWidth * 0.47f
                Image(
                    painterResource(R.drawable.home_ultrasound), null,
                    Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 7.dp).width(artWidth).aspectRatio(374f / 223f),
                    alpha = if (dark) 0.77f else 1f,
                    colorFilter = if (dark) DarkIllustration else null,
                )
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp)) {
                Surface(color = if (dark) colors.selected else Color(0xFFF2916C), shape = RoundedCornerShape(50)) {
                    HomeText(info.trimesterLabel, 12, if (dark) colors.ink else Color(0xFF572719),
                        Modifier.padding(horizontal = 12.dp, vertical = 3.dp), FontWeight.Medium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        append("孕 ")
                        withStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold)) { append("${info.weeks}") }
                        append(" 周 + ${info.days} 天")
                    },
                    Modifier.fillMaxWidth(if (showArt) 0.62f else 1f),
                    style = TextStyle(color = colors.ink, fontSize = 17.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
                )
                Text(
                    buildAnnotatedString {
                        append(if (info.daysToDue >= 0) "距离预产期还有 " else "已超过预产期 ")
                        withStyle(SpanStyle(color = colors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                            append("${kotlin.math.abs(info.daysToDue)}")
                        }
                        append(" 天")
                    },
                    Modifier.fillMaxWidth(if (showArt) 0.64f else 1f),
                    style = TextStyle(color = colors.ink, fontSize = 14.sp, lineHeight = 23.sp),
                )
                Spacer(Modifier.height(13.dp))
                LinearProgressIndicator(
                    progress = { info.progress },
                    modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50))
                        .border(1.dp, if (dark) colors.outline else Color(0xFFFFF5ED), RoundedCornerShape(50)),
                    color = if (dark) colors.accent else Color(0xFFF28582),
                    trackColor = if (dark) colors.outline else Color(0xFFFFF3E8), drawStopIndicator = {}, gapSize = 0.dp,
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HomeText("已陪伴 ${info.totalDays} 天", 12, colors.ink, Modifier.weight(1f))
                    HomeText("预产期 ${info.dueDate.cn()}", 12, colors.ink)
                }
            }
        }
    }
}

@Composable
private fun BabyTip(baby: BabySize, onOpen: () -> Unit) {
    val colors = LocalHomeColors.current
    val shape = RoundedCornerShape(18.dp)
    Surface(onClick = onOpen, modifier = Modifier.padding(horizontal = 7.dp).homeCard(shape), shape = shape, color = colors.tip) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(homePainter(R.drawable.home_bulb, R.drawable.home_bulb_dark), null, Modifier.size(38.dp))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                HomeText("宝宝现在大约像一个${baby.fruit} ${baby.emoji}", 13, colors.ink, weight = FontWeight.SemiBold)
                HomeText("身长${baby.length}，体重${baby.weight}", 12, colors.secondaryInk)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun NextCheckupCard(
    info: PregnancyInfo?, plan: CheckupPlan?, today: LocalDate,
    onEdit: () -> Unit, onPlans: () -> Unit, onSchedule: () -> Unit,
) {
    val colors = LocalHomeColors.current
    val entry = info?.let { scheduleFor(it.weeks) }
    val shape = RoundedCornerShape(21.dp)
    Surface(Modifier.homeCard(shape), shape = shape, color = colors.card) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(
                onClickLabel = "全部产检计划", role = Role.Button, onClick = onPlans,
            ), verticalAlignment = Alignment.CenterVertically) {
                Image(homePainter(R.drawable.home_medical, R.drawable.home_medical_dark), null, Modifier.size(57.dp))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (plan == null) {
                        HomeText("还没有下次产检安排", 14, colors.ink, weight = FontWeight.Bold)
                        HomeText("可以提前安排多次产检", 12, colors.secondaryInk)
                    } else {
                        val next = plan.date.toDate()
                        val days = ChronoUnit.DAYS.between(today, next)
                        HomeText("下次产检 · ${next.shortCn()} ${next.weekdayCn()}", 14, colors.ink, weight = FontWeight.Bold)
                        val details = listOf(plan.project, plan.hospital).filter(String::isNotBlank).joinToString(" · ")
                        if (details.isNotEmpty()) HomeText(details, 12, colors.secondaryInk, maxLines = 2)
                        HomeText(if (days == 0L) "今天产检，记得带好产检本" else "还有 $days 天，会提前一天提醒你",
                            12, colors.secondaryInk)
                    }
                }
                Spacer(Modifier.width(5.dp))
                Box(Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onEdit), contentAlignment = Alignment.Center) {
                    HomeText(if (plan == null) "添加安排" else "编辑安排", 12, colors.accent,
                        Modifier.clip(RoundedCornerShape(50)).background(colors.selected).padding(horizontal = 11.dp, vertical = 6.dp),
                        weight = FontWeight.Medium, maxLines = 1)
                }
            }
            if (entry != null) {
                Surface(onClick = onSchedule, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
                    color = colors.lavender) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        HomeText("本次产检计划", 12, colors.lavenderInk, weight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(homePainter(R.drawable.home_plan, R.drawable.home_plan_dark), null, Modifier.size(30.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f)) {
                                HomeText("${entry.range} · ${entry.title}", 12, colors.lavenderInk, weight = FontWeight.SemiBold)
                                HomeText(entry.items.take(2).joinToString("；"), 11, colors.secondaryInk, maxLines = 1)
                            }
                            Icon(Icons.Outlined.ChevronRight, "查看完整产检时间表", tint = colors.secondaryInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.homeCard(shape: RoundedCornerShape): Modifier =
    fillMaxWidth().shadow(5.dp, shape, clip = false, ambientColor = WarmShadow, spotColor = WarmShadow)

@Composable
private fun homePainter(@DrawableRes light: Int, @DrawableRes dark: Int) =
    painterResource(if (isSystemInDarkTheme()) dark else light)

@Composable
private fun DailyCheckins(
    items: List<CheckinItem>, doneToday: Set<Long>, onManage: () -> Unit, onToggle: (CheckinItem) -> Unit,
) {
    val colors = LocalHomeColors.current
    val shape = RoundedCornerShape(21.dp)
    Surface(Modifier.homeCard(shape), shape = shape, color = colors.card) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(homePainter(R.drawable.home_checkin, R.drawable.home_checkin_dark), null, Modifier.size(25.dp))
                Spacer(Modifier.width(6.dp))
                HomeText("今日打卡", 15, colors.ink, Modifier.weight(1f), FontWeight.Bold)
                Row(
                    Modifier.heightIn(min = 32.dp).clickable(role = Role.Button, onClick = onManage),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeText("${items.count { it.id in doneToday }}/${items.size}  ·  管理", 12, colors.accent)
                    Icon(Icons.Outlined.ChevronRight, null, tint = colors.accent, modifier = Modifier.size(15.dp))
                }
            }
            if (items.isEmpty()) {
                HomeText("在管理中添加打卡项", 12, colors.secondaryInk, Modifier.padding(bottom = 6.dp))
            } else {
                BoxWithConstraints {
                    val compact = items.size <= 4 && maxWidth >= 330.dp && LocalDensity.current.fontScale <= 1.1f &&
                        items.all { it.name.length <= 6 }
                    if (compact) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items.forEach { item ->
                                CheckinPill(item, item.id in doneToday, Modifier.weight(1f), fill = true) { onToggle(item) }
                            }
                        }
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items.forEach { item ->
                                CheckinPill(item, item.id in doneToday, Modifier) { onToggle(item) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckinPill(item: CheckinItem, done: Boolean, modifier: Modifier, fill: Boolean = false, onToggle: () -> Unit) {
    val colors = LocalHomeColors.current
    val active = if (isSystemInDarkTheme()) colors.selected else Color(0xFFF39890)
    Box(modifier.heightIn(min = 44.dp).toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center) {
        Surface(
            modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
            shape = RoundedCornerShape(50), color = if (done) active else colors.card,
            border = BorderStroke(1.dp, if (done) active else colors.outline),
        ) {
            Row(Modifier.padding(horizontal = if (fill) 5.dp else 7.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                HomeText(item.emoji, 17, colors.ink)
                Spacer(Modifier.width(3.dp))
                HomeText(item.name, 12, colors.ink, maxLines = 1)
                if (done) Icon(Icons.Default.Check, null, tint = colors.accent, modifier = Modifier.size(if (fill) 11.dp else 13.dp))
            }
        }
    }
}

private data class Shortcut(
    @DrawableRes val art: Int,
    val title: String,
    val detail: String,
    val route: String,
    val wash: Color? = null,
)

@Composable
private fun HomeShortcuts(nav: NavController) {
    val colors = LocalHomeColors.current
    val dark = isSystemInDarkTheme()
    val entries = listOf(
        Shortcut(R.drawable.home_shortcut_checkup, "产检记录", "AI识别产检单", Routes.CHECKUPS),
        Shortcut(R.drawable.home_shortcut_diary, "孕期日记", "记录心情点滴", Routes.DIARY),
        Shortcut(R.drawable.home_shortcut_shopping, "购物账本", "记开销、算预算", Routes.SHOPPING),
        Shortcut(R.drawable.home_shortcut_checkin, "每日打卡", "营养补充不漏掉", Routes.CHECKIN),
        Shortcut(R.drawable.home_shortcut_remedies, "缓解攻略", "孕反不适怎么办", Routes.knowledge(KnowledgeTab.REMEDIES)),
        Shortcut(R.drawable.home_shortcut_classes, "宝宝课堂", "提前学习育儿", Routes.knowledge(KnowledgeTab.CLASSES)),
        Shortcut(R.drawable.checkup_fetus, "孕肚记录", "记录每周的变化", Routes.BELLY, Color(0xFFFFD9E2)),
        Shortcut(R.drawable.diary_summary_heart, "我的攻略", "对我有效的方法", Routes.knowledge(KnowledgeTab.MINE), Color(0xFFE9D8F8)),
    )
    val rows = listOf(entries.take(4)) + entries.drop(4).chunked(2)
    Column(Modifier.fillMaxWidth().padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            val firstRow = row.size == 4
            Row(
                Modifier.fillMaxWidth().padding(start = if (firstRow) 0.dp else 34.dp, end = if (firstRow) 0.dp else 41.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { entry ->
                    Box(
                        Modifier.weight(1f).heightIn(min = if (firstRow) 85.dp else 82.dp)
                            .clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button) { nav.open(entry.route) },
                    ) {
                        ShortcutArt(entry, firstRow, dark, Modifier.align(Alignment.TopCenter))
                        Column(
                            Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = if (firstRow) 48.dp else 44.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            HomeText(entry.title, 13, colors.ink, weight = FontWeight.Bold, center = true)
                            HomeText(entry.detail, 10, colors.secondaryInk, center = true, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutArt(entry: Shortcut, firstRow: Boolean, dark: Boolean, modifier: Modifier = Modifier) {
    val artHeight = if (firstRow) 48.dp else 44.dp
    val wash = entry.wash
    if (wash != null) {
        val plate = if (dark) wash.copy(alpha = 0.28f) else wash
        Box(
            modifier.padding(top = 2.dp).size(artHeight - 4.dp)
                .clip(CircleShape)
                .background(plate)
                .border(1.dp, if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(entry.art), entry.title,
                Modifier.size(if (entry.title == "我的攻略") 24.dp else artHeight - 8.dp),
                alpha = if (dark) 0.92f else 1f,
            )
        }
    } else if (dark) {
        Box(modifier.height(artHeight).clip(RoundedCornerShape(13.dp))) {
            Image(
                painterResource(entry.art), null,
                Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).height(if (firstRow) 85.dp else 82.dp),
                alpha = 0.72f, colorFilter = DarkIllustration,
            )
        }
    } else {
        Image(painterResource(entry.art), null, modifier.height(if (firstRow) 85.dp else 82.dp))
    }
}

@Composable
internal fun HomeBottomBar(activeRoute: String = Routes.HOME, onNavigate: (String) -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkHome else LightHome
    val homeActive = activeRoute == Routes.HOME
    val diaryActive = activeRoute == Routes.DIARY
    val tabs = listOf(
        Triple(Routes.HOME, "首页", if (homeActive) R.drawable.home_tab_home else R.drawable.diary_tab_home_outline),
        Triple(Routes.CHECKUPS, "产检", R.drawable.home_tab_checkup),
        Triple(Routes.DIARY, "日记", if (diaryActive) R.drawable.diary_tab_diary_active else R.drawable.home_tab_diary),
        Triple(Routes.SHOPPING, "购物", R.drawable.home_tab_shopping),
        Triple(Routes.KNOWLEDGE, "百科", R.drawable.home_tab_knowledge),
    )
    Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = colors.card, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup().padding(horizontal = 5.dp)) {
            tabs.forEach { (route, label, icon) ->
                val active = route == activeRoute || activeRoute.startsWith("$route?")
                val tint = if (active) colors.accent else colors.secondaryInk
                Column(
                    Modifier.weight(1f).heightIn(min = 65.dp).selectable(active, role = Role.Tab) { onNavigate(route) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Box(Modifier.width(54.dp).height(31.dp).clip(RoundedCornerShape(50))
                        .background(if (active) colors.selected else Color.Transparent), contentAlignment = Alignment.Center) {
                        Image(painterResource(icon), null, Modifier.size(26.dp),
                            colorFilter = if (dark || (active && (route == Routes.CHECKUPS || route == Routes.KNOWLEDGE))) ColorFilter.tint(tint) else null)
                    }
                    HomeText(label, 11, tint, weight = if (active) FontWeight.Bold else FontWeight.Medium, center = true)
                }
            }
        }
    }
}
