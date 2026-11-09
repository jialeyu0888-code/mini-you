package com.miniyou.app.ui.checkin

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.miniyou.app.ui.components.RecordActionsMenu
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
    BackHandler(enabled = editing != null) { editing = null }
    fun delete(item: CheckinItem) {
        scope.launch {
            app.db.checkin().deleteWithRecords(item)
            Reminders.cancelCheckin(context, item.id)
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun ensureNotify() {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val colors = com.miniyou.app.ui.checkup.prenatalColors()
    var celebrate by remember { mutableStateOf(false) }
    androidx.compose.runtime.CompositionLocalProvider(com.miniyou.app.ui.checkup.LocalPrenatalColors provides colors) {
    val editingItem = editing
    if (editingItem != null) {
        CheckinItemDialog(
            initial = editingItem,
            onDismiss = { editing = null },
            onDelete = if (editingItem.id != 0L) ({ deleting = editingItem; editing = null }) else null,
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
    } else {
    AppScaffold(containerColor = colors.paper, topBar = { BackTopBar("每日打卡", onBack, containerColor = colors.paper) }) { pad ->
        val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val weekDays = (0..6).map { weekStart.plusDays(it.toLong()).toEpochDay() }
        val finishedDays = weekDays.count { day -> items.any { day in (byItem[it.id] ?: emptySet()) } }
        Column(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text("坚持打卡", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("照顾好自己和宝宝", color = colors.muted, fontSize = 13.sp)
            }
            com.miniyou.app.ui.checkup.PrenatalCard(
                Modifier.padding(horizontal = 14.dp),
                color = colors.pinkCard,
            ) {
                Text("本周进度", color = colors.ink, fontSize = 14.sp)
                Text("$finishedDays / 7 天", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
                        .background(colors.accent.copy(alpha = 0.18f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth((finishedDays / 7f).coerceIn(0f, 1f)).height(8.dp)
                            .background(colors.accent),
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.gridColumns),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val days = byItem[item.id] ?: emptySet()
                    CheckinCard(
                        item = item, days = days, today = today,
                        onToggle = {
                            scope.launch {
                                val wasDone = today.toEpochDay() in days
                                toggleCheckin(app, item.id, today)
                                val allDone = !wasDone && items.all { candidate ->
                                    val set = byItem[candidate.id] ?: emptySet()
                                    candidate.id == item.id || today.toEpochDay() in set
                                }
                                if (allDone && items.isNotEmpty()) celebrate = true
                            }
                        },
                        onEdit = { editing = item },
                        onDelete = { delete(item) },
                    )
                }
            }
            com.miniyou.app.ui.checkup.PrenatalButton(
                "＋  新增打卡项",
                { editing = CheckinItem(id = 0, name = "", sortOrder = items.size) },
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
    }
    if (celebrate) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { celebrate = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = colors.card) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(colors.accent),
                        contentAlignment = Alignment.Center,
                    ) { Text("✓", color = colors.onAccent, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
                    Text("今日打卡完成", Modifier.padding(top = 12.dp), color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("太棒了，今天的营养都补充到位啦。", Modifier.padding(top = 6.dp, bottom = 16.dp), color = colors.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    com.miniyou.app.ui.checkup.PrenatalButton("知道了", onClick = { celebrate = false })
                }
            }
        }
    }
    }

    deleting?.let { item ->
        ConfirmDialog("删除打卡项", deleteCheckinMessage(item), onDismiss = { deleting = null }) {
            deleting = null
            delete(item)
        }
    }
}

private fun deleteCheckinMessage(item: CheckinItem) = "确定删除「${item.name}」及其全部打卡记录吗？"

@Composable
private fun CheckinCard(
    item: CheckinItem,
    days: Set<Long>,
    today: LocalDate,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = com.miniyou.app.ui.checkup.LocalPrenatalColors.current
    val done = today.toEpochDay() in days
    val streak = streakOf(days, today.toEpochDay())
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.accentSoft).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
            Text(item.emoji, fontSize = 20.sp)
        }
        Column(Modifier.padding(start = 10.dp).weight(1f).clickable(onClick = onEdit)) {
            Text(item.name, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "连续 $streak 天" + if (item.reminderEnabled) " · ${minutesLabel(item.reminderMinutes)}" else "",
                color = colors.muted, fontSize = 12.sp,
            )
        }
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(if (done) colors.accent else colors.card)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text("✓", color = colors.onAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            else Text("○", color = colors.accent, fontSize = 18.sp)
        }
        RecordActionsMenu(
            "打卡项", deleteCheckinMessage(item),
            onEdit = onEdit, onDelete = onDelete,
            modifier = Modifier.align(Alignment.Top), tint = colors.muted,
        )
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

    val colors = com.miniyou.app.ui.checkup.LocalPrenatalColors.current
    Column(Modifier.fillMaxSize().background(colors.paper).statusBarsPadding().navigationBarsPadding().imePadding()) {
        com.miniyou.app.ui.checkup.PrenatalHeader(if (initial.id == 0L) "新增打卡项" else "编辑打卡项", onDismiss)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    TextButton(onClick = onDelete) { Text("删除此打卡项", color = colors.danger) }
                }
        }
        com.miniyou.app.ui.checkup.PrenatalButton(
            "保存",
            {
                onSave(initial.copy(name = name.trim(), emoji = emoji, note = note.trim(), reminderEnabled = remind, reminderMinutes = minutes))
            },
            Modifier.padding(14.dp),
            enabled = name.isNotBlank(),
        )
    }
    if (pickTime) TimePickerDialog(minutes, { pickTime = false }) { minutes = it; pickTime = false }
    if (loading) AiProgressDialog()
    error?.let { MessageDialog("识别失败", it) { error = null } }
}
