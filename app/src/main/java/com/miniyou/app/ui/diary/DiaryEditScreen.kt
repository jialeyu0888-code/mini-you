package com.miniyou.app.ui.diary

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.miniyou.app.MiniYouApp
import com.miniyou.app.R
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.DiaryEntry
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.DeleteRecordButton
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.LocalWindowLayout
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

val DefaultDiaryTags = listOf("胎动", "孕吐", "产检", "散步", "美食", "失眠", "胎教", "家人", "宝宝用品", "运动")

class DiaryEditVM(private val app: MiniYouApp, private val id: Long, initialDate: Long) : ViewModel() {
    var date by mutableStateOf(if (initialDate >= 0) initialDate.toDate() else LocalDate.now())
    var mood by mutableStateOf(Moods.first().label)
    val tags = mutableStateListOf<String>()
    val customTags = mutableStateListOf<String>()
    var content by mutableStateOf("")
    val photos = mutableStateListOf<String>()
    private var existing by mutableStateOf<DiaryEntry?>(null)
    val canDelete get() = existing != null
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val entry = when {
                id > 0 -> app.db.diary().get(id)
                initialDate >= 0 -> app.db.diary().byDate(initialDate).firstOrNull()
                else -> null
            }
            if (entry != null) {
                loadEntry(entry)
            }
        }
    }

    private fun loadEntry(e: DiaryEntry) {
        existing = e
        date = e.date.toDate()
        mood = if (e.mood == "有点疲惫") "疲惫" else e.mood
        tags.clear()
        tags.addAll(e.tags)
        content = e.content
        photos.clear()
        photos.addAll(e.photos)
        customTags.clear()
        customTags.addAll(e.tags.filter { it !in DefaultDiaryTags })
    }

    fun selectDate(newDate: LocalDate, allEntries: List<DiaryEntry>) {
        date = newDate
        val dayEntry = allEntries.firstOrNull { it.date == newDate.toEpochDay() }
        if (dayEntry != null) {
            loadEntry(dayEntry)
        } else if (id <= 0) {
            existing = null
            mood = Moods.first().label
            tags.clear()
            content = ""
            photos.clear()
        }
    }

    fun toggleTag(t: String) {
        if (t in tags) tags.remove(t) else tags.add(t)
    }

    fun addPhotos(context: Context, uris: List<Uri>) = viewModelScope.launch {
        uris.forEach { u ->
            if (photos.size < 9) {
                ImageStore.importImage(context, u)?.let(photos::add)
            }
        }
    }

    fun runAi(context: Context, uris: List<Uri>) {
        aiLoading = true
        viewModelScope.launch {
            try {
                val info = app.settings.current.lmp?.let { PregnancyInfo(it, date) }
                val d = app.ai.parseDiary(uris, Moods.map { it.label }, info)
                mood = d.mood
                d.tags.forEach { t ->
                    if (t !in tags) tags.add(t)
                    if (t !in DefaultDiaryTags && t !in customTags) customTags.add(t)
                }
                content = if (content.isBlank()) d.content else content.trimEnd() + "\n\n" + d.content
                uris.forEach { u ->
                    if (photos.size < 9) {
                        ImageStore.importImage(context, u)?.let(photos::add)
                    }
                }
            } catch (e: AiException) {
                aiError = e.message
            } catch (e: Exception) {
                aiError = "识别失败：${e.message}"
            } finally {
                aiLoading = false
            }
        }
    }

    suspend fun save() {
        val base = existing ?: DiaryEntry(date = date.toEpochDay())
        app.db.diary().upsert(
            base.copy(
                date = date.toEpochDay(),
                mood = mood,
                tags = tags.toList(),
                content = content.trim(),
                photos = photos.toList(),
            ),
        )
    }

    suspend fun delete() {
        existing?.let { deleteDiaryEntry(app, it) }
    }
}

