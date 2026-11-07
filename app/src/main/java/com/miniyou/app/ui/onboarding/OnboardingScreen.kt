package com.miniyou.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.cn
import com.miniyou.app.data.lmpFromDueDate
import com.miniyou.app.ui.components.AiFillCard
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.DateField
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.SoftGradient
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 孕期信息表单，首次启动与设置页共用。 */
@Composable
fun PregnancyForm(
    initialLmp: LocalDate?,
    initialName: String,
    confirmText: String,
    onSave: (lmp: LocalDate, name: String) -> Unit,
) {
    val app = app()
    val scope = rememberCoroutineScope()
    var mode by remember { mutableIntStateOf(0) }
    var lmp by remember { mutableStateOf(initialLmp) }
    var due by remember { mutableStateOf(initialLmp?.let { PregnancyInfo(it).dueDate }) }
    var name by remember { mutableStateOf(initialName) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }

    val effectiveLmp = if (mode == 0) lmp else due?.let(::lmpFromDueDate)

    val pick = rememberPhotoPicker(title = "拍 B 超单或产检单，自动识别孕周") { uris ->
        scope.launch {
            loading = true
            try {
                val r = app.ai.parsePregnancyDates(uris)
                lmp = r.lmp; due = r.lmp?.let { PregnancyInfo(it).dueDate }; mode = 0
                message = "识别成功" to "根据「${r.source}」推算出末次月经为 ${r.lmp?.cn()}，请核对。"
            } catch (e: AiException) {
                message = "识别失败" to (e.message ?: "")
            } catch (e: Exception) {
                message = "识别失败" to "出了点问题：${e.message}"
            } finally { loading = false }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AiFillCard("AI 识图自动推算", "拍一张 B 超单 / 产检单，自动识别预产期或孕周", onClick = pick)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("我知道末次月经", "我知道预产期").forEachIndexed { i, label ->
                SegmentedButton(selected = mode == i, onClick = { mode = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
            }
        }
        if (mode == 0) DateField("末次月经第一天", lmp, { lmp = it; due = it?.let { d -> PregnancyInfo(d).dueDate } })
        else DateField("预产期", due, { due = it; lmp = it?.let(::lmpFromDueDate) })
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(12) },
            label = { Text("怎么称呼你（选填）") }, placeholder = { Text("例如：小柚妈妈") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp),
        )
        val valid = effectiveLmp != null && effectiveLmp <= LocalDate.now() && effectiveLmp > LocalDate.now().minusDays(301)
        if (effectiveLmp != null) {
            val info = PregnancyInfo(effectiveLmp)
            SoftCard(color = MaterialTheme.colorScheme.primaryContainer) {
                if (valid) {
                    Text("今天是 ${info.weekLabel}", style = MaterialTheme.typography.titleMedium)
                    Text("预产期 ${info.dueDate.cn()} · 还有 ${info.daysToDue} 天", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("日期好像不太对，请检查一下（末次月经应在最近 43 周内）", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Button(
            onClick = { effectiveLmp?.let { onSave(it, name.trim()) } },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) { Text(confirmText, fontSize = 16.sp) }
    }

    if (loading) AiProgressDialog("AI 正在读取孕周信息…")
    message?.let { (t, m) -> MessageDialog(t, m) { message = null } }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val settings = app().settings
    Box(Modifier.fillMaxSize().background(SoftGradient)) {
        ContentWidth(maxWidth = 560.dp) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(28.dp))
                Text("🤰", fontSize = 64.sp)
                Spacer(Modifier.height(8.dp))
                Text("欢迎来到「小小你」", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "陪你记录孕育路上的每一天。\n先告诉我们你的末次月经或预产期，帮你计算孕周吧～",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                PregnancyForm(null, "", "开始记录") { lmp, name ->
                    settings.update { it.copy(lmpEpochDay = lmp.toEpochDay(), nickname = name) }
                    onDone()
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "所有记录只保存在你的手机上。AI 识图会把你选择的图片发送给智谱大模型进行解析。",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
