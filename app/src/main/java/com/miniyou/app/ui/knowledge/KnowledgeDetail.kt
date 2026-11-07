package com.miniyou.app.ui.knowledge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniyou.app.data.content.ClassContent
import com.miniyou.app.data.content.Lesson
import com.miniyou.app.data.content.Remedy
import com.miniyou.app.data.content.RemedyContent
import com.miniyou.app.data.content.RemedyMethod
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.BulletList
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.SectionTitle
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.Mint
import com.miniyou.app.ui.theme.WarnRed

private val WarnContainer = Color(0xFFFDECEC)
private val WarnText = Color(0xFF9B2C2C)

@Composable
fun RxBadge(text: String = "需遵医嘱") {
    Surface(color = WarnRed, shape = RoundedCornerShape(6.dp)) {
        Text("⚠ $text", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun WarningCard(title: String, items: List<String>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WarnContainer),
        border = BorderStroke(1.dp, WarnRed.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = WarnText)
            Spacer(Modifier.height(6.dp))
            BulletList(items, color = WarnText)
        }
    }
}

@Composable
fun EmergencyBanner(text: String) {
    if (text.isBlank()) return
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarnContainer),
        border = BorderStroke(1.dp, WarnRed.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp)) {
            Text("🚑", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = WarnText)
        }
    }
}

@Composable
fun DisclaimerFooter(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("免责声明", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RemedyDetailScreen(id: String, onBack: () -> Unit) {
    val content = app().content.remedies
    val r = content.remedies.firstOrNull { it.id == id }
    AppScaffold(topBar = { BackTopBar(r?.symptom ?: "缓解攻略", onBack) }) { pad ->
        if (r != null) ContentWidth(modifier = Modifier.padding(pad)) { RemedyDetailContent(r, content, showTitle = false) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemedyDetailContent(r: Remedy, content: RemedyContent, showTitle: Boolean = true) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showTitle) Text(r.symptom, style = MaterialTheme.typography.headlineSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(content.categoryLabel(r.category), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            r.trimesters.forEach { Pill(content.trimesterLabel(it)) }
            r.aliases.filter { it != r.symptom }.take(4).forEach { Pill("#$it", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        SoftCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Text(r.overview, style = MaterialTheme.typography.bodyMedium)
            if (r.trimesterNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("🕒 ${r.trimesterNote}", style = MaterialTheme.typography.bodySmall)
            }
        }

        val groups = r.methods.groupBy { if (it.isMedication) "medication" else it.type }
        content.methodTypes.map { it.id }.plus(groups.keys).distinct().forEach { type ->
            val list = groups[type].orEmpty()
            if (list.isEmpty()) return@forEach
            SectionTitle("${typeEmoji(type)} ${content.typeLabel(type)}（${list.size}）")
            if (type == "medication") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarnContainer),
                    border = BorderStroke(1.5.dp, WarnRed),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        RxBadge(content.medicationNotice)
                        Spacer(Modifier.width(8.dp))
                        Text(content.medicationNoticeDetail, style = MaterialTheme.typography.bodySmall, color = WarnText)
                    }
                }
            }
            list.forEach { MethodCard(it, content) }
        }

        WarningCard("🏥 出现这些情况请及时就医", r.whenToSeeDoctor)
        WarningCard("🚨 无论什么症状，出现以下情况请立即就医", content.generalEmergencySigns)
        DisclaimerFooter(content.disclaimer)
        Spacer(Modifier.height(24.dp))
    }
}

private fun typeEmoji(type: String) = when (type) {
    "diet" -> "🥗"
    "lifestyle" -> "🧘‍♀️"
    "medication" -> "💊"
    else -> "📌"
}

@Composable
private fun MethodCard(m: RemedyMethod, content: RemedyContent) {
    val rx = m.isMedication
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (rx) BorderStroke(2.dp, WarnRed) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (rx) { RxBadge(m.rxLabel ?: content.medicationNotice); Spacer(Modifier.height(8.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                content.evidence(m.evidence)?.let { ev ->
                    val (bg, fg) = when (ev.id) {
                        "guideline" -> Mint.copy(alpha = 0.18f) to Color(0xFF1F6B52)
                        "experience" -> Color(0xFFE3EEFB) to Color(0xFF285A8F)
                        else -> Color(0xFFF1EDE4) to Color(0xFF6B5B3E)
                    }
                    Pill(ev.label, bg, fg)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(m.howTo, style = MaterialTheme.typography.bodyMedium)
            if (m.cautions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(color = if (rx) WarnContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text("注意事项", style = MaterialTheme.typography.labelLarge, color = if (rx) WarnText else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        BulletList(m.cautions, color = if (rx) WarnText else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonDetailScreen(id: String, onBack: () -> Unit) {
    val content = app().content.classes
    val l = content.lesson(id)
    AppScaffold(topBar = { BackTopBar(l?.title ?: "准爸妈课堂", onBack) }) { pad ->
        if (l != null) ContentWidth(modifier = Modifier.padding(pad)) { LessonDetailContent(l, content, showTitle = false) }
    }
}

@Composable
fun LessonDetailContent(l: Lesson, content: ClassContent, showTitle: Boolean = true) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showTitle) Text(l.title, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(if (l.audience == "mother") "产后妈妈 · ${l.ageLabel}" else "宝宝 ${l.ageLabel}")
            Pill("阅读约 ${l.readMinutes} 分钟", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        }
        SoftCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Text("${lessonEmoji(l)} ${l.summary}", style = MaterialTheme.typography.bodyLarge)
        }
        if (l.steps.isNotEmpty()) {
            SectionTitle("步骤详解")
            l.steps.forEach { s ->
                SoftCard {
                    Row {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Text("${s.order}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(s.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        l.table?.let { t ->
            SectionTitle("📋 ${t.title}")
            SoftCard {
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row { t.columns.forEach { Text(it, style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 110.dp).padding(6.dp)) } }
                    HorizontalDivider()
                    t.rows.forEach { row ->
                        Row { row.forEach { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.widthIn(min = 110.dp).padding(6.dp)) } }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                if (t.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(t.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (l.tips.isNotEmpty()) {
            SectionTitle("💡 小贴士")
            SoftCard(color = Color(0xFFFFF7E0)) { BulletList(l.tips, color = Color(0xFF5C4813)) }
        }
        if (l.myths.isNotEmpty()) {
            SectionTitle("🙅 常见误区")
            l.myths.forEach { m ->
                SoftCard {
                    Text("❌ ${m.myth}", style = MaterialTheme.typography.bodyMedium, color = WarnText)
                    Spacer(Modifier.height(6.dp))
                    Text("✅ ${m.fact}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1F6B52))
                }
            }
        }
        WarningCard("🏥 何时就医", l.whenToSeeDoctor)
        EmergencyBanner(content.emergencyNotice)
        DisclaimerFooter(content.disclaimer)
        Spacer(Modifier.height(24.dp))
    }
}
