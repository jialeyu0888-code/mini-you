package com.miniyou.app.ui.knowledge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.content.ClassContent
import com.miniyou.app.data.content.Lesson
import com.miniyou.app.data.content.Remedy
import com.miniyou.app.data.content.RemedyContent
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.EmptyState
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

    KnowledgeTheme {
        val colors = LocalKnowledgeColors.current
        AppScaffold(containerColor = colors.paper) { pad ->
            val direction = LocalLayoutDirection.current
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = pad.calculateStartPadding(direction),
                        end = pad.calculateEndPadding(direction),
                        bottom = pad.calculateBottomPadding(),
                    ),
            ) {
                Column(Modifier.fillMaxSize()) {
                    KnowledgeHeroHeader(
                        title = "孕育百科",
                        subtitle = subtitle,
                        statusInset = pad.calculateTopPadding(),
                    )
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.45f))
                    KnowledgeTabBar(
                        selectedTab = tab,
                        onSelectTab = {
                            tab = it
                            query = ""
                        },
                    )
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.45f))
                    Spacer(Modifier.height(8.dp))
                    KnowledgeSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = when (tab) {
                            KnowledgeTab.REMEDIES -> "搜索症状，如孕吐、腿抽筋、失眠"
                            KnowledgeTab.MINE -> "搜索我记录的方法"
                            else -> "搜索课程，如 洗澡、拍嗝、黄疸"
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))

                    if (tab == KnowledgeTab.MINE) {
                        Box(Modifier.weight(1f)) {
                            MyRemediesList(nav, remedies, query)
                        }
                    } else if (layout.expanded) {
                        Row(Modifier.weight(1f).fillMaxSize()) {
                            Box(Modifier.weight(0.44f).fillMaxHeight()) {
                                if (tab == KnowledgeTab.REMEDIES) {
                                    RemedyList(remedies, query, trimester, selectedRemedy) { selectedRemedy = it.id }
                                } else {
                                    LessonList(classes, query, selectedLesson) { selectedLesson = it.id }
                                }
                            }
                            VerticalDivider(color = colors.outline.copy(alpha = 0.5f))
                            Box(Modifier.weight(0.56f).fillMaxHeight()) {
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
                        Box(Modifier.weight(1f)) {
                            if (tab == KnowledgeTab.REMEDIES) {
                                RemedyList(remedies, query, trimester, null) { nav.navigate(Routes.remedy(it.id)) }
                            } else {
                                LessonList(classes, query, null) { nav.navigate(Routes.lesson(it.id)) }
                            }
                        }
                    }
                }

                if (tab == KnowledgeTab.MINE) {
                    Surface(
                        onClick = { nav.navigate(Routes.myRemedyEdit()) },
                        shape = RoundedCornerShape(50),
                        color = colors.fabPinkBg,
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 18.dp, bottom = 18.dp)
                            .semantics { contentDescription = "记录我的方法" },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = colors.ink,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = "记录我的方法",
                                color = colors.ink,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        if (!settings.knowledgeDisclaimerSeen) {
            KnowledgeDisclaimerDialog(
                remediesDisclaimer = remedies.disclaimer,
                classesDisclaimer = classes.disclaimer,
                onConfirm = { app.settings.update { it.copy(knowledgeDisclaimerSeen = true) } },
            )
        }
    }
}

