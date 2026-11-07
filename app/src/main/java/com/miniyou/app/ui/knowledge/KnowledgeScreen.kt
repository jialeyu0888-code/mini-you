package com.miniyou.app.ui.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.content.ClassContent
import com.miniyou.app.data.content.Lesson
import com.miniyou.app.data.content.Remedy
import com.miniyou.app.data.content.RemedyContent
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MainTopBar
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout

object KnowledgeTab {
    const val REMEDIES = 0
    const val MINE = 1
    const val CLASSES = 2
}

@Composable
fun KnowledgeScreen(nav: NavController, initialTab: Int) {
    val app = app()
    val settings by app.settings.state.collectAsState()
    val layout = LocalWindowLayout.current
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedRemedy by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLesson by rememberSaveable { mutableStateOf<String?>(null) }
    val remedies = app.content.remedies
    val classes = app.content.classes
    val trimester = settings.lmp?.let { PregnancyInfo(it).trimesterId }

    val subtitle = when (tab) {
        KnowledgeTab.REMEDIES -> remedies.title
        KnowledgeTab.MINE -> "每个孕妈都不一样，记下真正对你有效的方法"
        else -> classes.title
    }
    AppScaffold(
        topBar = { MainTopBar("孕育百科", subtitle) },
        floatingActionButton = {
            if (tab == KnowledgeTab.MINE) {
                ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.myRemedyEdit()) },
                    icon = { Icon(Icons.Default.Add, null) }, text = { Text("记录我的方法") })
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(tab == KnowledgeTab.REMEDIES, { tab = KnowledgeTab.REMEDIES; query = "" }, text = { Text("🌿 缓解攻略") })
                Tab(tab == KnowledgeTab.MINE, { tab = KnowledgeTab.MINE; query = "" }, text = { Text("💗 我的攻略") })
                Tab(tab == KnowledgeTab.CLASSES, { tab = KnowledgeTab.CLASSES; query = "" }, text = { Text("👶 准爸妈课堂") })
            }
            OutlinedTextField(
                query, { query = it }, singleLine = true,
                placeholder = {
                    Text(
                        when (tab) {
                            KnowledgeTab.REMEDIES -> "搜索症状，如 孕吐、腿抽筋、失眠"
                            KnowledgeTab.MINE -> "搜索我记录的方法"
                            else -> "搜索课程，如 洗澡、拍嗝、黄疸"
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "清除") } },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = layout.horizontalPadding, vertical = 10.dp),
            )
            if (tab == KnowledgeTab.MINE) {
                MyRemediesList(nav, remedies, query)
            } else if (layout.expanded) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(0.42f).fillMaxHeight()) {
                        if (tab == KnowledgeTab.REMEDIES) RemedyList(remedies, query, trimester, selectedRemedy) { selectedRemedy = it.id }
                        else LessonList(classes, query, selectedLesson) { selectedLesson = it.id }
                    }
                    VerticalDivider()
                    Box(Modifier.weight(0.58f).fillMaxHeight()) {
                        if (tab == KnowledgeTab.REMEDIES) {
                            val r = remedies.remedies.firstOrNull { it.id == selectedRemedy } ?: remedies.remedies.firstOrNull()
                            if (r != null) RemedyDetailContent(r, remedies, nav)
                        } else {
                            val l = classes.lesson(selectedLesson.orEmpty()) ?: classes.lessons.firstOrNull()
                            if (l != null) LessonDetailContent(l, classes)
                        }
                    }
                }
            } else {
                if (tab == KnowledgeTab.REMEDIES) RemedyList(remedies, query, trimester, null) { nav.navigate(Routes.remedy(it.id)) }
                else LessonList(classes, query, null) { nav.navigate(Routes.lesson(it.id)) }
            }
        }
    }

    if (!settings.knowledgeDisclaimerSeen) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("阅读前请注意") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("缓解攻略", style = MaterialTheme.typography.titleSmall)
                    Text(remedies.disclaimer, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("准爸妈课堂", style = MaterialTheme.typography.titleSmall)
                    Text(classes.disclaimer, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { app.settings.update { it.copy(knowledgeDisclaimerSeen = true) } }) { Text("我已了解") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemedyList(content: RemedyContent, query: String, currentTrimester: String?, selected: String?, onOpen: (Remedy) -> Unit) {
    val layout = LocalWindowLayout.current
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var trimester by rememberSaveable { mutableStateOf<String?>(null) }
    val list = content.remedies.filter {
        it.matches(query) && (category == null || it.category == category) && (trimester == null || trimester in it.trimesters)
    }
    LazyColumn(
        contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(category == null, { category = null }, label = { Text("全部症状") }) }
                items(content.categories) { c -> FilterChip(category == c.id, { category = if (category == c.id) null else c.id }, label = { Text(c.label) }) }
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(trimester == null, { trimester = null }, label = { Text("全孕期") }) }
                items(content.trimesters) { t ->
                    FilterChip(trimester == t.id, { trimester = if (trimester == t.id) null else t.id },
                        label = { Text(t.label + if (t.id == currentTrimester) "（当前）" else "") })
                }
            }
        }
        if (list.isEmpty()) item { EmptyState("🔍", "没有找到相关症状", "换个关键词试试，比如「抽筋」「烧心」") }
        items(list, key = { it.id }) { r ->
            SoftCard(
                onClick = { onOpen(r) },
                color = if (r.id == selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.symptom, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (currentTrimester != null && currentTrimester in r.trimesters) {
                        Pill("当前孕期常见", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Text(r.overview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Pill(content.categoryLabel(r.category), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                    r.trimesters.forEach { Pill(content.trimesterLabel(it)) }
                    Pill("${r.methods.size} 个方法", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    if (r.medicationCount > 0) RxBadge("含 ${r.medicationCount} 项${content.medicationNotice}")
                }
            }
        }
    }
}

@Composable
private fun LessonList(content: ClassContent, query: String, selected: String?, onOpen: (Lesson) -> Unit) {
    val layout = LocalWindowLayout.current
    LazyColumn(
        contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { EmergencyBanner(content.emergencyNotice) }
        var any = false
        content.chapters.forEach { ch ->
            val lessons = content.lessonsOf(ch).filter { it.matches(query) }
            if (lessons.isEmpty()) return@forEach
            any = true
            item(key = "ch-${ch.id}") {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(ch.title, style = MaterialTheme.typography.titleLarge)
                    Text(ch.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(lessons, key = { it.id }) { l ->
                SoftCard(
                    onClick = { onOpen(l) },
                    color = if (l.id == selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(lessonEmoji(l), fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.title, style = MaterialTheme.typography.titleMedium)
                            Text(l.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill(if (l.audience == "mother") "产后妈妈 · ${l.ageLabel}" else l.ageLabel)
                                Pill("约 ${l.readMinutes} 分钟", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                if (l.table != null) Pill("含表格", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            }
        }
        if (!any) item { EmptyState("🔍", "没有找到相关课程", "换个关键词试试，比如「奶粉」「睡觉」") }
    }
}

fun lessonEmoji(l: Lesson) = when (l.chapterId) {
    "feeding" -> "🍼"
    "daily-care" -> "🛁"
    "sleep-soothing" -> "🌙"
    "health" -> "🩺"
    "development" -> "🌱"
    "postpartum" -> "💗"
    else -> "📖"
}