internal suspend fun deleteDiaryEntry(app: MiniYouApp, entry: DiaryEntry) {
    app.db.diary().delete(entry)
    entry.photos.filterNot { it.startsWith("preset:") }.forEach(ImageStore::delete)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditScreen(
    id: Long,
    date: Long,
    onDone: () -> Unit,
    nav: NavController? = null,
) {
    val app = app()
    val context = LocalContext.current
    val vm = viewModel(key = "diary-$id-$date") { DiaryEditVM(app, id, date) }
    val entries by app.db.diary().all().collectAsState(initial = emptyList())
    val settings by app.settings.state.collectAsState()
    val plans by app.checkupPlans.all.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkDiaryColors else LightDiaryColors

    var month by remember(vm.date) { mutableStateOf(YearMonth.from(vm.date)) }
    var addTag by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showReminders by rememberSaveable { mutableStateOf(false) }
    var viewingPhoto by remember { mutableStateOf<DiaryPhotoSelection?>(null) }

    val aiPick = rememberPhotoPicker(multiple = true, title = "选几张今天的照片，AI 帮你写日记") { vm.runAi(context, it) }
    val photoPick = rememberPhotoPicker(multiple = true, maxCount = 9, title = "添加照片（最多9张）") { vm.addPhotos(context, it) }
    val save: () -> Unit = { scope.launch { vm.save(); onDone() } }

    val byDay = remember(entries) { entries.groupBy { it.date } }
    val monthEntries = remember(entries, month) {
        entries.filter { YearMonth.from(it.date.toDate()) == month }
    }
    val info = settings.lmp?.let { PregnancyInfo(it, vm.date) }
    val gestationLabel = info?.gestationAt(vm.date)?.takeIf { it.isNotBlank() }?.let { "孕 $it 周" } ?: "孕 20+5 周"
    val hasUpcomingPlan = remember(plans) { plans.any { it.date >= LocalDate.now().toEpochDay() } }

    CompositionLocalProvider(LocalDiaryColors provides colors) {
        AppScaffold(containerColor = colors.paper) { pad ->
            val direction = LocalLayoutDirection.current
            Box(Modifier.fillMaxSize()) {
                if (!dark) {
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.verticalGradient(
                                0f to Color(0xFFFFF4E9),
                                0.55f to Color(0xFFFDF2EB),
                                1f to Color(0xFFFFF8EE),
                            ),
                        ),
                    )
                    DiaryPaperTexture(R.drawable.home_paper, Modifier.matchParentSize(), 0.28f)
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = pad.calculateStartPadding(direction),
                            top = pad.calculateTopPadding(),
                            end = pad.calculateEndPadding(direction),
                            bottom = pad.calculateBottomPadding(),
                        )
                        .consumeWindowInsets(pad)
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 880.dp).fillMaxWidth()) {
                        DiaryEditHeader(
                            dark = dark,
                            showReminderDot = true || hasUpcomingPlan,
                            canDelete = vm.canDelete,
                            onBack = onDone,
                            onDelete = { confirmDelete = true },
                            onReminders = { showReminders = true },
                            onSettings = { nav?.navigate(Routes.SETTINGS) },
                        )

                        val horizontalMargin = if (layout.expanded) 22.dp else 11.dp
                        Column(
                            Modifier.padding(horizontal = horizontalMargin),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DiaryNotebookBoard(
                                month = month,
                                selected = vm.date,
                                marks = byDay.mapValues { it.value.first().mood },
                                monthEntries = monthEntries,
                                dark = dark,
                                onSelect = { d -> vm.selectDate(d, entries) },
                                onMonth = { month = it },
                            )

                            DiaryEditFormCard(
                                vm = vm,
                                gestationLabel = gestationLabel,
                                dark = dark,
                                showExplicitDelete = id > 0 && vm.canDelete,
                                onOpenDatePicker = { showDatePicker = true },
                                onAddCustomTag = { addTag = true },
                                onAddPhotos = photoPick,
                                onAiWrite = aiPick,
                                onPreviewPhoto = { viewingPhoto = DiaryPhotoSelection(vm.photos.toList(), it) },
                                onDelete = { confirmDelete = true },
                                onSave = save,
                            )

                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = vm.date.toEpochDay() * 86_400_000L)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let {
                            val d = LocalDate.ofEpochDay(it / 86_400_000L)
                            vm.selectDate(d, entries)
                            month = YearMonth.from(d)
                        }
                        showDatePicker = false
                    }) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
            ) {
                DatePicker(state = pickerState, showModeToggle = true)
            }
        }

        if (addTag) {
            var text by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { addTag = false },
                title = { Text("添加标签") },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(8) },
                        singleLine = true,
                        placeholder = { Text("如：第一次胎动") },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val t = text.trim().removePrefix("#")
                        if (t.isNotBlank()) {
                            if (t !in vm.customTags && t !in DefaultDiaryTags) vm.customTags.add(t)
                            if (t !in vm.tags) vm.tags.add(t)
                        }
                        addTag = false
                    }) { Text("添加") }
                },
                dismissButton = { TextButton(onClick = { addTag = false }) { Text("取消") } },
            )
        }

        if (confirmDelete) {
            ConfirmDialog("删除日记", "删除后无法恢复，确定删除吗？", onDismiss = { confirmDelete = false }) {
                scope.launch { vm.delete(); onDone() }
            }
        }

        if (showReminders) {
            AlertDialog(
                onDismissRequest = { showReminders = false },
                title = { Text("提醒通知") },
                text = {
                    Text(
                        if (Reminders.canNotify(app)) "通知已开启。你可以在产检计划和每日打卡里查看提醒安排。"
                        else "通知权限尚未开启，可以前往设置开启。",
                    )
                },
                dismissButton = {
                    TextButton(onClick = {
                        showReminders = false
                        nav?.navigate(Routes.CHECKUP_PLANS)
                    }) { Text("产检计划") }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showReminders = false
                        nav?.navigate(Routes.SETTINGS)
                    }) { Text("去设置") }
                },
            )
        }

        if (vm.aiLoading) AiProgressDialog("AI 正在看图写日记…")
        vm.aiError?.let { MessageDialog("生成失败", it) { vm.aiError = null } }
        viewingPhoto?.let { selection -> DiaryPhotoViewer(selection) { viewingPhoto = null } }
    }
}

