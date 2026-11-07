package com.miniyou.app.ui.knowledge

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.miniyou.app.MiniYouApp
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.content.RemedyContent
import com.miniyou.app.data.db.MyRemedy
import com.miniyou.app.data.db.MyRemedyRating
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.AiFillCard
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.SectionTitle
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch

private fun ratingStars(r: Int) = "★".repeat(r.coerceIn(0, 2) + 1) + "☆".repeat(2 - r.coerceIn(0, 2))

@Composable
fun MyRemedyCard(item: MyRemedy, onClick: () -> Unit) {
    SoftCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Pill("${ratingStars(item.rating)} ${MyRemedyRating.labels[item.rating.coerceIn(0, 2)]}")
        }
        if (item.content.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(item.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MyRemediesList(nav: NavController, content: RemedyContent, query: String) {
    val app = app()
    val layout = LocalWindowLayout.current
    val mine by app.db.myRemedies().all().collectAsState(initial = emptyList())
    val favorites by app.db.myRemedies().favorites().collectAsState(initial = emptyList())
    val q = query.trim()
    val list = mine.filter { q.isEmpty() || it.title.contains(q, true) || it.content.contains(q, true) || it.symptom.contains(q, true) }
    val favMethods = favorites.mapNotNull { f ->
        val r = content.remedies.firstOrNull { it.id == f.remedyId } ?: return@mapNotNull null
        val m = r.methods.firstOrNull { it.id == f.methodId } ?: return@mapNotNull null
        Triple(r, m, f)
    }.filter { (r, m, _) -> q.isEmpty() || r.symptom.contains(q, true) || m.name.contains(q, true) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(layout.gridColumns),
        contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            AiFillCard("AI 识图记录攻略", "拍下笔记、医嘱或聊天截图，AI 帮你整理成一条攻略") {
                nav.navigate(Routes.myRemedyEdit(remedyId = AUTO_AI))
            }
        }
        if (list.isEmpty() && favMethods.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("💗", "还没有你的专属攻略", "每个孕妈都不一样。试过有用的方法就记下来，或在「缓解攻略」里把有效的方法标记为 ♥")
            }
        }
        list.groupBy { it.symptom }.forEach { (symptom, items) ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "h-$symptom") { SectionTitle("$symptom（${items.size}）") }
            items(items, key = { it.id }) { MyRemedyCard(it) { nav.navigate(Routes.myRemedyEdit(it.id)) } }
        }
        if (favMethods.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav-h") { SectionTitle("♥ 我标记有效的官方方法（${favMethods.size}）") }
            items(favMethods, key = { "fav-" + it.third.methodId }) { (r, m, _) ->
                SoftCard(onClick = { nav.navigate(Routes.remedy(r.id)) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♥ ${m.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        if (m.isMedication) RxBadge(content.medicationNotice)
                    }
                    Text("${r.symptom} · ${content.typeLabel(m.type)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(m.howTo, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** 作为 remedyId 传入时表示进入编辑页后立即打开 AI 识图。 */
const val AUTO_AI = "__ai__"

class MyRemedyEditVM(private val app: MiniYouApp, private val id: Long, presetRemedyId: String) : ViewModel() {
    private val content = app.content.remedies
    var remedyId by mutableStateOf(presetRemedyId.takeIf { it != AUTO_AI }.orEmpty())
    var customSymptom by mutableStateOf("")
    var title by mutableStateOf("")
    var body by mutableStateOf("")
    var rating by mutableIntStateOf(1)
    var autoAiPending = presetRemedyId == AUTO_AI
    private var existing: MyRemedy? = null
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)

    init {
        if (id > 0) viewModelScope.launch {
            app.db.myRemedies().get(id)?.let { m ->
                existing = m; remedyId = m.remedyId; title = m.title; body = m.content; rating = m.rating
                if (m.remedyId.isBlank()) customSymptom = m.symptom
            }
        }
    }

    val symptom: String get() = content.remedies.firstOrNull { it.id == remedyId }?.symptom ?: customSymptom.trim()
    val valid: Boolean get() = title.isNotBlank() && symptom.isNotBlank()

    fun runAi(uris: List<Uri>) {
        aiLoading = true
        viewModelScope.launch {
            try {
                val d = app.ai.parseMyRemedy(uris, content.remedies.map { it.symptom })
                val matched = content.remedies.firstOrNull { it.symptom == d.symptom }
                    ?: content.remedies.firstOrNull { r -> d.symptom.isNotBlank() && (r.aliases.any { d.symptom.contains(it) } || r.symptom.contains(d.symptom)) }
                if (matched != null) { remedyId = matched.id; customSymptom = "" }
                else if (d.symptom.isNotBlank()) { remedyId = ""; customSymptom = d.symptom }
                if (d.title.isNotBlank()) title = d.title
                if (d.content.isNotBlank()) body = d.content
            } catch (e: AiException) { aiError = e.message } catch (e: Exception) { aiError = "识别失败：${e.message}" }
            finally { aiLoading = false }
        }
    }

    suspend fun save() {
        val now = System.currentTimeMillis()
        app.db.myRemedies().upsert(
            (existing ?: MyRemedy(symptom = symptom, title = title)).copy(
                symptom = symptom, remedyId = remedyId, title = title.trim(), content = body.trim(), rating = rating, updatedAt = now,
            )
        )
    }

    suspend fun delete() { existing?.let { app.db.myRemedies().delete(it) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyRemedyEditScreen(id: Long, presetRemedyId: String, onDone: () -> Unit) {
    val app = app()
    val vm = viewModel(key = "myremedy-$id-$presetRemedyId") { MyRemedyEditVM(app, id, presetRemedyId) }
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    val content = app.content.remedies
    var confirmDelete by remember { mutableStateOf(false) }
    val aiPick = rememberPhotoPicker(multiple = true, title = "拍笔记 / 医嘱 / 聊天截图") { vm.runAi(it) }
    LaunchedEffect(Unit) { if (vm.autoAiPending) { vm.autoAiPending = false; aiPick() } }
    val save: () -> Unit = { if (vm.valid) scope.launch { vm.save(); onDone() } }

    AppScaffold(topBar = {
        BackTopBar(if (id > 0) "编辑我的攻略" else "记录我的方法", onDone) {
            if (id > 0) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "删除") }
            TextButton(onClick = save, enabled = vm.valid) { Text("保存") }
        }
    }) { pad ->
        ContentWidth(maxWidth = 720.dp, modifier = Modifier.padding(pad)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AiFillCard("AI 识图快速填写", "拍下笔记、医嘱或聊天截图，自动整理症状、方法和做法", onClick = aiPick)
                SectionTitle("针对哪种不适")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    content.remedies.forEach { r ->
                        FilterChip(vm.remedyId == r.id, { vm.remedyId = r.id; vm.customSymptom = "" }, label = { Text(r.symptom) })
                    }
                    FilterChip(vm.remedyId.isBlank(), { vm.remedyId = "" }, label = { Text("其他") })
                }
                if (vm.remedyId.isBlank()) {
                    OutlinedTextField(vm.customSymptom, { vm.customSymptom = it.take(16) }, label = { Text("症状名称") },
                        placeholder = { Text("如：手指发麻") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                }
                OutlinedTextField(vm.title, { vm.title = it.take(30) }, label = { Text("方法名称") }, placeholder = { Text("如：睡前泡脚 15 分钟") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(vm.body, { vm.body = it }, label = { Text("具体做法和感受") }, minLines = 4,
                    placeholder = { Text("怎么做、坚持多久、效果如何、有什么要注意的…") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                SectionTitle("对我的效果")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MyRemedyRating.labels.forEachIndexed { i, l ->
                        SegmentedButton(vm.rating == i, { vm.rating = i }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(l) }
                    }
                }
                Text("个人经验仅供自己参考。涉及药物或持续不适时，请先咨询医生。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = save, enabled = vm.valid, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) { Text("保存") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmDelete) ConfirmDialog("删除攻略", "确定删除「${vm.title}」吗？", onDismiss = { confirmDelete = false }) {
        scope.launch { vm.delete(); onDone() }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在整理攻略…")
    vm.aiError?.let { MessageDialog("识别失败", it) { vm.aiError = null } }
}
