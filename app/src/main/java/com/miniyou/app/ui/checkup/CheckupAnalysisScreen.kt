package com.miniyou.app.ui.checkup

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.CheckupReading
import com.miniyou.app.data.FetalMeasure
import com.miniyou.app.data.checkupReadings
import com.miniyou.app.data.cn
import com.miniyou.app.data.fetalPercentile
import com.miniyou.app.data.fetalReference
import com.miniyou.app.data.fetalReferenceDays
import com.miniyou.app.data.shortCn
import com.miniyou.app.data.toDate
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.ContentWidth
import java.util.Locale

private enum class AnalysisPage(val title: String, val subtitle: String) {
    Overview("产检分析", "从产检记录查看胎儿与妈妈变化"),
    Fetal("胎儿情况", "查看腹围、头围、股骨长和双顶径的变化"),
    Mother("妈妈变化", "查看体重、血压、胎心与产检节奏"),
}

@Composable
fun CheckupAnalysisScreen(nav: NavController) {
    val app = app()
    val recordsFlow = remember(app) { app.db.checkups().all() }
    val records by recordsFlow.collectAsState(initial = null)
    val settings by app.settings.state.collectAsState()
    val readings = remember(records, settings.lmpEpochDay) {
        checkupReadings(records.orEmpty(), settings.lmpEpochDay)
    }
    var page by rememberSaveable { mutableStateOf(AnalysisPage.Overview) }
    val back = {
        if (page == AnalysisPage.Overview) nav.popBackStack() else page = AnalysisPage.Overview
        Unit
    }
    BackHandler(page != AnalysisPage.Overview, onBack = back)

    PrenatalPage { padding ->
        ContentWidth(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AnalysisHeader(page, back)
                key(page) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when {
                            records == null -> AnalysisNote("正在读取产检记录")
                            readings.isEmpty() -> {
                                AnalysisNote("本次孕期暂无产检记录。保存产检数据后即可查看变化。")
                                AnalysisButton("记录产检", { nav.navigate(Routes.checkupEdit()) })
                            }
                            page == AnalysisPage.Overview -> AnalysisOverview(readings, onPage = { page = it })
                            page == AnalysisPage.Fetal -> FetalAnalysis(
                                readings,
                                onOpenRecord = { nav.navigate(Routes.checkupDetail(it)) },
                                onEditRecord = { nav.navigate(Routes.checkupEdit(it)) },
                                onSwitch = { page = AnalysisPage.Mother },
                            )
                            else -> MotherAnalysis(
                                readings,
                                onOpenRecord = { nav.navigate(Routes.checkupDetail(it)) },
                                onSwitch = { page = AnalysisPage.Fetal },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisHeader(page: AnalysisPage, back: () -> Unit) {
    val colors = LocalPrenatalColors.current
    Row(Modifier.padding(end = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = colors.ink) }
        Column {
            Text(page.title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 27.sp)
            Text(page.subtitle, color = colors.muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun AnalysisOverview(readings: List<CheckupReading>, onPage: (AnalysisPage) -> Unit) {
    val fetalVisit = readings.lastOrNull { it.fetal.isNotEmpty() }
    VisitBanner(fetalVisit ?: readings.last(), if (fetalVisit == null) "最近产检" else "最近胎儿测量")
    AnalysisTitle("选择查看", R.drawable.checkup_ic_plan)
    AnalysisCard {
        PanelHeading(AnalysisPage.Fetal, R.drawable.checkup_rec_scan, R.drawable.prenatal_week_ultrasound)
        StatGrid(FetalMeasure.entries.map {
            AnalysisStat(it.name, fetalVisit?.fetal?.get(it)?.let(::trendNumber), "cm")
        })
        val preview = FetalMeasure.entries.firstOrNull { measure -> readings.any { measure in it.fetal } }
        AnalysisSideBySide(side = { AnalysisButton("进入胎儿分析", { onPage(AnalysisPage.Fetal) }) }) {
            if (preview == null) AnalysisNote("还没有带单位的胎儿超声测量记录")
            else FetalTrend(readings, preview, showDetails = false)
        }
    }
    AnalysisCard {
        PanelHeading(AnalysisPage.Mother, R.drawable.checkup_ic_note, R.drawable.kb_hero_mom)
        StatGrid(motherStats(readings))
        val weights = readings.mapNotNull { item -> item.weight?.let { TrendPoint(item.record.date.toDouble(), it) } }
        AnalysisSideBySide(side = { AnalysisButton("进入妈妈分析", { onPage(AnalysisPage.Mother) }) }) {
            if (weights.isEmpty()) AnalysisNote("还没有体重记录")
            else CheckupTrendChart(listOf(TrendSeries("体重", analysisPink(), weights)), "kg", dateTicks(weights), Modifier.height(108.dp))
        }
    }
    AnalysisInfo(
        "分析说明",
        listOf("胎儿情况汇总产检单里的超声测量。", "妈妈变化会随本次孕期的产检记录更新。"),
    )
}

@Composable
private fun AnalysisSideBySide(side: @Composable () -> Unit, chart: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 370.dp && LocalDensity.current.fontScale <= 1.25f) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.68f)) { chart() }
                Column(Modifier.weight(0.32f)) { side() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                chart()
                side()
            }
        }
    }
}

@Composable
private fun FetalAnalysis(
    readings: List<CheckupReading>,
    onOpenRecord: (Long) -> Unit,
    onEditRecord: (Long) -> Unit,
    onSwitch: () -> Unit,
) {
    var measure by rememberSaveable { mutableStateOf(FetalMeasure.entries.firstOrNull { metric ->
        readings.any { metric in it.fetal }
    } ?: FetalMeasure.AC) }
    var bpdConfirmed by rememberSaveable { mutableStateOf(false) }
    val visit = readings.lastOrNull { it.fetal.isNotEmpty() }
    VisitBanner(visit ?: readings.last(), if (visit == null) "最近产检" else "最近胎儿测量")
    AnalysisTitle("四项核心指标", R.drawable.checkup_ic_plan)
    FetalMeasure.entries.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { item ->
                val value = visit?.fetal?.get(item)
                val canCompare = item != FetalMeasure.BPD || bpdConfirmed
                val standard = visit?.gestationDays?.takeIf { canCompare }?.let { fetalReference(item, it) }
                val score = value?.let { standard?.zScore(it) }
                AnalysisCard(Modifier.weight(1f), onClick = { measure = item }, tinted = true, dense = true) {
                    val colors = LocalPrenatalColors.current
                    val icon = when (item) {
                        FetalMeasure.AC -> Icons.Outlined.PregnantWoman
                        FetalMeasure.HC -> Icons.Outlined.Face
                        FetalMeasure.FL -> Icons.Outlined.Straighten
                        FetalMeasure.BPD -> Icons.Outlined.SwapHoriz
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, Modifier.size(27.dp), tint = analysisPink())
                        Column {
                            Text("${item.label} ${item.name}", color = colors.ink, fontWeight = FontWeight.Bold,
                                fontSize = 13.sp, lineHeight = 16.sp)
                            StatValue(value?.let(::trendNumber), "cm")
                        }
                    }
                    HorizontalDivider(color = colors.outline)
                    AnalysisNote(when {
                        score != null -> "${percentileLabel(score)}  ·  ${signed(score)} SD"
                        value != null && !canCompare -> "确认测量口径后对照"
                        else -> "参考百分位 —"
                    })
                }
            }
        }
    }
    AnalysisCard {
        AnalysisTitle("发育趋势图", R.drawable.checkup_ic_heart)
        val colors = LocalPrenatalColors.current
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FetalMeasure.entries.forEach { item ->
                val selected = item == measure
                Box(
                    Modifier.weight(1f)
                        .background(if (selected) analysisPink() else colors.pinkCard, RoundedCornerShape(50))
                        .selectable(selected = selected, role = Role.Tab, onClick = { measure = item })
                        .height(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.name, color = if (selected && !isSystemInDarkTheme()) Color.White else if (selected) colors.paper else colors.ink,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        if (measure == FetalMeasure.BPD && readings.any { measure in it.fetal }) {
            Row(
                Modifier.fillMaxWidth().toggleable(bpdConfirmed, role = Role.Checkbox, onValueChange = { bpdConfirmed = it }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = bpdConfirmed, onCheckedChange = null, modifier = Modifier.padding(10.dp))
                AnalysisNote("确认这些双顶径均按外缘到外缘测量，显示参考曲线和百分位。")
            }
        }
        key(measure) {
            FetalTrend(readings, measure, compare = measure != FetalMeasure.BPD || bpdConfirmed, onOpenRecord = onOpenRecord)
        }
        if (readings.none { measure in it.fetal }) {
            TextButton(onClick = { onEditRecord((visit ?: readings.last()).record.id) }) {
                Text("补录检查指标")
            }
        }
    }
    AnalysisInfo(
        "解读与说明",
        listOf(
            "孕周优先采用产检记录，缺失或格式无法识别时按末次月经推算。",
            "参考曲线采用 INTERGROWTH-21st 单胎标准，覆盖孕 14+0 至 40+0 周。",
            "百分位用于对照同孕周参考分布，需结合报告和医生解读。",
        ),
        showReference = true,
    )
    AnalysisButton("切换查看妈妈变化", onSwitch)
}

@Composable
private fun FetalTrend(
    readings: List<CheckupReading>,
    measure: FetalMeasure,
    compare: Boolean = measure != FetalMeasure.BPD,
    showDetails: Boolean = true,
    onOpenRecord: (Long) -> Unit = {},
) {
    val measured = readings.filter { measure in it.fetal }
    if (measured.isEmpty()) {
        AnalysisNote("暂无${measure.label}记录。可在产检记录的「检查指标」中填写名称、数值与 mm 或 cm 单位。")
        return
    }
    val byWeek = compare && measured.all { it.gestationDays != null } &&
        measured.any { reading -> reading.gestationDays?.let { it in fetalReferenceDays } == true }
    fun x(reading: CheckupReading) =
        if (byWeek) checkNotNull(reading.gestationDays).toDouble() else reading.record.date.toDouble()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = measured.lastOrNull { it.record.id == selectedId } ?: measured.last()
    val currentValue = selected.fetal.getValue(measure)
    val reference = if (byWeek) (fetalReferenceDays step 7).map { day ->
        TrendReference(day.toDouble(), checkNotNull(fetalReference(measure, day)))
    } else emptyList()
    val points = measured.map { TrendPoint(x(it), it.fetal.getValue(measure)) }.sortedBy { it.x }
    val ticks = if (byWeek) (14..34 step 5).map { (it * 7).toDouble() to "$it" } +
        (280.0 to "40 周") else dateTicks(points)
    CheckupTrendChart(
        listOf(TrendSeries(measure.label, analysisPink(), points)),
        "cm",
        ticks,
        modifier = if (showDetails) Modifier else Modifier.height(108.dp),
        reference = reference,
        selectedX = x(selected),
        onSelect = if (showDetails) ({ position -> selectedId = measured.last { x(it) == position }.record.id }) else null,
    )
    if (showDetails) {
        if (byWeek) {
            ChartLegend("记录值", "中位数与 ±2 SD 参考带")
        } else if (compare) {
            val reason = if (measured.any { it.gestationDays == null }) "部分记录缺少孕周" else "记录孕周超出参考范围"
            AnalysisNote("$reason，按检查日期绘制。")
        }
        val standard = selected.gestationDays?.takeIf { compare }?.let { fetalReference(measure, it) }
        val z = standard?.zScore(currentValue)
        StatGrid(
            listOf(
                AnalysisStat("选中记录", trendNumber(currentValue), "cm"),
                AnalysisStat("参考中位数", standard?.median?.let(::trendNumber), "cm"),
                AnalysisStat("参考百分位", z?.let(::percentileLabel)),
            ),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AnalysisNote(
                listOfNotNull(selected.record.date.toDate().cn(), selected.week, z?.let { "${signed(it)} SD" }).joinToString(" · "),
                Modifier.weight(1f),
            )
            TextButton(onClick = { onOpenRecord(selected.record.id) }) { Text("查看记录") }
        }
    }
}

@Composable
private fun MotherAnalysis(readings: List<CheckupReading>, onOpenRecord: (Long) -> Unit, onSwitch: () -> Unit) {
    val pink = analysisPink()
    val purple = if (isSystemInDarkTheme()) Color(0xFFD3A2FD) else Color(0xFF8050B6)
    fun series(label: String, color: Color, value: (CheckupReading) -> Double?) =
        TrendSeries(label, color, readings.mapNotNull { item ->
            value(item)?.let { TrendPoint(item.record.date.toDouble(), it) }
        })
    val weight = series("体重", pink, CheckupReading::weight)
    val systolic = series("收缩压", pink, CheckupReading::systolic)
    val diastolic = series("舒张压", purple, CheckupReading::diastolic)
    val heart = series("胎心", pink, CheckupReading::heartRate)
    val lastBp = readings.lastOrNull { it.systolic != null && it.diastolic != null }
    AnalysisCard(tinted = true, dense = true) {
        PanelHeading(AnalysisPage.Mother, R.drawable.checkup_ic_note, R.drawable.kb_hero_mom)
        StatGrid(motherStats(readings))
    }
    AnalysisCard(dense = true) {
        AnalysisTitle("体重变化趋势", R.drawable.checkup_ic_weight)
        val values = weight.points
        if (values.size >= 2) {
            AnalysisSideBySide(side = {
                TrendCallout("较上次记录", signed(values.last().value - values[values.lastIndex - 1].value), "kg")
            }) {
                PlotOrEmpty(listOf(weight), "kg", "暂无体重记录")
            }
        } else {
            PlotOrEmpty(listOf(weight), "kg", "暂无体重记录")
            if (values.isNotEmpty()) AnalysisNote("目前只有一条体重记录")
        }
    }
    AnalysisCard(dense = true) {
        AnalysisTitle("血压变化趋势", R.drawable.checkup_ic_drop)
        val pressurePlot: @Composable () -> Unit = {
            if (systolic.points.isNotEmpty() || diastolic.points.isNotEmpty()) ChartLegend("收缩压", "舒张压", purple)
            PlotOrEmpty(listOf(systolic, diastolic), "mmHg", "暂无血压记录")
        }
        if (lastBp == null) pressurePlot() else {
            AnalysisSideBySide(side = {
                TrendCallout("最近完整血压", checkNotNull(bloodPressure(lastBp)), "mmHg", lastBp.record.date.toDate().shortCn())
            }, chart = pressurePlot)
        }
    }
    AnalysisCard {
        AnalysisTitle("胎心与产检节奏", R.drawable.checkup_ic_calendar)
        if (heart.points.size == 1) {
            AnalysisNote("最近记录 ${trendNumber(heart.points.single().value)} 次/分")
        } else {
            PlotOrEmpty(listOf(heart), "次/分", "暂无胎心记录")
        }
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = (readings.size - 3).coerceAtLeast(0)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 4.dp),
        ) {
            items(readings, key = { it.record.id }) { item ->
                AnalysisCard(Modifier.width(138.dp), tinted = true, dense = true, onClick = { onOpenRecord(item.record.id) }) {
                    Text(item.record.checkupType.ifBlank { "产检记录" }, color = LocalPrenatalColors.current.ink,
                        fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    AnalysisNote(listOfNotNull(item.week, item.record.date.toDate().toString()).joinToString(" · "))
                    AnalysisNote("体重  ${item.weight?.let { "${trendNumber(it)} kg" } ?: "—"}")
                    AnalysisNote("血压  ${bloodPressure(item) ?: "—"}")
                    AnalysisNote("胎心  ${item.heartRate?.let { "${trendNumber(it)} 次/分" } ?: "—"}")
                    AnalysisNote("指标  ${item.record.indicators.size} 项")
                }
            }
        }
    }
    AnalysisInfo(
        "妈妈记录小结",
        listOf(
            "已记录 ${readings.size} 次产检、${weight.points.size} 次体重和 ${heart.points.size} 次胎心。",
            "体重变化以本次孕期的首次体重记录为基准。",
            "血压和胎心图展示已录入数值，缺失值留空。",
        ),
    )
    AnalysisButton("切换查看胎儿情况", onSwitch)
}

@Composable
private fun TrendCallout(label: String, value: String, unit: String, date: String? = null) {
    AnalysisCard(tinted = true, dense = true) {
        AnalysisNote(label)
        StatValue(value, unit, compact = true)
        if (date != null) AnalysisNote(date)
    }
}

@Composable
private fun PlotOrEmpty(series: List<TrendSeries>, unit: String, emptyMessage: String) {
    val points = series.flatMap { it.points }
    if (points.isEmpty()) {
        AnalysisNote(emptyMessage)
        return
    }
    var selectedDate by rememberSaveable { mutableStateOf<Double?>(null) }
    val selected = selectedDate?.takeIf { day -> points.any { it.x == day } }
    CheckupTrendChart(
        series, unit, dateTicks(points), Modifier.height(114.dp),
        selectedX = selected, onSelect = { selectedDate = it },
    )
    if (selected != null) {
        val values = series.mapNotNull { line ->
            line.points.lastOrNull { it.x == selected }?.let { "${line.label} ${trendNumber(it.value)} $unit" }
        }
        AnalysisNote((listOf(selected.toLong().toDate().cn()) + values).joinToString(" · "))
    }
}

private fun dateTicks(points: List<TrendPoint>): List<Pair<Double, String>> {
    val dates = points.map { it.x }.distinct().sorted()
    val span = dates.last() - dates.first()
    return dates.filterIndexed { index, date ->
        index == 0 || index == dates.lastIndex ||
            (date - dates[index - 1] >= span / 4 && dates.last() - date >= span / 5)
    }.map { it to it.toLong().toDate().shortCn() }
}

private fun bloodPressure(item: CheckupReading): String? {
    val systolic = item.systolic ?: return null
    val diastolic = item.diastolic ?: return null
    return "${trendNumber(systolic)}/${trendNumber(diastolic)}"
}

private fun motherStats(readings: List<CheckupReading>): List<AnalysisStat> {
    val weights = readings.mapNotNull { it.weight }
    return listOf(
        AnalysisStat("体重", weights.lastOrNull()?.let(::trendNumber), "kg"),
        AnalysisStat("血压", readings.asReversed().firstNotNullOfOrNull(::bloodPressure)),
        AnalysisStat("胎心", readings.lastOrNull { it.heartRate != null }?.heartRate?.let(::trendNumber), "次/分"),
        AnalysisStat("较首次体重", if (weights.size > 1) signed(weights.last() - weights.first()) else null, "kg"),
    )
}

private data class AnalysisStat(val label: String, val value: String?, val unit: String = "")

@Composable
private fun StatGrid(stats: List<AnalysisStat>) {
    val colors = LocalPrenatalColors.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rowSize = if (maxWidth < 330.dp || LocalDensity.current.fontScale > 1.25f) 2 else stats.size
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.chunked(rowSize).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEachIndexed { index, stat ->
                        Surface(
                            Modifier.weight(1f),
                            color = if (index % 2 == 0) colors.pinkCard else lerp(colors.pinkCard, colors.aiStart, 0.33f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.outline),
                        ) {
                            Column(Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
                                Text(stat.label, color = colors.ink, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                                StatValue(stat.value, stat.unit, compact = true)
                            }
                        }
                    }
                    repeat(rowSize - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StatValue(value: String?, unit: String, compact: Boolean = false) {
    val colors = LocalPrenatalColors.current
    val pink = analysisPink()
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = pink, fontWeight = FontWeight.Bold, fontSize = if (compact) 16.sp else 23.sp)) {
                append(value ?: "—")
            }
            if (unit.isNotBlank() && value != null) {
                withStyle(SpanStyle(color = colors.muted, fontSize = 10.sp)) { append(" $unit") }
            }
        },
        lineHeight = if (compact) 20.sp else 27.sp,
    )
}

@Composable
private fun VisitBanner(item: CheckupReading, label: String) {
    val colors = LocalPrenatalColors.current
    AnalysisCard(tinted = true) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PrenatalArt(R.drawable.checkup_fetus, Modifier.size(82.dp, 74.dp))
            Column(Modifier.weight(1f).padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = colors.muted, fontSize = 11.sp, lineHeight = 13.sp)
                Text(item.week?.let { "孕 $it" } ?: "孕周未记录", color = colors.ink, fontWeight = FontWeight.Bold,
                    fontSize = 21.sp, lineHeight = 24.sp)
                AnalysisNote(item.record.date.toDate().cn())
                if (item.record.hospital.isNotBlank()) AnalysisNote(item.record.hospital)
            }
            PrenatalArt(R.drawable.prenatal_week_ultrasound, Modifier.size(99.dp, 76.dp))
        }
    }
}

