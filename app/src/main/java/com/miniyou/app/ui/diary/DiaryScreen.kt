package com.miniyou.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.DiaryEntry
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MainTopBar
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.PhotoStrip
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout
import java.time.LocalDate
import java.time.YearMonth

data class Mood(val label: String, val emoji: String)

val Moods = listOf(
    Mood("开心", "😊"), Mood("幸福", "🥰"), Mood("平静", "😌"), Mood("期待", "🤩"), Mood("疲惫", "😴"),
    Mood("难受", "🤢"), Mood("低落", "😢"), Mood("烦躁", "😤"), Mood("焦虑", "😰"),
)

fun moodEmoji(label: String) = Moods.firstOrNull { it.label == label }?.emoji ?: "📝"

@Composable
fun DiaryScreen(nav: NavController) {
    val app = app()
    val entries by app.db.diary().all().collectAsState(initial = emptyList())
    val settings by app.settings.state.collectAsState()
    val layout = LocalWindowLayout.current
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val selected = selectedDay.toDate()
    val byDay = remember(entries) { entries.groupBy { it.date } }
    val dayEntries = byDay[selectedDay].orEmpty()
    val monthEntries = entries.filter { YearMonth.from(it.date.toDate()) == month }
    val info = settings.lmp?.let { PregnancyInfo(it) }

    AppScaffold(
        topBar = { MainTopBar("孕期日记", "已记录 ${entries.size} 篇 · ${byDay.size} 天") },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.diaryEdit(date = selectedDay)) },
                icon = { Icon(Icons.Default.Edit, null) }, text = { Text("写日记") })
        },
    ) { pad ->
        val calendar: @Composable () -> Unit = {
            SoftCard {
                MonthCalendar(month, selected, byDay.mapValues { moodEmoji(it.value.first().mood) },
                    onSelect = { selectedDay = it.toEpochDay() }, onMonth = { month = it })
            }
        }
        val dayList: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${selected.cn()} ${selected.weekdayCn()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    info?.gestationAt(selected)?.takeIf { it.isNotBlank() }?.let { Pill("孕 $it 周") }
                }
                if (dayEntries.isEmpty()) {
                    SoftCard {
                        Text("这一天还没有日记", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { nav.navigate(Routes.diaryEdit(date = selectedDay)) }) { Text("✍️ 写一篇") }
                    }
                } else dayEntries.forEach { DiaryCard(it) { nav.navigate(Routes.diaryEdit(id = it.id)) } }
            }
        }
        if (layout.expanded) {
            Row(Modifier.padding(pad).fillMaxSize().padding(horizontal = layout.horizontalPadding)) {
                LazyColumn(Modifier.weight(0.46f).fillMaxHeight(), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { calendar() }
                    item { MonthMoodSummary(monthEntries) }
                }
                Spacer(Modifier.width(12.dp))
                VerticalDivider()
                Spacer(Modifier.width(12.dp))
                LazyColumn(Modifier.weight(0.54f).fillMaxHeight(), contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { dayList() }
                    monthTimeline(monthEntries.filter { it.date != selectedDay }, month) { nav.navigate(Routes.diaryEdit(id = it.id)) }
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(pad),
                contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { calendar() }
                item { dayList() }
                monthTimeline(monthEntries.filter { it.date != selectedDay }, month) { nav.navigate(Routes.diaryEdit(id = it.id)) }
                if (entries.isEmpty()) item { EmptyState("📔", "开始写第一篇孕期日记吧", "记录心情、胎动和生活里的小确幸，也可以选张照片让 AI 帮你起草") }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.monthTimeline(list: List<DiaryEntry>, month: YearMonth, onClick: (DiaryEntry) -> Unit) {
    if (list.isEmpty()) return
    item { Text("${month.monthValue} 月的其他日记", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
    items(list, key = { it.id }) { e -> DiaryCard(e, showDate = true) { onClick(e) } }
}

@Composable
private fun MonthMoodSummary(list: List<DiaryEntry>) {
    if (list.isEmpty()) return
    SoftCard {
        Text("本月心情", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            list.groupingBy { it.mood }.eachCount().entries.sortedByDescending { it.value }.forEach { (m, n) ->
                Pill("${moodEmoji(m)} $m × $n")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryCard(e: DiaryEntry, showDate: Boolean = false, onClick: () -> Unit) {
    SoftCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(moodEmoji(e.mood), fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (showDate) "${e.date.toDate().cn()} · ${e.mood}" else e.mood.ifBlank { "日记" }, style = MaterialTheme.typography.titleSmall)
                if (e.tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { e.tags.forEach { Pill("#$it") } }
                }
            }
        }
        if (e.content.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(e.content, style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
        if (e.photos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PhotoStrip(e.photos, size = 72.dp)
        }
    }
}

@Composable
fun MonthCalendar(month: YearMonth, selected: LocalDate, marks: Map<Long, String>, onSelect: (LocalDate) -> Unit, onMonth: (YearMonth) -> Unit) {
    val today = LocalDate.now()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonth(month.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上个月") }
            Text("${month.year} 年 ${month.monthValue} 月", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { onMonth(month.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下个月") }
        }
        Row {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        val offset = month.atDay(1).dayOfWeek.value - 1
        val cells = offset + month.lengthOfMonth()
        (0 until (cells + 6) / 7).forEach { row ->
            Row {
                (0 until 7).forEach { col ->
                    val dayNum = row * 7 + col - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            val mark = marks[date.toEpochDay()]
                            val isSel = date == selected
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else if (mark != null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .then(if (date == today && !isSel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                    .clickable { onSelect(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$dayNum", fontSize = 13.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                    if (mark != null) Text(mark, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
