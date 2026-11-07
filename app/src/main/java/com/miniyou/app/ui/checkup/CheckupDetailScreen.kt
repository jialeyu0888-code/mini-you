package com.miniyou.app.ui.checkup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.InfoRow
import com.miniyou.app.ui.components.PhotoStrip
import com.miniyou.app.ui.components.SectionTitle
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.ContentWidth
import kotlinx.coroutines.launch

@Composable
fun CheckupDetailScreen(id: Long, nav: NavController) {
    val app = app()
    val c by app.db.checkups().observe(id).collectAsState(initial = null)
    AppScaffold(topBar = { BackTopBar("产检详情", { nav.popBackStack() }) }) { pad ->
        c?.let { checkup ->
            ContentWidth(modifier = Modifier.padding(pad)) {
                CheckupDetailContent(checkup, nav, onDeleted = { nav.popBackStack() })
            }
        }
    }
}

@Composable
fun CheckupDetailContent(c: Checkup, nav: NavController, onDeleted: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    val d = c.date.toDate()
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(c.checkupType.ifBlank { "常规产检" }, style = MaterialTheme.typography.titleLarge)
                    Text("${d.cn()} ${d.weekdayCn()}", style = MaterialTheme.typography.bodyMedium)
                    if (c.gestationalWeek.isNotBlank()) Text("孕 ${c.gestationalWeek} 周", style = MaterialTheme.typography.bodyMedium)
                    if (c.hospital.isNotBlank()) Text("🏥 ${c.hospital}", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { nav.navigate(Routes.checkupEdit(c.id)) }) { Icon(Icons.Outlined.Edit, "编辑") }
                IconButton(onClick = { confirm = true }) { Icon(Icons.Outlined.Delete, "删除") }
            }
        }
        if (c.weightKg.isNotBlank() || c.bpSystolic.isNotBlank() || c.fetalHeartRate.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VitalTile("体重", c.weightKg, "kg", Modifier.weight(1f))
                VitalTile("血压", if (c.bpSystolic.isBlank()) "" else "${c.bpSystolic}/${c.bpDiastolic}", "mmHg", Modifier.weight(1f))
                VitalTile("胎心", c.fetalHeartRate, "次/分", Modifier.weight(1f))
            }
        }
        if (c.indicators.isNotEmpty()) {
            c.indicators.groupBy { it.category.ifBlank { "其他" } }.forEach { (cat, list) ->
                SectionTitle("$cat（${list.size}）")
                SoftCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    list.forEachIndexed { i, ind ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        IndicatorRow(ind, Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
        }
        if (c.doctorAdvice.isNotBlank()) {
            SectionTitle("医生建议")
            SoftCard { Text(c.doctorAdvice, style = MaterialTheme.typography.bodyMedium) }
        }
        if (c.nextCheckupDate != null || c.nextCheckupNote.isNotBlank()) {
            SectionTitle("下次产检")
            SoftCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                c.nextCheckupDate?.toDate()?.let { Text("📅 ${it.cn()} ${it.weekdayCn()}", style = MaterialTheme.typography.titleMedium) }
                if (c.nextCheckupNote.isNotBlank()) Text(c.nextCheckupNote, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (c.notes.isNotBlank()) {
            SectionTitle("备注")
            SoftCard { InfoRow("备注", c.notes) }
        }
        if (c.photos.isNotEmpty()) {
            SectionTitle("产检单照片")
            PhotoStrip(c.photos, size = 110.dp)
        }
        OutlinedButton(onClick = { nav.navigate(Routes.checkupEdit(c.id)) }, modifier = Modifier.fillMaxWidth()) { Text("编辑这条记录") }
        Spacer(Modifier.height(96.dp))
    }
    if (confirm) {
        ConfirmDialog("删除产检记录", "删除后无法恢复，确定删除吗？", onDismiss = { confirm = false }) {
            scope.launch {
                Reminders.cancelCheckup(context, c.id)
                c.photos.forEach(ImageStore::delete)
                app.db.checkups().delete(c)
                onDeleted()
            }
        }
    }
}

@Composable
private fun VitalTile(label: String, value: String, unit: String, modifier: Modifier) {
    SoftCard(modifier, contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(value.ifBlank { "—" }, fontSize = 20.sp, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}