@Composable
private fun DiaryEditHeader(
    dark: Boolean,
    showReminderDot: Boolean,
    canDelete: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onReminders: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = LocalDiaryColors.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showArt = maxWidth >= 390.dp && LocalDensity.current.fontScale < 1.2f
        val artWidth = (maxWidth * 0.40f).coerceAtMost(195.dp)
        if (showArt) Box(Modifier.matchParentSize()) {
            Image(
                painter = painterResource(R.drawable.diary_edit_hero),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 58.dp)
                    .wrapContentSize(Alignment.BottomEnd, unbounded = true)
                    .size(artWidth, artWidth * (231f / 390f)),
                alpha = if (dark) 0.78f else 1f,
                colorFilter = if (dark) DiaryDarkIllustration else null,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp)
                .padding(start = 20.dp, end = if (showArt) artWidth + 58.dp else 16.dp, bottom = 2.dp),
        ) {
            Image(
                painter = painterResource(if (dark) R.drawable.home_wordmark_dark else R.drawable.home_wordmark),
                contentDescription = "Mini YOU",
                modifier = Modifier.width(116.dp).height(30.dp),
            )
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(role = Role.Button, onClick = onBack),
            ) {
                Image(
                    painter = painterResource(R.drawable.diary_back_arrow),
                    contentDescription = "返回",
                    modifier = Modifier.size(19.dp),
                    colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
                )
                Spacer(Modifier.width(6.dp))
                DiaryText("孕期日记", 19, colors.ink, weight = FontWeight.Bold)
            }
            Spacer(Modifier.height(1.dp))
            DiaryText("写下今天的孕期日常 ♡", 11, colors.secondaryInk, maxLines = 2)
        }
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onReminders, modifier = Modifier.size(36.dp)) {
                Box {
                    Image(
                        painter = painterResource(R.drawable.diary_top_bell),
                        contentDescription = "提醒通知",
                        modifier = Modifier.size(25.dp),
                        colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
                    )
                    if (showReminderDot && dark) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF66D5D)),
                        )
                    }
                }
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
                Image(
                    painter = painterResource(R.drawable.diary_top_settings),
                    contentDescription = "设置",
                    modifier = Modifier.size(25.dp),
                    colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
                )
            }
        }
    }
}

