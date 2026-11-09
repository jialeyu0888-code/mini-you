package com.miniyou.app.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.miniyou.app.BuildConfig
import com.miniyou.app.R
import com.miniyou.app.ai.ZhipuClient
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.cn
import com.miniyou.app.notify.Reminders
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.InfoRow
import com.miniyou.app.ui.components.SectionTitle
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.onboarding.PregnancyForm
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch

private fun mask(key: String) = if (key.length <= 10) "••••" else key.take(6) + "••••••" + key.takeLast(4)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val settings by app.settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    var editPregnancy by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(settings.customApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val info = settings.lmp?.let { PregnancyInfo(it) }

    AppScaffold(topBar = { BackTopBar("设置", onBack) }) { pad ->
        ContentWidth(maxWidth = 720.dp, modifier = Modifier.padding(pad).consumeWindowInsets(pad).imePadding()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionTitle("孕期信息")
                SoftCard {
                    if (editPregnancy) {
                        PregnancyForm(settings.lmp, settings.nickname, "保存") { lmp, name ->
                            app.settings.update { it.copy(lmpEpochDay = lmp.toEpochDay(), nickname = name) }
                            editPregnancy = false
                        }
                        TextButton(onClick = { editPregnancy = false }) { Text("取消") }
                    } else {
                        InfoRow("称呼", settings.nickname.ifBlank { "准妈妈" })
                        info?.let {
                            InfoRow("末次月经", it.lmp.cn())
                            InfoRow("预产期", it.dueDate.cn())
                            InfoRow("当前孕周", it.weekLabel)
                        }
                        OutlinedButton(onClick = { editPregnancy = true }, modifier = Modifier.fillMaxWidth()) { Text("修改孕期信息") }
                    }
                }

                SectionTitle("AI 识图")
                SoftCard {
                    Text("模型：${BuildConfig.ZHIPU_MODEL}（智谱 GLM 视觉理解）", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        when {
                            settings.usingCustomKey -> "当前使用：你填写的 Key（${mask(settings.customApiKey)}）"
                            BuildConfig.ZHIPU_API_KEY.isNotBlank() -> "当前使用：安装包内置的 Key"
                            else -> "当前没有可用的 Key，请在下方填写"
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        keyInput, { keyInput = it.trim() }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        label = { Text("自定义智谱 API Key") }, placeholder = { Text("在 open.bigmodel.cn 获取") },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { app.settings.update { it.copy(customApiKey = keyInput) }; testResult = "已保存" }) { Text("保存 Key") }
                        OutlinedButton(onClick = { keyInput = ""; app.settings.update { it.copy(customApiKey = "") }; testResult = "已恢复为内置 Key" }) { Text("恢复默认") }
                    }
                    TextButton(onClick = {
                        testing = true; testResult = null
                        scope.launch {
                            testResult = try { "✅ 连接成功：" + ZhipuClient.ping(app.settings.current.effectiveApiKey) } catch (e: Exception) { "❌ ${e.message}" }
                            testing = false
                        }
                    }, enabled = !testing) { Text(if (testing) "测试中…" else "测试连接") }
                    testResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text("AI 识图时会把所选图片发送给智谱开放平台解析，结果仅填入表单，不会自动保存，请核对后再保存。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                SectionTitle("提醒通知")
                SoftCard {
                    val granted = remember(refresh) { Reminders.canNotify(context) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("通知权限：" + if (granted) "已开启" else "未开启", style = MaterialTheme.typography.bodyMedium)
                            Text("用于产检日期提醒和每日打卡提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!granted) {
                            TextButton(onClick = {
                                if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                            }) { Text("去开启") }
                        }
                    }
                }

                SectionTitle("关于")
                SoftCard {
                    InfoRow("应用", stringResource(R.string.about_app_title, stringResource(R.string.app_name)))
                    InfoRow("版本", BuildConfig.VERSION_NAME)
                    InfoRow("数据", "全部保存在本机，卸载应用会清除")
                    Spacer(Modifier.height(4.dp))
                    Text("本应用内容仅供参考，不能替代医生的诊断与治疗。身体不适请及时就医。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