@Composable
private fun PanelHeading(page: AnalysisPage, @DrawableRes icon: Int, @DrawableRes art: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AnalysisTitle(page.title, icon)
            AnalysisNote(page.subtitle)
        }
        PrenatalArt(art, Modifier.size(98.dp, 56.dp))
    }
}

@Composable
private fun AnalysisTitle(title: String, @DrawableRes icon: Int) {
    val colors = LocalPrenatalColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PrenatalIcon(icon, Modifier.size(25.dp))
        Text(title, color = colors.ink, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AnalysisCard(
    modifier: Modifier = Modifier,
    tinted: Boolean = false,
    dense: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalPrenatalColors.current
    PrenatalCard(
        modifier,
        color = if (tinted) colors.pinkCard else lerp(colors.card, colors.pinkCard, 0.16f),
        onClick = onClick,
        border = BorderStroke(1.dp, colors.outline),
        contentPadding = PaddingValues(if (dense) 8.dp else 11.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (dense) 4.dp else 7.dp), content = { content() })
    }
}

@Composable
private fun AnalysisButton(label: String, onClick: () -> Unit) {
    val colors = LocalPrenatalColors.current
    val pink = analysisPink()
    val contentColor = if (isSystemInDarkTheme()) colors.paper else Color.White
    Surface(onClick = onClick, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .background(Brush.verticalGradient(listOf(lerp(pink, colors.accent, 0.28f), pink)))
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 22.sp)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = contentColor)
        }
    }
}