@Composable
private fun DiaryNotebookBoard(
    month: YearMonth,
    selected: LocalDate,
    marks: Map<Long, String>,
    monthEntries: List<DiaryEntry>,
    dark: Boolean,
    onSelect: (LocalDate) -> Unit,
    onMonth: (YearMonth) -> Unit,
) {
    val colors = LocalDiaryColors.current
    val boardShape = RoundedCornerShape(22.dp)
    val happyDays = remember(monthEntries) { monthEntries.filter { it.mood == "开心" }.map { it.date }.distinct().size }
    val blissDays = remember(monthEntries) { monthEntries.filter { it.mood == "幸福" }.map { it.date }.distinct().size }

    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Surface(
            modifier = Modifier.diaryCardShadow(boardShape),
            shape = boardShape,
            color = colors.board,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.65f)),
        ) {
            Box {
                if (!dark) {
                    DiaryPaperTexture(R.drawable.prenatal_paper, Modifier.matchParentSize(), 0.45f)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 9.dp, end = 9.dp, top = 9.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left inner calendar sheet
                    Surface(
                        modifier = Modifier.weight(1f).height(162.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.innerSheet,
                        border = BorderStroke(0.8.dp, colors.outline.copy(alpha = 0.5f)),
                    ) {
                        Box(Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
                            MonthCalendar(
                                month = month,
                                selected = selected,
                                marks = marks,
                                showSelectedRays = false,
                                highlightSaturdays = false,
                                compact = true,
                                onSelect = onSelect,
                                onMonth = onMonth,
                            )
                        }
                    }

                    // Right "本月小结" summary card
                    Surface(
                        modifier = Modifier.width(104.dp).height(162.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.innerSheet,
                        border = BorderStroke(0.8.dp, colors.outline.copy(alpha = 0.5f)),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(R.drawable.diary_summary_sprout),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    DiaryText("本月小结", 12, colors.ink, weight = FontWeight.Bold, maxLines = 1)
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.outline.copy(alpha = 0.6f)),
                                )
                                SummaryStatLine(R.drawable.diary_summary_pencil, "${monthEntries.size} 篇日记")
                                SummaryStatLine(R.drawable.diary_summary_smile, "开心 ${happyDays.coerceAtLeast(if (monthEntries.isNotEmpty()) 1 else 0)} 天")
                                SummaryStatLine(R.drawable.diary_summary_heart, "幸福 ${blissDays.coerceAtLeast(if (monthEntries.size >= 2) 1 else 0)} 天")
                            }
                            Image(
                                painter = painterResource(R.drawable.diary_summary_cat),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(60.dp),
                                contentScale = ContentScale.Fit,
                                alpha = if (dark) 0.84f else 1f,
                            )
                        }
                    }
                }
            }
        }

        // Top left and right binder rings
        Image(
            painter = painterResource(R.drawable.diary_ring_left),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp)
                .offset(y = (-8).dp)
                .size(16.dp, 29.dp),
        )
        Image(
            painter = painterResource(R.drawable.diary_ring_right),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 36.dp)
                .offset(y = (-8).dp)
                .size(16.dp, 29.dp),
        )
    }
}

