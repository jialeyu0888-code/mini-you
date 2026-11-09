package com.miniyou.app.ui.checkup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.db.Indicator
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.PhotoStrip
import com.miniyou.app.ui.components.app
import kotlinx.coroutines.launch

internal const val DeleteCheckupMessage = "删除后无法恢复，此记录关联的复诊安排和提醒也会取消。确定删除吗？"

@Composable
fun CheckupDetailScreen(id: Long, nav: NavController) {
    val checkup by app().db.checkups().observe(id).collectAsState(initial = null)
    PrenatalPage { pad ->
        checkup?.let {
            CheckupDetailContent(
                it, nav,
                onDeleted = { nav.popBackStack() },
                modifier = Modifier.padding(pad),
                showBack = true,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
fun CheckupDetailContent(
    checkup: Checkup,
    nav: NavController,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    val app = app()
    val scope = rememberCoroutineScope()
    val colors = LocalPrenatalColors.current
    var confirm by remember { mutableStateOf(false) }
    val date = checkup.date.toDate()
    Column(modifier.fillMaxSize()) {
        if (showBack) {
            PrenatalHeader("产检详情", onBack) {
                IconButton(onClick = { nav.navigate(Routes.checkupEdit(checkup.id)) }) {
                    Icon(Icons.Outlined.Edit, "编辑", tint = colors.ink)
                }
                IconButton(onClick = { confirm = true }) {
                    Icon(Icons.Outlined.Delete, "删除", tint = colors.ink)
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!showBack) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("产检详情", Modifier.weight(1f), color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { nav.navigate(Routes.checkupEdit(checkup.id)) }) {
                        Icon(Icons.Outlined.Edit, "编辑", tint = colors.ink)
                    }
                    IconButton(onClick = { confirm = true }) { Icon(Icons.Outlined.Delete, "删除", tint = colors.ink) }
                }
            }
            PrenatalCard(color = colors.pinkCard, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(checkup.checkupType.ifBlank { "常规产检" }, color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PrenatalIcon(R.drawable.checkup_ic_calendar, Modifier.size(14.dp))
                            Text("${date.cn()} ${date.weekdayCn()}", Modifier.padding(start = 4.dp), color = colors.ink, fontSize = 12.sp)
                        }
                        if (checkup.gestationalWeek.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PrenatalIcon(R.drawable.checkup_ic_heart, Modifier.size(14.dp))
                                Text("孕 ${checkup.gestationalWeek} 周", Modifier.padding(start = 4.dp), color = colors.ink, fontSize = 12.sp)
                            }
                        }
                        if (checkup.hospital.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PrenatalIcon(R.drawable.checkup_ic_hospital, Modifier.size(14.dp))
                                Text(checkup.hospital, Modifier.padding(start = 4.dp), color = colors.ink, fontSize = 12.sp)
                            }
                        }
                    }
                    PrenatalArt(R.drawable.checkup_form_bunny, Modifier.size(108.dp, 90.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VitalTile("体重", checkup.weightKg.ifBlank { "—" }, "kg", R.drawable.checkup_ic_weight, Modifier.weight(1f))
                VitalTile(
                    "血压",
                    if (checkup.bpSystolic.isBlank()) "—" else "${checkup.bpSystolic}/${checkup.bpDiastolic}",
                    "mmHg", R.drawable.checkup_ic_heart, Modifier.weight(1f),
                )
                VitalTile("胎心", checkup.fetalHeartRate.ifBlank { "—" }, "次/分", R.drawable.checkup_ic_fhr, Modifier.weight(1f))
            }
            if (checkup.indicators.isNotEmpty()) {
                checkup.indicators.groupBy { it.category.ifBlank { "其他" } }.forEach { (category, rows) ->
                    PrenatalCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PrenatalIcon(
                                    if (category.contains("尿")) R.drawable.checkup_rec_tube else R.drawable.checkup_ic_drop,
                                    Modifier.size(16.dp),
                                )
                                Text("$category（${rows.size}）", Modifier.padding(start = 6.dp), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            rows.forEach { IndicatorLine(it) }
                        }
                    }
                }
            }
            if (checkup.doctorAdvice.isNotBlank()) NoteBlock("医生建议", R.drawable.checkup_ic_note, checkup.doctorAdvice)
            if (checkup.nextCheckupDate != null || checkup.nextCheckupNote.isNotBlank()) {
                val next = checkup.nextCheckupDate?.toDate()
                NoteBlock(
                    "下次产检",
                    R.drawable.checkup_ic_calendar,
                    listOfNotNull(next?.let { "${it.cn()} ${it.weekdayCn()}" }, checkup.nextCheckupNote.takeIf { it.isNotBlank() }).joinToString("\n"),
                )
            }
            if (checkup.notes.isNotBlank()) NoteBlock("备注", R.drawable.checkup_ic_note, checkup.notes)
            if (checkup.photos.isNotEmpty()) {
                PrenatalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PrenatalIcon(R.drawable.checkup_ic_photo, Modifier.size(16.dp))
                            Text("产检单照片", Modifier.padding(start = 6.dp), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        PhotoStrip(checkup.photos, size = 96.dp)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }
    }
    if (confirm) {
        PrenatalConfirmDialog("删除产检记录", DeleteCheckupMessage, onDismiss = { confirm = false }) {
            scope.launch {
                app.checkupPlans.deleteCheckup(checkup)
                onDeleted()
            }
        }
    }
}

@Composable
private fun VitalTile(label: String, value: String, unit: String, icon: Int, modifier: Modifier) {
    val colors = LocalPrenatalColors.current
    PrenatalCard(modifier, contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PrenatalIcon(icon, Modifier.size(16.dp))
            Text(label, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Text(value, color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(unit, color = colors.muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun IndicatorLine(indicator: Indicator) {
    val colors = LocalPrenatalColors.current
    val flagColor = when (indicator.flag) {
        "偏高", "异常" -> colors.danger
        "偏低" -> colors.accent
        else -> colors.ink
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(indicator.name.ifBlank { "未命名指标" }, color = colors.ink, fontSize = 14.sp)
            val sub = listOfNotNull(
                indicator.category.takeIf { it.isNotBlank() },
                indicator.reference.takeIf { it.isNotBlank() }?.let { "参考 $it" },
            ).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, color = colors.muted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${indicator.value} ${indicator.unit}".trim(), color = flagColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (indicator.flag.isNotBlank()) Text(indicator.flag, color = flagColor, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NoteBlock(title: String, icon: Int, body: String) {
    val colors = LocalPrenatalColors.current
    PrenatalCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrenatalIcon(icon, Modifier.size(16.dp))
                Text(title, Modifier.padding(start = 6.dp), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(body, color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