@Composable
private fun KnowledgeTabBar(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    val colors = LocalKnowledgeColors.current
    val items = listOf(
        Triple(KnowledgeTab.REMEDIES, "缓解攻略", R.drawable.kb_tab_leaf),
        Triple(KnowledgeTab.MINE, "我的攻略", R.drawable.kb_tab_heart),
        Triple(KnowledgeTab.CLASSES, "准爸妈课堂", R.drawable.kb_tab_baby),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showIcons = maxWidth >= (365 * LocalDensity.current.fontScale).dp
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { idx, (id, label, icon) ->
                val selected = selectedTab == id
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab) { onSelectTab(id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier.heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (showIcons) KnowledgeSectionIcon(icon, Modifier.size(16.dp))
                        Text(
                            text = label,
                            color = colors.accentPink,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (selected) {
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.62f).height(3.dp)
                                .background(colors.accentPink, RoundedCornerShape(50)),
                        )
                    }
                }
                if (idx < items.lastIndex) {
                    Box(
                        Modifier.width(1.dp).height(22.dp).background(colors.outline.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun KnowledgeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKnowledgeColors.current
    Surface(
        modifier = modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(50),
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.secondaryInk,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.secondaryInk.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.accentPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = placeholder },
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除",
                        tint = colors.secondaryInk,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemedyList(
    content: RemedyContent,
    query: String,
    currentTrimester: String?,
    selected: String?,
    onOpen: (Remedy) -> Unit,
) {
    val colors = LocalKnowledgeColors.current
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var trimester by rememberSaveable { mutableStateOf<String?>(null) }
    val list = content.remedies.filter {
        it.matches(query) && (category == null || it.category == category) && (trimester == null || trimester in it.trimesters)
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        KnowledgeFilterChip(
                            label = "全部症状",
                            selected = category == null,
                            onClick = { category = null },
                        )
                    }
                    items(content.categories) { c ->
                        KnowledgeFilterChip(
                            label = c.label,
                            selected = category == c.id,
                            onClick = { category = if (category == c.id) null else c.id },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        KnowledgeFilterChip(
                            label = "全孕期",
                            selected = trimester == null,
                            onClick = { trimester = null },
                        )
                    }
                    items(content.trimesters) { t ->
                        val isCurrent = t.id == (currentTrimester ?: "second")
                        KnowledgeFilterChip(
                            label = t.label + if (isCurrent) "（当前）" else "",
                            selected = trimester == t.id,
                            onClick = { trimester = if (trimester == t.id) null else t.id },
                        )
                    }
                }
            }
        }
        if (list.isEmpty()) {
            item { EmptyState("🔍", "没有找到相关症状", "换个关键词试试，比如「抽筋」「烧心」") }
        }
        items(list, key = { it.id }) { r ->
            val shape = RoundedCornerShape(24.dp)
            val activeTrimester = currentTrimester ?: "second"
            Surface(
                onClick = { onOpen(r) },
                modifier = Modifier.knowledgeCardShadow(shape),
                shape = shape,
                color = if (r.id == selected) colors.cardSoft else colors.card,
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KnowledgeArt(
                        res = symptomArtRes(r.id, r.category),
                        contentDescription = r.symptom,
                        modifier = Modifier.size(76.dp, 94.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = r.symptom,
                                color = colors.ink,
                                fontSize = 17.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            if (activeTrimester in r.trimesters) {
                                Surface(
                                    color = colors.badgePink,
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        text = "当前孕期常见",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = r.overview,
                                color = colors.secondaryInk,
                                fontSize = 12.5.sp,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.secondaryInk,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            KnowledgeTagPill(content.categoryLabel(r.category), colors.tagBlueBg, colors.tagBlueInk)
                            r.trimesters.forEach { tId ->
                                KnowledgeTagPill(content.trimesterLabel(tId), colors.tagPinkBg, colors.tagPinkInk)
                            }
                            KnowledgeTagPill("${r.methods.size} 个方法", colors.tagPeachBg, colors.tagPeachInk)
                        }
                        if (r.medicationCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            RxBadge("含 ${r.medicationCount} 项${content.medicationNotice}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonList(
    content: ClassContent,
    query: String,
    selected: String?,
    onOpen: (Lesson) -> Unit,
) {
    val colors = LocalKnowledgeColors.current
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item { EmergencyBanner(content.emergencyNotice) }
        var any = false
        content.chapters.forEach { ch ->
            val lessons = content.lessonsOf(ch).filter { it.matches(query) }
            if (lessons.isEmpty()) return@forEach
            any = true
            item(key = "ch-${ch.id}") {
                Column(Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                    Text(
                        text = ch.title,
                        color = colors.ink,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = ch.description,
                        color = colors.secondaryInk,
                        fontSize = 13.sp,
                    )
                }
            }
            items(lessons, key = { it.id }) { l ->
                val shape = RoundedCornerShape(22.dp)
                Surface(
                    onClick = { onOpen(l) },
                    modifier = Modifier.knowledgeCardShadow(shape),
                    shape = shape,
                    color = if (l.id == selected) colors.cardSoft else colors.card,
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KnowledgeArt(
                            res = R.drawable.kb_lesson_bottle,
                            contentDescription = l.title,
                            modifier = Modifier.size(66.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = l.title,
                                color = colors.ink,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = l.summary,
                                color = colors.secondaryInk,
                                fontSize = 12.5.sp,
                                lineHeight = 15.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                KnowledgeTagPill(
                                    text = if (l.audience == "mother") "产后妈妈 · ${l.ageLabel}" else l.ageLabel,
                                    bg = colors.tagPinkBg,
                                    fg = colors.tagPinkInk,
                                )
                                KnowledgeTagPill(
                                    text = "约 ${l.readMinutes} 分钟",
                                    bg = colors.tagPeachBg,
                                    fg = colors.tagPeachInk,
                                )
                                if (l.table != null) {
                                    KnowledgeTagPill("含表格", colors.tagBlueBg, colors.tagBlueInk)
                                }
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.secondaryInk,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        if (!any) {
            item { EmptyState("🔍", "没有找到相关课程", "换个关键词试试，比如「奶粉」「睡觉」") }
        }
    }
}

@Composable
private fun KnowledgeDisclaimerDialog(
    remediesDisclaimer: String,
    classesDisclaimer: String,
    onConfirm: () -> Unit,
) {
    val colors = LocalKnowledgeColors.current
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = colors.cardSoft,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "阅读前请注意",
                                    color = colors.ink,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .width(108.dp)
                                        .height(4.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(colors.accentPink.copy(alpha = 0.75f), Color.Transparent),
                                            ),
                                            RoundedCornerShape(50),
                                        ),
                                )
                            }
                            KnowledgeArt(
                                res = R.drawable.kb_disclaimer_mom,
                                contentDescription = null,
                                modifier = Modifier.size(104.dp, 70.dp),
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        DisclaimerSectionCard(
                            icon = R.drawable.kb_tab_leaf,
                            title = "缓解攻略",
                            text = remediesDisclaimer,
                        )
                        Spacer(Modifier.height(10.dp))
                        DisclaimerSectionCard(
                            icon = R.drawable.kb_tab_baby,
                            title = "准爸妈课堂",
                            text = classesDisclaimer,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button, onClick = onConfirm)
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "我已了解",
                            color = colors.accentPink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerSectionCard(
    icon: Int,
    title: String,
    text: String,
) {
    val colors = LocalKnowledgeColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.card.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Surface(
                color = colors.tagPinkBg,
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KnowledgeSectionIcon(icon, Modifier.size(16.dp))
                    Text(
                        text = title,
                        color = colors.accentPink,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                color = colors.ink,
                fontSize = 12.5.sp,
                lineHeight = 16.5.sp,
            )
        }
    }
}

@Composable
internal fun KnowledgeSectionIcon(icon: Int, modifier: Modifier) {
    val vector = when (icon) {
        R.drawable.kb_tab_heart -> Icons.Filled.Favorite
        R.drawable.kb_tab_baby -> Icons.Filled.ChildCare
        else -> null
    }
    if (vector == null) {
        Image(painter = painterResource(icon), contentDescription = null, modifier = modifier)
    } else {
        Icon(vector, contentDescription = null, modifier = modifier, tint = LocalKnowledgeColors.current.accentPink)
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
