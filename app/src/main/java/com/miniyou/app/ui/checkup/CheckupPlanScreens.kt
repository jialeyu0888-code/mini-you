package com.miniyou.app.ui.checkup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.CheckupPlan
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val DeletePlanMessage = "删除后会取消这项计划的提醒。"

@Composable
fun CheckupPlansScreen(nav: NavController, onBack: () -> Unit) {
    val repository = app().checkupPlans
    val plans by repository.all.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val today = LocalDate.now().toEpochDay()
    PrenatalPage { pad ->
        val colors = LocalPrenatalColors.current
        Column(Modifier.fillMaxSize().padding(pad)) {
            BoxHeader(onBack) {
                PrenatalTextAction("新增计划") { nav.navigate(Routes.checkupPlanEdit()) }
            }
            val loaded = plans ?: return@Column
            val (upcoming, past) = loaded.partition { it.date >= today }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrenatalIcon(R.drawable.checkup_ic_calendar, Modifier.size(16.dp))
                        Text("待产检（${upcoming.size}）", Modifier.padding(start = 6.dp), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                if (upcoming.isEmpty()) {
                    item {
                        PrenatalCard {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                PrenatalArt(R.drawable.checkup_form_bunny, Modifier.size(96.dp, 80.dp))
                                Text("还没有待产检计划", color = colors.ink, fontWeight = FontWeight.Bold)
                                Text("可以提前安排多次产检", color = colors.muted, fontSize = 13.sp)
                            }
                        }
                    }
                }
                items(upcoming, key = { it.id }) { plan ->
                    PlanCard(
                        plan,
                        onEdit = { nav.navigate(Routes.checkupPlanEdit(plan.id)) },
                        onDelete = { scope.launch { repository.delete(plan.id) } },
                    )
                }
                if (past.isNotEmpty()) {
                    item { Text("已过日期的计划", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                    items(past.asReversed(), key = { it.id }) { plan ->
                        PlanCard(
                            plan,
                            onEdit = { nav.navigate(Routes.checkupPlanEdit(plan.id)) },
                            onDelete = { scope.launch { repository.delete(plan.id) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxHeader(onBack: () -> Unit, action: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
        PrenatalArt(
            R.drawable.checkup_form_bunny,
            Modifier.align(Alignment.CenterEnd).padding(end = 72.dp).size(92.dp, 72.dp),
        )
        PrenatalHeader("产检计划", onBack) { action() }
    }
}

@Composable
private fun PlanCard(plan: CheckupPlan, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalPrenatalColors.current
    val date = plan.date.toDate()
    PrenatalCard(onClick = onEdit, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrenatalArt(checkupKindArt(plan.project), Modifier.size(46.dp, 40.dp))
            Column(Modifier.padding(start = 8.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(plan.project.ifBlank { "产检" }, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${date.cn()} ${date.weekdayCn()}", color = colors.muted, fontSize = 12.sp)
                if (plan.hospital.isNotBlank()) Text(plan.hospital, color = colors.muted, fontSize = 12.sp)
                if (plan.note.isNotBlank()) Text(plan.note, color = colors.ink, fontSize = 12.sp, maxLines = 2)
            }
            RecordActionsMenu(
                "产检计划", DeletePlanMessage,
                onEdit = onEdit, onDelete = onDelete,
                modifier = Modifier.align(Alignment.Top), tint = colors.muted,
            )
        }
    }
}

@Composable
fun CheckupPlanEditScreen(id: Long, onBack: () -> Unit) {
    val repository = app().checkupPlans
    var plan by remember(id) { mutableStateOf<CheckupPlan?>(null) }
    var loaded by remember(id) { mutableStateOf(id <= 0) }
    LaunchedEffect(id) {
        if (id > 0) {
            plan = repository.get(id)
            loaded = true
        }
    }
    PrenatalPage { pad ->
        val colors = LocalPrenatalColors.current
        if (!loaded) return@PrenatalPage
        if (id > 0 && plan == null) {
            Column(Modifier.padding(pad).padding(20.dp)) {
                PrenatalHeader("产检计划", onBack)
                Text("找不到这项计划", color = colors.ink, fontWeight = FontWeight.Bold)
            }
            return@PrenatalPage
        }
        CheckupPlanForm(plan, Modifier.padding(pad).consumeWindowInsets(pad).imePadding(), onBack)
    }
}

@Composable
private fun CheckupPlanForm(plan: CheckupPlan?, modifier: Modifier, onDone: () -> Unit) {
    val repository = app().checkupPlans
    val context = LocalContext.current
    val colors = LocalPrenatalColors.current
    val scope = rememberCoroutineScope()
    val expanded = LocalWindowLayout.current.expanded
    var date by rememberSaveable { mutableStateOf(plan?.date ?: LocalDate.now().toEpochDay()) }
    var project by rememberSaveable { mutableStateOf(plan?.project.orEmpty()) }
    var hospital by rememberSaveable { mutableStateOf(plan?.hospital.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(plan?.note.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun submit(errorMessage: String, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                action()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = errorMessage
            } finally {
                busy = false
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        PrenatalHeader(if (plan == null) "新增产检计划" else "编辑产检计划", onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrenatalDateField("产检日期", date.toDate(), { it?.let { picked -> date = picked.toEpochDay() } }, "请选择产检日期", Modifier.weight(1f), required = true)
                    PrenatalInput("检查项目（选填）", project, { project = it }, "请选择检查项目", Modifier.weight(1f))
                }
            } else {
                PrenatalDateField("产检日期", date.toDate(), { it?.let { picked -> date = picked.toEpochDay() } }, "请选择产检日期", required = true)
                PrenatalInput("检查项目（选填）", project, { project = it }, "请选择检查项目")
            }
            PrenatalInput("医院（选填）", hospital, { hospital = it }, "请选择医院")
            PrenatalInput("准备事项或备注（选填）", note, { note = it }, "例如：空腹 8–12 小时，带上产检手册", minLines = 3)
            Text(
                "开启通知后，会在产检日前一天和当天早上提醒你，避免遗漏产检哦",
                color = colors.muted, fontSize = 12.sp, lineHeight = 18.sp,
            )
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrenatalButton("保存计划", enabled = !busy, onClick = {
                val updated = CheckupPlan(
                    id = plan?.id ?: 0, date = date, project = project.trim(),
                    hospital = hospital.trim(), note = note.trim(), sourceCheckupId = plan?.sourceCheckupId,
                )
                submit("保存计划失败，请重试。") {
                    repository.save(updated)
                    if (updated.date >= LocalDate.now().toEpochDay() && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            })
            if (plan != null) {
                Text(
                    "删除计划",
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { confirmDelete = true }.padding(vertical = 8.dp),
                    color = colors.danger,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    if (confirmDelete && plan != null) {
        PrenatalConfirmDialog("删除产检计划", DeletePlanMessage, onDismiss = { confirmDelete = false }) {
            confirmDelete = false
            submit("删除计划失败，请重试。") { repository.delete(plan.id) }
        }
    }
    error?.let { MessageDialog("产检计划", it) { error = null } }
}
