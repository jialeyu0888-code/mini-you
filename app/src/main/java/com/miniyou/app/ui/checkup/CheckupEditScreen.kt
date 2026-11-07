package com.miniyou.app.ui.checkup

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniyou.app.MiniYouApp
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.db.Indicator
import com.miniyou.app.data.toDate
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.AiFillCard
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.DateField
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.PhotoStrip
import com.miniyou.app.ui.components.SectionTitle
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch
import java.time.LocalDate

val IndicatorCategories = listOf("血常规", "尿常规", "B超", "生化", "血糖", "甲功", "其他")

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

    private val info get() = app.settings.current.lmp?.let { PregnancyInfo(it) }

    init {
        if (id > 0) viewModelScope.launch {
            app.db.checkups().get(id)?.let { c ->
                date = c.date.toDate(); hospital = c.hospital; week = c.gestationalWeek; type = c.checkupType
                weight = c.weightKg; bpS = c.bpSystolic; bpD = c.bpDiastolic; fhr = c.fetalHeartRate
                indicators.addAll(c.indicators); advice = c.doctorAdvice; nextDate = c.nextCheckupDate?.toDate()
                nextNote = c.nextCheckupNote; notes = c.notes; photos.addAll(c.photos); createdAt = c.createdAt
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
                val abnormal = d.indicators.count { it.abnormal }
                aiSummary = buildString {
                    append("已识别 ${d.indicators.size} 项指标")
                    if (abnormal > 0) append("，其中 $abnormal 项标记异常")
                    append("。请核对后保存。")
                    if (d.summary.isNotBlank()) append("\n\n").append(d.summary)
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

    suspend fun save(context: Context) {
        val c = Checkup(
            id = if (id > 0) id else 0, date = date.toEpochDay(), hospital = hospital.trim(), gestationalWeek = week.trim(),
            checkupType = type.trim(), weightKg = weight.trim(), bpSystolic = bpS.trim(), bpDiastolic = bpD.trim(),
            fetalHeartRate = fhr.trim(), indicators = indicators.toList(), doctorAdvice = advice.trim(),
            nextCheckupDate = nextDate?.toEpochDay(), nextCheckupNote = nextNote.trim(), notes = notes.trim(),
            photos = photos.toList(), createdAt = createdAt,
        )
        val newId = app.db.checkups().upsert(c)
        Reminders.scheduleCheckup(context, c.copy(id = if (id > 0) id else newId))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckupEditScreen(id: Long, onDone: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val vm = viewModel(key = "checkup-$id") { CheckupEditVM(app, id) }
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val aiPick = rememberPhotoPicker(multiple = true, title = "拍产检单 / 化验单 / B超单（可多张）") { vm.runAi(context, it) }
    val photoPick = rememberPhotoPicker(multiple = true, title = "添加产检单照片") { vm.addPhotos(context, it) }

    val save: () -> Unit = {
        if (vm.nextDate != null && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scope.launch { vm.save(context); onDone() }
    }

    AppScaffold(topBar = {
        BackTopBar(if (id > 0) "编辑产检记录" else "新增产检记录", onDone) {
            TextButton(onClick = save) { Text("保存") }
        }
    }) { pad ->
        ContentWidth(maxWidth = 820.dp, modifier = Modifier.padding(pad)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AiFillCard("AI 识图快速填写", "拍产检单，自动识别日期、医院、指标、医生建议和下次产检日期", onClick = aiPick)

                SectionTitle("基本信息")
                FormPair(layout.expanded,
                    { m -> DateField("检查日期", vm.date, { it?.let(vm::onDateChange) }, m) },
                    { m -> OutlinedTextField(vm.week, { vm.week = it }, label = { Text("孕周（如 20+3）") }, singleLine = true, modifier = m, shape = RoundedCornerShape(14.dp)) },
                )
                FormPair(layout.expanded,
                    { m -> OutlinedTextField(vm.hospital, { vm.hospital = it }, label = { Text("医院") }, singleLine = true, modifier = m, shape = RoundedCornerShape(14.dp)) },
                    { m -> OutlinedTextField(vm.type, { vm.type = it }, label = { Text("检查项目（如 NT、大排畸）") }, singleLine = true, modifier = m, shape = RoundedCornerShape(14.dp)) },
                )

                SectionTitle("体征数据")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("体重 kg", vm.weight, { vm.weight = it }, Modifier.weight(1f))
                    NumberField("胎心 次/分", vm.fhr, { vm.fhr = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("收缩压 mmHg", vm.bpS, { vm.bpS = it }, Modifier.weight(1f))
                    NumberField("舒张压 mmHg", vm.bpD, { vm.bpD = it }, Modifier.weight(1f))
                }

                SectionTitle("检查指标（${vm.indicators.size}）") {
                    TextButton(onClick = { vm.indicators.add(Indicator()); editingIndex = vm.indicators.lastIndex }) {
                        Icon(Icons.Default.Add, null); Text("添加")
                    }
                }
                if (vm.indicators.isEmpty()) {
                    Text("血常规、尿常规、B超数据等，可用 AI 识图自动填入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    SoftCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
                        vm.indicators.forEachIndexed { i, ind ->
                            Surface(onClick = { editingIndex = i }, color = MaterialTheme.colorScheme.surface) {
                                IndicatorRow(ind, Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                }

                SectionTitle("医生建议")
                OutlinedTextField(vm.advice, { vm.advice = it }, placeholder = { Text("医生的诊断意见、注意事项") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(14.dp))

                SectionTitle("下次产检")
                FormPair(layout.expanded,
                    { m -> DateField("下次产检日期", vm.nextDate, { vm.nextDate = it }, m, allowClear = true, placeholder = "设置后会提前提醒") },
                    { m -> OutlinedTextField(vm.nextNote, { vm.nextNote = it }, label = { Text("下次检查项目 / 准备事项") }, modifier = m, shape = RoundedCornerShape(14.dp)) },
                )

                SectionTitle("产检单照片")
                PhotoStrip(vm.photos, onAdd = photoPick, onRemove = { vm.photos.remove(it) })

                OutlinedTextField(vm.notes, { vm.notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 2, shape = RoundedCornerShape(14.dp))
                Button(onClick = save, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) { Text("保存记录") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    editingIndex?.let { i ->
        if (i in vm.indicators.indices) {
            IndicatorDialog(
                initial = vm.indicators[i],
                onDismiss = {
                    if (vm.indicators[i].name.isBlank()) vm.indicators.removeAt(i)
                    editingIndex = null
                },
                onDelete = { vm.indicators.removeAt(i); editingIndex = null },
                onSave = { vm.indicators[i] = it; editingIndex = null },
            )
        }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在解析产检单…")
    vm.aiError?.let { MessageDialog("识别失败", it) { vm.aiError = null } }
    vm.aiSummary?.let { MessageDialog("识别完成 ✨", it) { vm.aiSummary = null } }
}

/** 宽屏两列并排，窄屏上下堆叠。 */
@Composable
fun FormPair(expanded: Boolean, first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    if (expanded) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { first(Modifier.weight(1f)); second(Modifier.weight(1f)) }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value, { onChange(it.filter { c -> c.isDigit() || c == '.' }) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier, shape = RoundedCornerShape(14.dp),
    )
}

@Composable
fun IndicatorRow(ind: Indicator, modifier: Modifier = Modifier) {
    val flagColor = when (ind.flag) {
        "偏高" -> MaterialTheme.colorScheme.error
        "偏低" -> com.miniyou.app.ui.theme.Amber
        "异常" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(ind.name.ifBlank { "（未命名指标）" }, style = MaterialTheme.typography.bodyMedium)
            val sub = listOf(ind.category, ind.reference.takeIf { it.isNotBlank() }?.let { "参考 $it" }).filterNotNull().joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${ind.value} ${ind.unit}".trim(), style = MaterialTheme.typography.titleSmall, color = flagColor)
            if (ind.flag.isNotBlank()) Text(
                when (ind.flag) { "偏高" -> "↑ 偏高"; "偏低" -> "↓ 偏低"; else -> "⚠ 异常" },
                style = MaterialTheme.typography.labelSmall, color = flagColor,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndicatorDialog(initial: Indicator, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (Indicator) -> Unit) {
    var cat by remember { mutableStateOf(initial.category.ifBlank { "其他" }) }
    var name by remember { mutableStateOf(initial.name) }
    var value by remember { mutableStateOf(initial.value) }
    var unit by remember { mutableStateOf(initial.unit) }
    var ref by remember { mutableStateOf(initial.reference) }
    var flag by remember { mutableStateOf(initial.flag) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检查指标") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IndicatorCategories.forEach { FilterChip(cat == it, { cat = it }, label = { Text(it) }) }
                }
                OutlinedTextField(name, { name = it }, label = { Text("名称（如 血红蛋白 HGB）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value, { value = it }, label = { Text("结果") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(unit, { unit = it }, label = { Text("单位") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(ref, { ref = it }, label = { Text("参考范围") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("" to "正常", "偏高" to "偏高", "偏低" to "偏低", "异常" to "异常").forEach { (k, l) ->
                        FilterChip(flag == k, { flag = k }, label = { Text(l) })
                    }
                }
                OutlinedButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null); Text(" 删除该指标") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(Indicator(cat, name.trim(), value.trim(), unit.trim(), ref.trim(), flag)) }, enabled = name.isNotBlank()) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