@Composable
private fun ChartLegend(first: String, second: String, secondColor: Color = LocalPrenatalColors.current.muted) {
    val colors = LocalPrenatalColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(first to analysisPink(), second to secondColor).forEach { (label, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(color, CircleShape))
                Text(label, color = colors.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AnalysisInfo(title: String, lines: List<String>, showReference: Boolean = false) {
    val colors = LocalPrenatalColors.current
    AnalysisCard(tinted = true, dense = true) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val showArt = maxWidth >= 365.dp && LocalDensity.current.fontScale <= 1.25f
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnalysisTitle(title, R.drawable.checkup_ic_note)
                    lines.forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                            PrenatalIcon(R.drawable.checkup_ic_check, Modifier.size(16.dp))
                            AnalysisNote(line, Modifier.weight(1f))
                        }
                    }
                }
                if (showArt) {
                    PrenatalArt(
                        if (showReference) R.drawable.kb_hero_mom else R.drawable.prenatal_week_clipboard,
                        Modifier.size(86.dp, 72.dp),
                    )
                }
            }
        }
        if (showReference) {
            Text("Papageorghiou 等，Lancet 2014 · doi:10.1016/S0140-6736(14)61490-2",
                color = colors.muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun AnalysisNote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = LocalPrenatalColors.current.muted, fontSize = 12.sp, lineHeight = 16.sp)
}

@Composable
private fun analysisPink(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFF9FB5) else Color(0xFFBF305A)

private fun signed(value: Double): String = String.format(Locale.ROOT, "%+.2f", value).trimEnd('0').trimEnd('.')

private fun percentileLabel(z: Double): String {
    val percentile = fetalPercentile(z)
    return when {
        percentile < 0.1 -> "<0.1%"
        percentile > 99.9 -> ">99.9%"
        else -> "第 ${String.format(Locale.ROOT, "%.1f", percentile)} 百分位"
    }
}
