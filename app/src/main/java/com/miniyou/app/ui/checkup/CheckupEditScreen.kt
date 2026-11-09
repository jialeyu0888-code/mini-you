package com.miniyou.app.ui.checkup

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniyou.app.MiniYouApp
import com.miniyou.app.R
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.db.Indicator
import com.miniyou.app.data.toDate
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.PhotoStrip
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch
import java.time.LocalDate

val IndicatorCategories = listOf("血常规", "尿常规", "B超", "生化", "血糖", "甲功", "其他")

private val CheckupProjectOptions = listOf(
    "常规产检", "早孕 B 超", "建档检查", "NT 检查", "唐氏筛查", "无创 DNA（NIPT）",
    "大排畸", "糖耐量试验（OGTT）", "小排畸", "胎心监护（NST）", "GBS 筛查", "产科 B 超",
    "血常规", "尿常规", "孕中期常规产检", "孕中期筛查", "四维彩超",
)

class CheckupEditVM(private val app: MiniYouApp, private val id: Long) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var hospital by mutableStateOf("")
    var week by mutableStateOf("")
    var type by mutableStateOf("")
    var weight by mutableStateOf("")
    var bpS by mutableStateOf("")
    var bpD by mutableStateOf("")
    var fhr by mutableStateOf("")
    val indicators = mutableStateListOf<Indicator>()
    var advice by mutableStateOf("")
    var nextDate by mutableStateOf<LocalDate?>(null)
    var nextNote by mutableStateOf("")
    var notes by mutableStateOf("")
    val photos = mutableStateListOf<String>()
    private var createdAt = System.currentTimeMillis()

    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)
    var aiSummary by mutableStateOf<String?>(null)
    var canDelete by mutableStateOf(false)
        private set

    private val info get() = app.settings.current.lmp?.let { PregnancyInfo(it) }

    init {
        if (id > 0) viewModelScope.launch {
            app.db.checkups().get(id)?.let { c ->
                date = c.date.toDate(); hospital = c.hospital; week = c.gestationalWeek; type = c.checkupType
                weight = c.weightKg; bpS = c.bpSystolic; bpD = c.bpDiastolic; fhr = c.fetalHeartRate
                indicators.addAll(c.indicators); advice = c.doctorAdvice; nextDate = c.nextCheckupDate?.toDate()
                nextNote = c.nextCheckupNote; notes = c.notes; photos.addAll(c.photos); createdAt = c.createdAt
                canDelete = true
            }
        } else {
            week = info?.gestationAt(date).orEmpty()
        }
    }

    fun onDateChange(d: LocalDate) {
        val auto = info?.gestationAt(date)
        date = d
        if (week.isBlank() || week == auto) week = info?.gestationAt(d).orEmpty()
    }

    fun addPhotos(context: Context, uris: List<Uri>) = viewModelScope.launch {
        uris.forEach { u -> ImageStore.importImage(context, u)?.let(photos::add) }
    }

    fun runAi(context: Context, uris: List<Uri>) {
        aiLoading = true
        viewModelScope.launch {
            try {
                val d = app.ai.parseCheckup(uris, info)
                d.date?.let { date = it }
                if (d.hospital.isNotBlank()) hospital = d.hospital
                week = d.gestationalWeek.ifBlank { info?.gestationAt(date) ?: week }
                if (d.checkupType.isNotBlank()) type = d.checkupType
                if (d.weightKg.isNotBlank()) weight = d.weightKg
                if (d.bpSystolic.isNotBlank()) bpS = d.bpSystolic
                if (d.bpDiastolic.isNotBlank()) bpD = d.bpDiastolic
                if (d.fetalHeartRate.isNotBlank()) fhr = d.fetalHeartRate
                if (d.indicators.isNotEmpty()) { indicators.clear(); indicators.addAll(d.indicators) }
                if (d.doctorAdvice.isNotBlank()) advice = d.doctorAdvice
                d.nextCheckupDate?.let { nextDate = it }
                if (d.nextCheckupNote.isNotBlank()) nextNote = d.nextCheckupNote
                uris.forEach { u -> ImageStore.importImage(context, u)?.let(photos::add) }
                val parts = buildList {
                    if (d.checkupType.isNotBlank()) add("1 个检查项目")
                    if (d.indicators.isNotEmpty()) add("${d.indicators.size} 项指标")
                }
                aiSummary = buildString {
                    if (parts.isEmpty()) append("已识别到产检单上的信息")
                    else append("已识别到 ").append(parts.joinToString("和 "))
                    append("，已自动为你填写相关信息。请检查确认后保存。")
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
        val c = Checkup(
            id = if (id > 0) id else 0, date = date.toEpochDay(), hospital = hospital.trim(), gestationalWeek = week.trim(),
            checkupType = type.trim(), weightKg = weight.trim(), bpSystolic = bpS.trim(), bpDiastolic = bpD.trim(),
            fetalHeartRate = fhr.trim(), indicators = indicators.toList(), doctorAdvice = advice.trim(),
            nextCheckupDate = nextDate?.toEpochDay(), nextCheckupNote = nextNote.trim(), notes = notes.trim(),
            photos = photos.toList(), createdAt = createdAt,
        )
        app.checkupPlans.saveCheckup(c)
    }

    suspend fun delete() {
        app.db.checkups().get(id)?.let { app.checkupPlans.deleteCheckup(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckupEditScreen(id: Long, onDone: () -> Unit, onDeleted: () -> Unit, openAi: Boolean = false) {
    val app = app()
    val context = LocalContext.current
    val colors = prenatalColors()
    CompositionLocalProvider(LocalPrenatalColors provides colors) {
    val vm = viewModel(key = "checkup-$id") { CheckupEditVM(app, id) }
    val hospitals by remember(app) { app.db.checkups().hospitals() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var aiOpened by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val aiPick = rememberPrenatalPhotoSheet("拍产检单 / 化验单 / B超单（可多张）") { vm.runAi(context, it) }
    val photoPick = rememberPrenatalPhotoSheet("添加产检单照片") { vm.addPhotos(context, it) }

    LaunchedEffect(openAi) {
        if (openAi && !aiOpened) {
            aiOpened = true
            aiPick()
        }
    }

    val save: () -> Unit = {
        if (vm.nextDate != null && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scope.launch { vm.save(); onDone() }
    }

        AppScaffold(containerColor = colors.paper) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding()) {
                Column(Modifier.fillMaxWidth().background(colors.paper)) {
                    PrenatalHeader(if (id > 0) "编辑产检记录" else "新增产检记录", onDone) {
                        if (vm.canDelete) PrenatalTextAction("删除记录", colors.muted) { confirmDelete = true }
                        PrenatalTextAction("保存", onClick = save)
                    }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (id <= 0) AiBanner(aiPick)
                    PrenatalSection("基本信息", R.drawable.checkup_ic_plan) {
                        if (layout.expanded) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrenatalDateField("检查日期", vm.date, { it?.let(vm::onDateChange) }, "请选择检查日期", Modifier.weight(1f), required = true)
                                PrenatalInput("孕周", vm.week, { vm.week = it }, "例如 20+5", Modifier.weight(1f))
                            }
                        } else {
                            PrenatalDateField("检查日期", vm.date, { it?.let(vm::onDateChange) }, "请选择检查日期", required = true)
                            PrenatalInput("孕周", vm.week, { vm.week = it }, "例如 20+5")
                        }
                        PrenatalMenuField("医院", vm.hospital, { vm.hospital = it }, "请选择医院", hospitals)
                        PrenatalMenuField("检查项目", vm.type, { vm.type = it }, "请选择检查项目", CheckupProjectOptions)
                    }
                    PrenatalSection("体征数据", R.drawable.checkup_ic_heart) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrenatalInput("体重 kg", vm.weight, { vm.weight = it }, "请输入体重", Modifier.weight(1f), numeric = true)
                            PrenatalInput("胎心 次/分", vm.fhr, { vm.fhr = it }, "请输入胎心率", Modifier.weight(1f), numeric = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrenatalInput("收缩压 mmHg", vm.bpS, { vm.bpS = it }, "请输入收缩压", Modifier.weight(1f), numeric = true)
                            PrenatalInput("舒张压 mmHg", vm.bpD, { vm.bpD = it }, "请输入舒张压", Modifier.weight(1f), numeric = true)
                        }
                    }
                    PrenatalSection("检查指标（${vm.indicators.size}）", R.drawable.checkup_ic_drop, action = {
                        PrenatalTextAction("＋ 添加") {
                            vm.indicators.add(Indicator())
                            editingIndex = vm.indicators.lastIndex
                        }
                    }) {
                        if (vm.indicators.isEmpty()) {
                            Text("拍产检单后会自动填入，也可以手动添加", color = colors.muted, fontSize = 12.sp)
                        } else {
                            vm.indicators.forEachIndexed { index, indicator ->
                                Surface(onClick = { editingIndex = index }, color = colors.field, shape = RoundedCornerShape(12.dp)) {
                                    IndicatorLineEdit(indicator, Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                                }
                            }
                        }
                    }
                    PrenatalSection("医生建议", R.drawable.checkup_ic_note) {
                        PrenatalInput("建议内容", vm.advice, { vm.advice = it }, "请输入医生建议", minLines = 3)
                    }
                    PrenatalSection("下次产检", R.drawable.checkup_ic_calendar) {
                        PrenatalDateField("下次产检日期", vm.nextDate, { vm.nextDate = it }, "请选择下次产检日期", allowClear = true)
                        PrenatalInput("下次检查项目 / 准备事项", vm.nextNote, { vm.nextNote = it }, "例如：空腹 8–12 小时，带上产检手册")
                    }
                    PrenatalSection("产检单照片", R.drawable.checkup_ic_photo) {
                        if (vm.photos.isNotEmpty()) PhotoStrip(vm.photos, onRemove = { vm.photos.remove(it) })
                        Surface(onClick = photoPick, color = colors.field, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("添加照片", Modifier.weight(1f), color = colors.muted, fontSize = 14.sp)
                                PrenatalArt(R.drawable.checkup_ai_camera, Modifier.size(42.dp, 34.dp))
                            }
                        }
                    }
                    PrenatalSection("备注", R.drawable.checkup_ic_note) {
                        PrenatalInput("备注", vm.notes, { vm.notes = it }, "想记下的其他事情", minLines = 2)
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

    editingIndex?.let { index ->
        if (index in vm.indicators.indices) {
            IndicatorDialog(
                initial = vm.indicators[index],
                onDismiss = {
                    if (vm.indicators[index].name.isBlank()) vm.indicators.removeAt(index)
                    editingIndex = null
                },
                onDelete = { vm.indicators.removeAt(index); editingIndex = null },
                onSave = { vm.indicators[index] = it; editingIndex = null },
            )
        }
    }
    if (confirmDelete) {
        PrenatalConfirmDialog("删除产检记录", DeleteCheckupMessage, onDismiss = { confirmDelete = false }) {
            scope.launch { vm.delete(); onDeleted() }
        }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在解析产检单…")
    vm.aiError?.let { MessageDialog("识别失败", it) { vm.aiError = null } }
    vm.aiSummary?.let { PrenatalSuccessDialog(it) { vm.aiSummary = null } }
    }
}

@Composable
private fun AiBanner(onClick: () -> Unit) {
    val colors = LocalPrenatalColors.current
    Surface(onClick = onClick, color = colors.aiStart, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            PrenatalArt(R.drawable.checkup_ai_camera, Modifier.size(54.dp, 42.dp))
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Text("AI 识图快速填写", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("拍产检单，自动识别日期、医院、指标、医生建议和下次产检日期", color = colors.muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun IndicatorLineEdit(indicator: Indicator, modifier: Modifier) {
    val colors = LocalPrenatalColors.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(indicator.name.ifBlank { "（未命名指标）" }, color = colors.ink, fontSize = 14.sp)
            Text(
                listOf(indicator.category, indicator.reference.takeIf { it.isNotBlank() }?.let { "参考 $it" }).filterNotNull().joinToString(" · "),
                color = colors.muted, fontSize = 11.sp,
            )
        }
        Text(
            "${indicator.value} ${indicator.unit}".trim().ifBlank { "填写" },
            color = if (indicator.flag.isNotBlank()) colors.danger else colors.accent,
            fontWeight = FontWeight.Bold, fontSize = 14.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberPrenatalPhotoSheet(title: String, onResult: (List<Uri>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val colors = LocalPrenatalColors.current
    val callback by rememberUpdatedState(onResult)
    var show by remember { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pending
        if (ok && path != null) callback(listOf(Uri.parse(path)))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) callback(uris)
    }
    if (show) {
        ModalBottomSheet(onDismissRequest = { show = false }, containerColor = colors.card) {
            Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { show = false }) { Icon(Icons.Default.Close, "关闭", tint = colors.muted) }
                }
                PrenatalArt(R.drawable.checkup_form_bunny, Modifier.align(Alignment.CenterHorizontally).size(96.dp, 78.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SheetChoice("拍照", Modifier.weight(1f)) {
                        show = false
                        val uri = ImageStore.newCameraUri(context)
                        pending = uri.toString()
                        runCatching { camera.launch(uri) }
                    }
                    SheetChoice("从相册选择", Modifier.weight(1f)) {
                        show = false
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }
        }
    }
    return { show = true }
}

@Composable
private fun SheetChoice(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalPrenatalColors.current
    Surface(onClick = onClick, modifier = modifier.height(64.dp), shape = RoundedCornerShape(16.dp), color = colors.accentSoft) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndicatorDialog(initial: Indicator, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (Indicator) -> Unit) {
    val colors = LocalPrenatalColors.current
    var category by remember { mutableStateOf(initial.category.ifBlank { "其他" }) }
    var name by remember { mutableStateOf(initial.name) }
    var value by remember { mutableStateOf(initial.value) }
    var unit by remember { mutableStateOf(initial.unit) }
    var reference by remember { mutableStateOf(initial.reference) }
    var flag by remember { mutableStateOf(initial.flag) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.card) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("检查指标", color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IndicatorCategories.forEach { FilterChip(category == it, { category = it }, label = { Text(it) }) }
                }
                PrenatalInput("名称", name, { name = it }, "例如 血红蛋白")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrenatalInput("结果", value, { value = it }, "118", Modifier.weight(1f))
                    PrenatalInput("单位", unit, { unit = it }, "g/L", Modifier.weight(1f))
                }
                PrenatalInput("参考范围", reference, { reference = it }, "110–150")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("" to "正常", "偏高" to "偏高", "偏低" to "偏低", "异常" to "异常").forEach { (key, label) ->
                        FilterChip(flag == key, { flag = key }, label = { Text(label) })
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null, tint = colors.danger)
                    Text("删除该指标", color = colors.danger)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = colors.muted) }
                    TextButton(
                        onClick = { onSave(Indicator(category, name.trim(), value.trim(), unit.trim(), reference.trim(), flag)) },
                        enabled = name.isNotBlank(),
                    ) { Text("确定", color = colors.accent, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
