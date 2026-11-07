package com.miniyou.app.ui.checkup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.shortCn
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MainTopBar
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout
import com.miniyou.app.ui.theme.Rose
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun CheckupListScreen(nav: NavController) {
    val app = app()
    val list by app.db.checkups().all().collectAsState(initial = null)
    val layout = LocalWindowLayout.current
    var selected by rememberSaveable { mutableLongStateOf(-1L) }
    val today = LocalDate.now()
    val next = list.orEmpty().mapNotNull { it.nextCheckupDate }.filter { it >= today.toEpochDay() }.minOrNull()?.toDate()

    AppScaffold(
        topBar = {
            MainTopBar("产检记录", "共 ${list?.size ?: 0} 次产检") {
                TextButton(onClick = { nav.navigate(Routes.SCHEDULE) }) {
                    Icon(Icons.Outlined.CalendarMonth, null); Text(" 产检时间表")
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.checkupEdit()) }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("记录产检") })
        },
    ) { pad ->
        val items = list ?: return@AppScaffold
        val open: (Checkup) -> Unit = { if (layout.expanded) selected = it.id else nav.navigate(Routes.checkupDetail(it.id)) }
        val listPane: @Composable (Modifier) -> Unit = { m ->
            LazyColumn(
                m,
                contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SoftCard(color = MaterialTheme.colorScheme.primaryContainer, onClick = { nav.navigate(Routes.SCHEDULE) }) {
                        if (next != null) {
                            val days = ChronoUnit.DAYS.between(today, next)
                            Text("⏰ 下次产检：${next.shortCn()} ${next.weekdayCn()}", style = MaterialTheme.typography.titleMedium)
                            Text(if (days == 0L) "就是今天！" else "还有 $days 天 · 前一天 20:00 和当天 8:00 会提醒你", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("📅 不知道什么时候该产检？", style = MaterialTheme.typography.titleMedium)
                            Text("点这里查看孕期产检时间表参考", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                val weights = items.filter { it.weightKg.toDoubleOrNull() != null }.sortedBy { it.date }
                if (weights.size >= 2) item { WeightChart(weights) }
                if (items.isEmpty()) {
                    item { EmptyState("📋", "还没有产检记录", "点击右下角「记录产检」，拍一张产检单，AI 帮你自动填好") }
                }
                items(items, key = { it.id }) { c -> CheckupCard(c, selected == c.id && layout.expanded) { open(c) } }
            }
        }
        if (layout.expanded) {
            Row(Modifier.padding(pad).fillMaxSize()) {
                listPane(Modifier.weight(0.45f).fillMaxHeight())
                VerticalDivider()
                Box(Modifier.weight(0.55f).fillMaxHeight()) {
                    val sel = items.firstOrNull { it.id == selected } ?: items.firstOrNull()
                    if (sel != null) CheckupDetailContent(sel, nav, onDeleted = { selected = -1 })
                    else EmptyState("👈", "选择一条记录", "在左侧选择产检记录查看详情")
                }
            }
        } else {
            listPane(Modifier.padding(pad))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckupCard(c: Checkup, highlighted: Boolean, onClick: () -> Unit) {
    val d = c.date.toDate()
    SoftCard(onClick = onClick, color = if (highlighted) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Text("${d.dayOfMonth}", fontSize = 26.sp, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("${d.year % 100}年${d.monthValue}月", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.checkupType.ifBlank { "常规产检" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(c.gestationalWeek.takeIf { it.isNotBlank() }?.let { "孕 $it 周" }, c.hospital.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (c.weightKg.isNotBlank()) Pill("体重 ${c.weightKg}kg")
                    if (c.bpSystolic.isNotBlank()) Pill("血压 ${c.bpSystolic}/${c.bpDiastolic}")
                    if (c.fetalHeartRate.isNotBlank()) Pill("胎心 ${c.fetalHeartRate}")
                    val abnormal = c.indicators.count { it.abnormal }
                    if (c.indicators.isNotEmpty()) Pill("${c.indicators.size} 项指标", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                    if (abnormal > 0) Pill("$abnormal 项异常", MaterialTheme.colorScheme.error.copy(alpha = 0.12f), MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun WeightChart(points: List<Checkup>) {
    val values = points.mapNotNull { it.weightKg.toDoubleOrNull() }
    val min = values.min(); val max = values.max()
    SoftCard {
        Row {
            Text("体重变化", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val diff = values.last() - values.first()
            Text("%s%.1f kg".format(if (diff >= 0) "+" else "", diff), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        val line = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val range = (max - min).takeIf { it > 0 } ?: 1.0
            val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
            val pts = values.mapIndexed { i, v -> Offset(i * stepX, (size.height - 12f - ((v - min) / range * (size.height - 24f))).toFloat()) }
            val path = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            drawPath(path, line, style = Stroke(width = 5f))
            pts.forEach { drawCircle(Rose, 8f, it) }
        }
        Row {
            Text(points.first().date.toDate().shortCn(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(points.last().date.toDate().shortCn(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