@Composable
private fun SummaryStatLine(@DrawableRes icon: Int, label: String) {
    val colors = LocalDiaryColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(14.dp))
        DiaryText(label, 11, colors.ink, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaryEditFormCard(
    vm: DiaryEditVM,
    gestationLabel: String,
    dark: Boolean,
    showExplicitDelete: Boolean,
    onOpenDatePicker: () -> Unit,
    onAddCustomTag: () -> Unit,
    onAddPhotos: () -> Unit,
    onAiWrite: () -> Unit,
    onPreviewPhoto: (Int) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = Modifier.diaryCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Top Date & Gestation Week Row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onOpenDatePicker)
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DiaryText(
                        text = "${vm.date.cn()}  ${vm.date.weekdayCn()}",
                        size = 15,
                        color = colors.ink,
                        weight = FontWeight.Bold,
                    )
                }
                if (showExplicitDelete) {
                    DeleteRecordButton("删除日记", onClick = onDelete)
                    Spacer(Modifier.width(4.dp))
                }
                Surface(color = colors.pillBg, shape = RoundedCornerShape(50)) {
                    DiaryText(
                        text = gestationLabel,
                        size = 11,
                        color = colors.pillInk,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 2.5.dp),
                        weight = FontWeight.Bold,
                    )
                }
            }

            // Section 1: 今天的心情
            DiarySectionHeader(R.drawable.diary_sec_mood, "今天的心情")
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Moods.take(5).forEach { m ->
                        MoodGridCell(
                            mood = m,
                            selected = vm.mood == m.label,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.mood = m.label },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Moods.drop(5).forEach { m ->
                        MoodGridCell(
                            mood = m,
                            selected = vm.mood == m.label,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.mood = m.label },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            // Section 2: 标签（可多选）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.diary_sec_tag),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
                )
                Spacer(Modifier.width(5.dp))
                DiaryText("标签", 13, colors.ink, weight = FontWeight.Bold)
                Spacer(Modifier.width(3.dp))
                DiaryText("（可多选）", 11, colors.secondaryInk)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    DefaultDiaryTags.take(5).forEach { tag ->
                        DiaryTagChip(
                            label = "#$tag",
                            selected = tag in vm.tags,
                            onClick = { vm.toggleTag(tag) },
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    (DefaultDiaryTags.drop(5) + vm.customTags).distinct().forEach { tag ->
                        DiaryTagChip(
                            label = "#$tag",
                            selected = tag in vm.tags,
                            onClick = { vm.toggleTag(tag) },
                        )
                    }
                    DiaryCustomTagChip(onClick = onAddCustomTag)
                }
            }

            // Section 3: 写点什么
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.diary_sec_write),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
                )
                Spacer(Modifier.width(5.dp))
                DiaryText("写点什么", 13, colors.ink, Modifier.weight(1f), weight = FontWeight.Bold)
                // Keep subtle AI helper trigger for power users
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onAiWrite)
                        .semantics { contentDescription = "AI 起草" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AutoAwesome, "AI 起草", tint = colors.activeCoral.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.innerSheet,
                border = BorderStroke(1.dp, colors.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(horizontal = 11.dp, vertical = 6.dp)) {
                    BasicTextField(
                        value = vm.content,
                        onValueChange = { if (it.length <= 500) vm.content = it },
                        textStyle = TextStyle(
                            color = colors.ink,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                        ),
                        cursorBrush = SolidColor(colors.activeCoral),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        decorationBox = { inner ->
                            if (vm.content.isEmpty()) {
                                DiaryText(
                                    "今天散步时，好像第一次感受到了小小的胎动！\n肚子里轻轻的，像一条小鱼在游，特别神奇又幸福～",
                                    11,
                                    colors.secondaryInk.copy(alpha = 0.55f),
                                )
                            }
                            inner()
                        },
                    )
                    DiaryText(
                        text = "${vm.content.length} / 500",
                        size = 10,
                        color = colors.secondaryInk.copy(alpha = 0.78f),
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }

            // Section 4: 照片
            DiarySectionHeader(R.drawable.diary_sec_photo, "照片")
            DiaryPhotoRow(
                photos = vm.photos,
                onAdd = onAddPhotos,
                onRemove = { vm.photos.removeAt(it) },
                onPreview = onPreviewPhoto,
            )

            // Save Button with decorative rays and sprout icon
            Box(Modifier.fillMaxWidth()) {
                Surface(
                    onClick = onSave,
                    shape = RoundedCornerShape(50),
                    color = colors.activeCoral,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                        .heightIn(min = 48.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.diary_save_sprout),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp, 16.dp),
                        )
                        DiaryText("保存日记", 15, colors.onActiveCoral, weight = FontWeight.Bold)
                    }
                }
                Image(
                    painter = painterResource(R.drawable.diary_save_rays),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 44.dp)
                        .offset(y = (-7).dp)
                        .size(24.dp, 22.dp),
                )
            }
        }
    }
}

