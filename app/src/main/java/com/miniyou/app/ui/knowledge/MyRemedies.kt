package com.miniyou.app.ui.knowledge

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.miniyou.app.MiniYouApp
import com.miniyou.app.R
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.content.RemedyContent
import com.miniyou.app.data.db.MyRemedy
import com.miniyou.app.data.db.MyRemedyRating
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.DeleteRecordButton
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch

private fun ratingStars(r: Int) = "★".repeat(r.coerceIn(0, 2) + 1) + "☆".repeat(2 - r.coerceIn(0, 2))

@Composable
internal fun MyRemedyCard(item: MyRemedy, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalKnowledgeColors.current
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onEdit,
        modifier = Modifier.knowledgeCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KnowledgeArt(
                res = R.drawable.kb_my_remedy_eat,
                contentDescription = item.title,
                modifier = Modifier.size(76.dp, 88.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = colors.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    color = colors.tagPinkBg,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = "${ratingStars(item.rating)} ${MyRemedyRating.labels[item.rating.coerceIn(0, 2)]}",
                        color = colors.tagPinkInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                if (item.content.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.content,
                        color = colors.secondaryInk,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RecordActionsMenu(
                "攻略", "确定删除「${item.title}」吗？",
                onEdit = onEdit, onDelete = onDelete,
                modifier = Modifier.align(Alignment.Top), tint = colors.secondaryInk,
            )
        }
    }
}

@Composable
fun MyRemediesList(nav: NavController, content: RemedyContent, query: String) {
    val app = app()
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    val colors = LocalKnowledgeColors.current
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            KnowledgeAiBanner(
                title = "AI 识图记录攻略",
                subtitle = "拍下笔记、医嘱或聊天截图，AI 帮你整理成一条攻略",
                onClick = { nav.navigate(Routes.myRemedyEdit(remedyId = AUTO_AI)) },
            )
        }
        if (list.isEmpty() && favMethods.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("💗", "还没有你的专属攻略", "每个孕妈都不一样。试过有用的方法就记下来，或在「缓解攻略」里把有效的方法标记为 ♥")
            }
        }
        list.groupBy { it.symptom }.forEach { (symptom, items) ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "h-$symptom") {
                Text(
                    text = "$symptom（${items.size}）",
                    color = colors.ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }
            items(items, key = { it.id }) { remedy ->
                MyRemedyCard(
                    remedy,
                    onEdit = { nav.navigate(Routes.myRemedyEdit(remedy.id)) },
                    onDelete = { scope.launch { app.db.myRemedies().delete(remedy) } },
                )
            }
        }
        if (favMethods.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav-h") {
                Text(
                    text = "♥ 我标记有效的官方方法（${favMethods.size}）",
                    color = colors.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }
            items(favMethods, key = { "fav-" + it.third.methodId }) { (r, m, favorite) ->
                val shape = RoundedCornerShape(22.dp)
                Surface(
                    onClick = { nav.navigate(Routes.remedy(r.id)) },
                    modifier = Modifier.knowledgeCardShadow(shape),
                    shape = shape,
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "♥ ${m.name}",
                                color = colors.accentPink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            if (m.isMedication) RxBadge(content.medicationNotice)
                            RecordActionsMenu(
                                "有效标记", "确定取消「${m.name}」的有效标记吗？",
                                onDelete = { scope.launch { app.db.myRemedies().removeFavorite(favorite.methodId) } },
                                modifier = Modifier.align(Alignment.Top), tint = colors.secondaryInk,
                                deleteLabel = "取消有效标记", confirmLabel = "取消标记",
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${r.symptom} · ${content.typeLabel(m.type)}",
                            color = colors.secondaryInk,
                            fontSize = 12.5.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = m.howTo,
                            color = colors.ink,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
    private var existing by mutableStateOf<MyRemedy?>(null)
    val canDelete get() = existing != null
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)

    init {
        if (id > 0) viewModelScope.launch {
            app.db.myRemedies().get(id)?.let { m ->
                existing = m
                remedyId = m.remedyId
                title = m.title
                body = m.content
                rating = m.rating
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
                if (matched != null) {
                    remedyId = matched.id
                    customSymptom = ""
                } else if (d.symptom.isNotBlank()) {
                    remedyId = ""
                    customSymptom = d.symptom
                }
                if (d.title.isNotBlank()) title = d.title
                if (d.content.isNotBlank()) body = d.content
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
        val now = System.currentTimeMillis()
        app.db.myRemedies().upsert(
            (existing ?: MyRemedy(symptom = symptom, title = title)).copy(
                symptom = symptom,
                remedyId = remedyId,
                title = title.trim(),
                content = body.trim(),
                rating = rating,
                updatedAt = now,
            ),
        )
    }

    suspend fun delete() {
        existing?.let { app.db.myRemedies().delete(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyRemedyEditScreen(id: Long, presetRemedyId: String, onDone: () -> Unit) {
    val app = app()
    val vm = viewModel(key = "myremedy-$id-$presetRemedyId") { MyRemedyEditVM(app, id, presetRemedyId) }
    val scope = rememberCoroutineScope()
    val content = app.content.remedies
    var confirmDelete by remember { mutableStateOf(false) }
    val aiPick = rememberPhotoPicker(multiple = true, title = "拍笔记 / 医嘱 / 聊天截图") { vm.runAi(it) }
    LaunchedEffect(Unit) { if (vm.autoAiPending) { vm.autoAiPending = false; aiPick() } }
    val save: () -> Unit = {
        if (vm.valid) {
            scope.launch { vm.save(); onDone() }
        }
    }

    KnowledgeTheme {
        val colors = LocalKnowledgeColors.current
        AppScaffold(containerColor = colors.paper) { pad ->
            val direction = LocalLayoutDirection.current
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = pad.calculateStartPadding(direction),
                        end = pad.calculateEndPadding(direction),
                        bottom = pad.calculateBottomPadding(),
                    ),
            ) {
                KnowledgeHeroHeader(
                    title = if (id > 0) "编辑我的攻略" else "记录我的方法",
                    statusInset = pad.calculateTopPadding(),
                    onBack = onDone,
                    trailingAction = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (vm.canDelete) {
                                DeleteRecordButton("删除") { confirmDelete = true }
                                Spacer(Modifier.width(4.dp))
                            }
                            Surface(
                                onClick = save,
                                shape = RoundedCornerShape(50),
                                color = colors.tagPinkBg,
                                modifier = Modifier.semantics { contentDescription = "保存" },
                            ) {
                                Text(
                                    text = "保存",
                                    color = colors.accentPink,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }
                    },
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.45f))

                ContentWidth(maxWidth = 720.dp, modifier = Modifier.weight(1f)) {
                    Column(
                        Modifier
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KnowledgeAiBanner(
                            title = "AI 识图快速填写",
                            subtitle = "拍下笔记、医嘱或聊天截图，自动整理症状、方法和做法",
                            onClick = aiPick,
                        )

                        Text(
                            text = "针对哪种不适",
                            color = colors.ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            content.remedies.forEach { r ->
                                KnowledgeFilterChip(
                                    label = r.symptom,
                                    selected = vm.remedyId == r.id,
                                    onClick = {
                                        vm.remedyId = r.id
                                        vm.customSymptom = ""
                                    },
                                )
                            }
                            KnowledgeFilterChip(
                                label = "其他",
                                selected = vm.remedyId.isBlank(),
                                onClick = { vm.remedyId = "" },
                            )
                        }

                        if (vm.remedyId.isBlank()) {
                            KnowledgeInputField(
                                value = vm.customSymptom,
                                onValueChange = { vm.customSymptom = it.take(16) },
                                placeholder = "症状名称",
                                singleLine = true,
                                minHeight = 46.dp,
                            )
                        }

                        KnowledgeInputField(
                            value = vm.title,
                            onValueChange = { vm.title = it.take(30) },
                            placeholder = "方法名称",
                            singleLine = true,
                            minHeight = 46.dp,
                        )

                        KnowledgeInputField(
                            value = vm.body,
                            onValueChange = { vm.body = it },
                            placeholder = "具体做法和感受",
                            singleLine = false,
                            minHeight = 104.dp,
                        )

                        Text(
                            text = "对我的效果",
                            color = colors.ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        KnowledgeRatingSegmentedBar(
                            selectedRating = vm.rating,
                            onSelect = { vm.rating = it },
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        if (confirmDelete) {
            ConfirmDialog("删除攻略", "确定删除「${vm.title}」吗？", onDismiss = { confirmDelete = false }) {
                scope.launch { vm.delete(); onDone() }
            }
        }
        if (vm.aiLoading) AiProgressDialog("AI 正在整理攻略…")
        vm.aiError?.let { MessageDialog("识别失败", it) { vm.aiError = null } }
    }
}

@Composable
private fun KnowledgeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp,
) {
    val colors = LocalKnowledgeColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.secondaryInk.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(
                    color = colors.ink,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                ),
                cursorBrush = SolidColor(colors.accentPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = placeholder },
            )
        }
    }
}

@Composable
private fun KnowledgeRatingSegmentedBar(
    selectedRating: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalKnowledgeColors.current
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            MyRemedyRating.labels.forEachIndexed { idx, label ->
                val selected = selectedRating == idx
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selected) colors.chipSelectedBg else Color.Transparent)
                        .clickable(role = Role.RadioButton) { onSelect(idx) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.ink,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = label,
                            color = colors.ink,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
                if (idx < MyRemedyRating.labels.lastIndex) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(colors.outline),
                    )
                }
            }
        }
    }
}
