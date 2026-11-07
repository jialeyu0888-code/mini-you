package com.miniyou.app.ui.diary

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniyou.app.MiniYouApp
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.db.DiaryEntry
import com.miniyou.app.data.toDate
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.AiFillCard
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.DateField
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.PhotoStrip
import com.miniyou.app.ui.components.SectionTitle
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch
import java.time.LocalDate

val DefaultDiaryTags = listOf("胎动", "孕吐", "产检", "散步", "美食", "失眠", "胎教", "家人", "宝宝用品", "运动")

class DiaryEditVM(private val app: MiniYouApp, private val id: Long, initialDate: Long) : ViewModel() {
    var date by mutableStateOf(if (initialDate >= 0) initialDate.toDate() else LocalDate.now())
    var mood by mutableStateOf(Moods.first().label)
    val tags = mutableStateListOf<String>()
    val customTags = mutableStateListOf<String>()
    var content by mutableStateOf("")
    val photos = mutableStateListOf<String>()
    var existing: DiaryEntry? = null
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)

    init {
        if (id > 0) viewModelScope.launch {
            app.db.diary().get(id)?.let { e ->
                existing = e
                date = e.date.toDate(); mood = e.mood; tags.addAll(e.tags); content = e.content; photos.addAll(e.photos)
                customTags.addAll(e.tags.filter { it !in DefaultDiaryTags })
            }
        }
    }

    fun toggleTag(t: String) { if (t in tags) tags.remove(t) else tags.add(t) }

    fun addPhotos(context: Context, uris: List<Uri>) = viewModelScope.launch {
        uris.forEach { u -> ImageStore.importImage(context, u)?.let(photos::add) }
    }

    fun runAi(context: Context, uris: List<Uri>) {
        aiLoading = true
        viewModelScope.launch {
            try {
                val info = app.settings.current.lmp?.let { PregnancyInfo(it, date) }
                val d = app.ai.parseDiary(uris, Moods.map { it.label }, info)
                mood = d.mood
                d.tags.forEach { t -> if (t !in tags) tags.add(t); if (t !in DefaultDiaryTags && t !in customTags) customTags.add(t) }
                content = if (content.isBlank()) d.content else content.trimEnd() + "\n\n" + d.content
                uris.forEach { u -> ImageStore.importImage(context, u)?.let(photos::add) }
            } catch (e: AiException) { aiError = e.message } catch (e: Exception) { aiError = "识别失败：${e.message}" }
            finally { aiLoading = false }
        }
    }

    suspend fun save() {
        val base = existing ?: DiaryEntry(date = date.toEpochDay())
        app.db.diary().upsert(base.copy(date = date.toEpochDay(), mood = mood, tags = tags.toList(), content = content.trim(), photos = photos.toList()))
    }

    suspend fun delete() {
        existing?.let { e -> e.photos.forEach(ImageStore::delete); app.db.diary().delete(e) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryEditScreen(id: Long, date: Long, onDone: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val vm = viewModel(key = "diary-$id-$date") { DiaryEditVM(app, id, date) }
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    var addTag by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val aiPick = rememberPhotoPicker(multiple = true, title = "选几张今天的照片，AI 帮你写日记") { vm.runAi(context, it) }
    val photoPick = rememberPhotoPicker(multiple = true, title = "添加照片") { vm.addPhotos(context, it) }
    val save: () -> Unit = { scope.launch { vm.save(); onDone() } }

    AppScaffold(topBar = {
        BackTopBar(if (id > 0) "编辑日记" else "写日记", onDone) {
            if (id > 0) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "删除") }
            TextButton(onClick = save) { Text("保存") }
        }
    }) { pad ->
        ContentWidth(maxWidth = 760.dp, modifier = Modifier.padding(pad)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AiFillCard("AI 看图写日记", "选择照片，自动生成日记草稿、心情和标签", onClick = aiPick)
                DateField("日期", vm.date, { it?.let { d -> vm.date = d } })

                SectionTitle("今天的心情")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Moods.forEach { m ->
                        val sel = vm.mood == m.label
                        Surface(
                            onClick = { vm.mood = m.label }, shape = RoundedCornerShape(18.dp),
                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(Modifier.width(62.dp).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(m.emoji, fontSize = 26.sp)
                                Text(m.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                SectionTitle("标签")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (DefaultDiaryTags + vm.customTags).distinct().forEach { t ->
                        FilterChip(selected = t in vm.tags, onClick = { vm.toggleTag(t) }, label = { Text("#$t") })
                    }
                    AssistChip(onClick = { addTag = true }, label = { Text("自定义") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                }

                SectionTitle("写点什么")
                OutlinedTextField(
                    vm.content, { vm.content = it }, modifier = Modifier.fillMaxWidth(), minLines = 6,
                    placeholder = { Text("今天宝宝动了吗？身体感觉怎么样？有什么想对 TA 说的话…") },
                    shape = RoundedCornerShape(16.dp),
                )

                SectionTitle("照片")
                PhotoStrip(vm.photos, onAdd = photoPick, onRemove = { vm.photos.remove(it) })
                Button(onClick = save, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) { Text("保存日记") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (addTag) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addTag = false },
            title = { Text("添加标签") },
            text = { OutlinedTextField(text, { text = it.take(8) }, singleLine = true, placeholder = { Text("如：第一次胎动") }) },
            confirmButton = {
                TextButton(onClick = {
                    val t = text.trim().removePrefix("#")
                    if (t.isNotBlank()) { if (t !in vm.customTags && t !in DefaultDiaryTags) vm.customTags.add(t); if (t !in vm.tags) vm.tags.add(t) }
                    addTag = false
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { addTag = false }) { Text("取消") } },
        )
    }
    if (confirmDelete) ConfirmDialog("删除日记", "删除后无法恢复，确定删除吗？", onDismiss = { confirmDelete = false }) {
        scope.launch { vm.delete(); onDone() }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在看图写日记…")
    vm.aiError?.let { MessageDialog("生成失败", it) { vm.aiError = null } }
}
