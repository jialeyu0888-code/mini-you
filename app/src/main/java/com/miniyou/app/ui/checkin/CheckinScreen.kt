package com.miniyou.app.ui.checkin

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniyou.app.MiniYouApp
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.db.CheckinItem
import com.miniyou.app.data.db.CheckinRecord
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.AiFillCard
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.TimePickerDialog
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.minutesLabel
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.LocalWindowLayout
import com.miniyou.app.ui.theme.Mint
import kotlinx.coroutines.launch
import java.time.LocalDate

suspend fun toggleCheckin(app: MiniYouApp, itemId: Long, date: LocalDate) {
    val dao = app.db.checkin()
    val day = date.toEpochDay()
    if (dao.count(itemId, day) > 0) dao.deleteRecord(itemId, day) else dao.insertRecord(CheckinRecord(itemId, day))
}

/** 连续打卡天数：今天已打卡从今天往前数，否则从昨天往前数。 */
fun streakOf(days: Set<Long>, today: Long): Int {
    var d = if (today in days) today else today - 1
    var n = 0
    while (d in days) { n++; d-- }
    return n
}

private val emojiChoices = listOf("💊", "🌿", "🥛", "🐟", "☀️", "🩸", "🍊", "🥚", "🚶‍♀️", "💧", "🧘‍♀️", "🍎", "🦶", "📖")

@Composable
fun CheckinScreen(onBack: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val items by app.db.checkin().items().collectAsState(initial = emptyList())
    val records by app.db.checkin().records().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    val today = LocalDate.now()
    val byItem = remember(records) { records.groupBy { it.itemId }.mapValues { e -> e.value.map(CheckinRecord::date).toSet() } }
    var editing by remember { mutableStateOf<CheckinItem?>(null) }
    var deleting by remember { mutableStateOf<CheckinItem?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun ensureNotify() {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    AppScaffold(
        topBar = { BackTopBar("每日打卡", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editing = CheckinItem(id = 0, name = "", sortOrder = items.size) },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("新增打卡项") })
        },
    ) { pad ->
        val done = items.count { today.toEpochDay() in (byItem[it.id] ?: emptySet()) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(layout.gridColumns),
            contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SoftCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("${today.monthValue}月${today.dayOfMonth}日 ${today.weekdayCn()}", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("今日已完成 ", style = MaterialTheme.typography.titleMedium)
                        Text("$done", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(" / ${items.size}", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        if (items.isNotEmpty() && done == items.size) "太棒了，今天的营养都补充到位啦 🎉" else "坚持每天补充，宝宝和你一起变得更强壮～",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                val days = byItem[item.id] ?: emptySet()
                CheckinCard(
                    item = item, days = days, today = today,
                    onToggle = { scope.launch { toggleCheckin(app, item.id, today) } },
                    onEdit = { editing = item },
                )
            }
        }
    }

    editing?.let { item ->
        CheckinItemDialog(
            initial = item,
            onDismiss = { editing = null },
            onDelete = if (item.id != 0L) ({ deleting = item; editing = null }) else null,
            onSave = { saved ->
                editing = null
                if (saved.reminderEnabled) ensureNotify()
                scope.launch {
                    val id = app.db.checkin().upsertItem(saved)
                    val realId = if (saved.id == 0L) id else saved.id
                    Reminders.scheduleCheckin(context, saved.copy(id = realId))
                }
            },
        )
    }
    deleting?.let { item ->
        ConfirmDialog("删除打卡项", "确定删除「${item.name}」及其全部打卡记录吗？", onDismiss = { deleting = null }) {
            scope.launch {
                Reminders.cancelCheckin(context, item.id)
                app.db.checkin().deleteRecordsOf(item.id)
                app.db.checkin().deleteItem(item)
            }
        }
    }
}

@Composable
private fun CheckinCard(item: CheckinItem, days: Set<Long>, today: LocalDate, onToggle: () -> Unit, onEdit: () -> Unit) {
    val done = today.toEpochDay() in days
    val streak = streakOf(days, today.toEpochDay())
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(item.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.reminderEnabled) {
                        Icon(Icons.Outlined.Alarm, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" ${minutesLabel(item.reminderMinutes)}  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("连续 $streak 天 · 累计 ${days.size} 天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "编辑") }
        }
        if (item.note.isNotBlank()) {
            Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (6 downTo 0).forEach { back ->
                    val d = today.minusDays(back.toLong())
                    val hit = d.toEpochDay() in days
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(if (hit) Mint else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) { if (hit) Text("✓", color = Color.White, fontSize = 11.sp) }
                        Text(d.weekdayCn().takeLast(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (done) {
                OutlinedButton(onClick = onToggle, shape = RoundedCornerShape(20.dp)) { Text("已打卡 ✓") }
            } else {
                Button(onClick = onToggle, shape = RoundedCornerShape(20.dp)) { Text("打卡") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckinItemDialog(initial: CheckinItem, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (CheckinItem) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var note by remember { mutableStateOf(initial.note) }
    var remind by remember { mutableStateOf(initial.reminderEnabled) }
    var minutes by remember { mutableIntStateOf(initial.reminderMinutes) }
    var pickTime by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val pick = rememberPhotoPicker(title = "拍下药瓶或包装，自动识别") { uris ->
        scope.launch {
            loading = true
            try {
                val d = app.ai.parseCheckinItem(uris)
                if (d.name.isNotBlank()) name = d.name
                emoji = d.emoji
                if (d.note.isNotBlank()) note = d.note
                d.reminderMinutes?.let { minutes = it; remind = true }
            } catch (e: AiException) { error = e.message } catch (e: Exception) { error = "识别失败：${e.message}" }
            finally { loading = false }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "新增打卡项" else "编辑打卡项") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AiFillCard("AI 识图填写", "拍药瓶/包装，识别名称和用法", onClick = pick)
                OutlinedTextField(name, { name = it.take(16) }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf(emoji) + emojiChoices).distinct().forEach { e ->
                        Surface(
                            onClick = { emoji = e }, shape = CircleShape,
                            color = if (e == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ) { Text(e, fontSize = 20.sp, modifier = Modifier.padding(6.dp)) }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("备注（用法用量等）") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("每日提醒", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { pickTime = true }, enabled = remind, contentPadding = PaddingValues(0.dp)) {
                            Text("提醒时间 ${minutesLabel(minutes)}")
                        }
                    }
                    Switch(remind, { remind = it })
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除此打卡项") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(initial.copy(name = name.trim(), emoji = emoji, note = note.trim(), reminderEnabled = remind, reminderMinutes = minutes))
            }, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
    if (pickTime) TimePickerDialog(minutes, { pickTime = false }) { minutes = it; pickTime = false }
    if (loading) AiProgressDialog()
    error?.let { MessageDialog("识别失败", it) { error = null } }
}