@Composable
private fun DiarySectionHeader(@DrawableRes icon: Int, title: String) {
    val colors = LocalDiaryColors.current
    val dark = isSystemInDarkTheme()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
        )
        Spacer(Modifier.width(6.dp))
        DiaryText(title, 14, colors.ink, weight = FontWeight.Bold)
    }
}

@Composable
private fun MoodGridCell(
    mood: Mood,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalDiaryColors.current
    val bg = if (selected) colors.lightPinkMark else colors.innerSheet
    val border = if (selected) {
        BorderStroke(1.6.dp, colors.activeCoral)
    } else {
        BorderStroke(1.dp, colors.outline)
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(13.dp),
        color = bg,
        border = border,
    ) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(mood.iconRes),
                contentDescription = mood.label,
                modifier = Modifier.size(22.dp, 19.dp),
            )
            DiaryText(
                text = mood.label,
                size = 10,
                color = colors.ink,
                weight = if (selected) FontWeight.Bold else FontWeight.Medium,
                center = true,
            )
        }
    }
}

@Composable
private fun DiaryTagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    val bg = if (selected) colors.activeCoral else colors.innerSheet
    val textColor = if (selected) colors.onActiveCoral else colors.ink
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, colors.outline),
        modifier = Modifier.height(22.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            DiaryText(
                text = label,
                size = 10,
                color = textColor,
                weight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.onActiveCoral,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

@Composable
private fun DiaryCustomTagChip(onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    val strokeColor = colors.activeCoral.copy(alpha = 0.65f)
    Box(
        Modifier
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = strokeColor,
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f), 0f),
                ),
            )
        }
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = colors.activeCoral,
                modifier = Modifier.size(11.dp),
            )
            DiaryText("自定义", 10, colors.ink)
        }
    }
}

@Composable
private fun DiaryPhotoRow(
    photos: List<String>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onPreview: (Int) -> Unit,
) {
    val colors = LocalDiaryColors.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(photos) { index, photo ->
            Box(Modifier.size(width = 72.dp, height = 64.dp)) {
                Surface(
                    onClick = { onPreview(index) },
                    shape = RoundedCornerShape(11.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, colors.outline),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 3.dp, end = 3.dp),
                ) {
                    DiaryPhotoImage(
                        photo = photo,
                        description = "日记照片 ${index + 1}/${photos.size}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7D5D55))
                        .clickable(role = Role.Button) { onRemove(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "移除照片",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }
        if (photos.size < 9) {
            item {
                val dashColor = colors.secondaryInk.copy(alpha = 0.45f)
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(width = 76.dp, height = 61.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.innerSheet)
                        .clickable(role = Role.Button, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRoundRect(
                            color = dashColor,
                            cornerRadius = CornerRadius(22f, 22f),
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                            ),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加照片",
                            tint = colors.ink,
                            modifier = Modifier.size(16.dp),
                        )
                        DiaryText("添加照片", 9, colors.secondaryInk, center = true)
                        DiaryText("（最多 9 张）", 8, colors.secondaryInk.copy(alpha = 0.85f), center = true)
                    }
                }
            }
        }
    }
}
