package com.miniyou.app.ui.knowledge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.content.ClassContent
import com.miniyou.app.data.content.Lesson
import com.miniyou.app.data.content.Remedy
import com.miniyou.app.data.content.RemedyContent
import com.miniyou.app.data.content.RemedyMethod
import com.miniyou.app.data.db.FavoriteMethod
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.BulletList
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.Mint
import com.miniyou.app.ui.theme.WarnRed
import kotlinx.coroutines.launch

private val WarnContainer = Color(0xFFFDECEC)
private val WarnText = Color(0xFF9B2C2C)

@Composable
fun RxBadge(text: String = "需遵医嘱") {
    val colors = LocalKnowledgeColors.current
    Surface(color = colors.rxOrange, shape = RoundedCornerShape(50)) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun WarningCard(title: String, items: List<String>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val colors = LocalKnowledgeColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.dangerBg,
        border = BorderStroke(1.dp, colors.dangerBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.dangerInk)
            Spacer(Modifier.height(6.dp))
            BulletList(items, color = colors.dangerInk)
        }
    }
}

@Composable
fun EmergencyBanner(text: String) {
    if (text.isBlank()) return
    val colors = LocalKnowledgeColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.dangerBg,
        border = BorderStroke(1.dp, colors.dangerBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KnowledgeArt(
                res = R.drawable.kb_ambulance,
                contentDescription = null,
                modifier = Modifier.size(44.dp, 36.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = colors.dangerInk,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun DisclaimerFooter(text: String) {
    val colors = LocalKnowledgeColors.current
    Surface(
        color = colors.cardSoft,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("免责声明", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.secondaryInk)
            Spacer(Modifier.height(4.dp))
            Text(text, fontSize = 12.sp, lineHeight = 18.sp, color = colors.secondaryInk)
        }
    }
}

@Composable
fun RemedyDetailScreen(id: String, nav: NavController, onBack: () -> Unit) {
    val content = app().content.remedies
    val r = content.remedies.firstOrNull { it.id == id }
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
                    title = r?.symptom ?: "缓解攻略",
                    statusInset = pad.calculateTopPadding(),
                    onBack = onBack,
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.45f))
                if (r != null) {
                    ContentWidth(modifier = Modifier.weight(1f)) {
                        RemedyDetailContent(r, content, nav, showTitle = false)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemedyDetailContent(
    r: Remedy,
    content: RemedyContent,
    nav: NavController,
    showTitle: Boolean = true,
) {
    val app = app()
    val scope = rememberCoroutineScope()
    val colors = LocalKnowledgeColors.current
    val mine by app.db.myRemedies().all().collectAsState(initial = emptyList())
    val favorites by app.db.myRemedies().favorites().collectAsState(initial = emptyList())
    val favIds = remember(favorites) { favorites.map { it.methodId }.toSet() }
    val myForThis = mine.filter { it.remedyId == r.id }
    val effectiveCount = myForThis.size + r.methods.count { it.id in favIds }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showTitle) {
            Text(r.symptom, color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KnowledgeTagPill(content.categoryLabel(r.category), colors.tagBlueBg, colors.tagBlueInk)
            r.trimesters.forEach { tId ->
                KnowledgeTagPill(content.trimesterLabel(tId), colors.tagPinkBg, colors.tagPinkInk)
            }
            r.aliases.filter { it != r.symptom }.take(4).forEach { alias ->
                KnowledgeTagPill("#$alias", colors.tagGrayBg, colors.tagGrayInk)
            }
        }

        // Overview Card with Symptom Illustration
        val overviewShape = RoundedCornerShape(22.dp)
        Surface(
            modifier = Modifier.knowledgeCardShadow(overviewShape),
            shape = overviewShape,
            color = colors.cardSoft,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KnowledgeArt(
                    res = symptomArtRes(r.id, r.category),
                    contentDescription = r.symptom,
                    modifier = Modifier.size(86.dp, 98.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = r.overview,
                        color = colors.ink,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                    )
                    if (r.trimesterNote.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF7CA7BE),
                                modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = r.trimesterNote,
                                color = colors.ink.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                lineHeight = 17.5.sp,
                            )
                        }
                    }
                }
            }
        }

        // Section: 对我有效的方法
        Row(verticalAlignment = Alignment.CenterVertically) {
            KnowledgeSectionIcon(R.drawable.kb_tab_heart, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "对我有效的方法（$effectiveCount）",
                color = colors.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardSoft,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (myForThis.isEmpty() && favIds.none { id -> r.methods.any { it.id == id } }) {
                    Text(
                        text = "每个孕妈都不一样。试过有用的方法，点下方方法卡片上的 ♡ 标记，或者记下你自己的小妙招。",
                        color = colors.secondaryInk,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
                r.methods.filter { it.id in favIds }.forEach { m ->
                    Text(
                        text = "♥ ${m.name}",
                        color = colors.accentPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                myForThis.forEach { item ->
                    key(item.id) {
                        MyRemedyCard(
                            item,
                            onEdit = { nav.navigate(Routes.myRemedyEdit(item.id)) },
                            onDelete = { scope.launch { app.db.myRemedies().delete(item) } },
                        )
                    }
                }
                Surface(
                    onClick = { nav.navigate(Routes.myRemedyEdit(remedyId = r.id)) },
                    shape = RoundedCornerShape(50),
                    color = colors.card.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, colors.accentPink.copy(alpha = 0.55f)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.kb_icon_write_hand),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "记录我自己的方法",
                            color = colors.accentPink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Method groups
        val groups = r.methods.groupBy { if (it.isMedication) "medication" else it.type }
        content.methodTypes.map { it.id }.plus(groups.keys).distinct().forEach { type ->
            val list = groups[type].orEmpty()
            if (list.isEmpty()) return@forEach
            Row(
                Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (type == "diet") {
                    Image(
                        painter = painterResource(R.drawable.kb_icon_salad),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp, 18.dp),
                    )
                } else {
                    Text(typeEmoji(type), fontSize = 18.sp)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${content.typeLabel(type)}（${list.size}）",
                    color = colors.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (type == "medication") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.dangerBg,
                    border = BorderStroke(1.dp, colors.dangerBorder),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        RxBadge(content.medicationNotice)
                        Spacer(Modifier.width(8.dp))
                        Text(content.medicationNoticeDetail, fontSize = 12.5.sp, lineHeight = 18.sp, color = colors.dangerInk)
                    }
                }
            }
            list.forEach { m ->
                MethodCard(m, content, m.id in favIds) {
                    scope.launch {
                        if (m.id in favIds) app.db.myRemedies().removeFavorite(m.id)
                        else app.db.myRemedies().addFavorite(FavoriteMethod(m.id, r.id))
                    }
                }
            }
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
private fun MethodCard(
    m: RemedyMethod,
    content: RemedyContent,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val colors = LocalKnowledgeColors.current
    val rx = m.isMedication
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier.knowledgeCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(
            width = if (rx) 1.5.dp else 1.dp,
            color = if (rx) colors.rxOrange else colors.outline.copy(alpha = 0.55f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (rx) {
                RxBadge(m.rxLabel ?: content.medicationNotice)
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = m.name,
                    color = colors.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                content.evidence(m.evidence)?.let { ev ->
                    val (bg, fg) = when (ev.id) {
                        "guideline" -> Mint.copy(alpha = 0.20f) to Color(0xFF1F6B52)
                        "experience" -> Color(0xFFE5EEFA) to Color(0xFF2B4C7E)
                        else -> colors.tagPeachBg to colors.tagPeachInk
                    }
                    KnowledgeTagPill(ev.label, bg, fg)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = m.howTo,
                color = colors.ink,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onToggleFavorite,
                shape = RoundedCornerShape(50),
                color = if (favorite) colors.accentPink else colors.tagPinkBg,
            ) {
                Text(
                    text = if (favorite) "♥ 对我有效" else "♡ 对我有效？",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (favorite) MaterialTheme.colorScheme.onPrimary else colors.ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
            if (m.cautions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = if (rx) colors.dangerBg else colors.cautionBg,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "注意事项",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rx) colors.dangerInk else colors.secondaryInk,
                        )
                        Spacer(Modifier.height(4.dp))
                        BulletList(m.cautions, color = if (rx) colors.dangerInk else colors.ink.copy(alpha = 0.88f))
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
                    title = l?.title ?: "准爸妈课堂",
                    statusInset = pad.calculateTopPadding(),
                    onBack = onBack,
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.45f))
                if (l != null) {
                    ContentWidth(modifier = Modifier.weight(1f)) {
                        LessonDetailContent(l, content, showTitle = false)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonDetailContent(l: Lesson, content: ClassContent, showTitle: Boolean = true) {
    val colors = LocalKnowledgeColors.current
    val isVaccineLesson = l.id == "vaccine-schedule"

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showTitle) {
            Text(l.title, color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KnowledgeTagPill(
                text = if (l.audience == "mother") "产后妈妈 · ${l.ageLabel}" else "宝宝 ${l.ageLabel}",
                bg = colors.tagPinkBg,
                fg = colors.tagPinkInk,
            )
            KnowledgeTagPill(
                text = "阅读约 ${l.readMinutes} 分钟",
                bg = colors.tagPeachBg,
                fg = colors.tagPeachInk,
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSoft,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KnowledgeArt(
                    if (isVaccineLesson) R.drawable.kb_step_calendar else R.drawable.kb_lesson_bottle,
                    null,
                    Modifier.size(56.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(l.summary, color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
        }

        if (l.steps.isNotEmpty()) {
            Text("步骤详解", color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            l.steps.forEach { s ->
                LessonStepCard(step = s, isVaccineLesson = isVaccineLesson)
            }
        }

        l.table?.let { t ->
            Row(
                Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.kb_icon_clipboard),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp, 25.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(0.92f)
                            .height(10.dp)
                            .background(colors.tagPinkBg, RoundedCornerShape(50)),
                    )
                    Text(
                        text = t.title,
                        color = colors.ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            val tableShape = RoundedCornerShape(24.dp)
            Surface(
                modifier = Modifier.knowledgeCardShadow(tableShape),
                shape = tableShape,
                color = colors.card,
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
            ) {
                Column(Modifier.padding(8.dp)) {
                    // Header pill bar
                    Surface(
                        color = colors.tagPinkBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            t.columns.forEachIndexed { idx, colName ->
                                val weight = when (idx) {
                                    0 -> 0.34f
                                    1 -> 0.46f
                                    else -> 0.20f
                                }
                                Text(
                                    text = colName,
                                    color = colors.ink,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(weight),
                                )
                            }
                        }
                    }

                    t.rows.forEachIndexed { rIdx, row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (rIdx % 2 == 1) colors.cardSoft.copy(alpha = 0.5f) else Color.Transparent,
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            row.forEachIndexed { cIdx, cell ->
                                val weight = when (cIdx) {
                                    0 -> 0.34f
                                    1 -> 0.46f
                                    else -> 0.20f
                                }
                                Text(
                                    text = cell,
                                    color = colors.ink,
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(weight),
                                )
                            }
                        }
                    }

                    if (t.note.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = colors.cardSoft,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    Modifier
                                        .padding(top = 2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(colors.badgePink),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("i", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = t.note,
                                    color = colors.ink.copy(alpha = 0.88f),
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (l.tips.isNotEmpty()) {
            Text("💡 小贴士", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = colors.tagPeachBg.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.padding(14.dp)) {
                    BulletList(l.tips, color = colors.ink)
                }
            }
        }
        if (l.myths.isNotEmpty()) {
            Text("🙅 常见误区", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            l.myths.forEach { m ->
                Surface(
                    color = colors.card,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("❌ ${m.myth}", fontSize = 13.5.sp, color = WarnText)
                        Spacer(Modifier.height(6.dp))
                        Text("✅ ${m.fact}", fontSize = 13.5.sp, color = Color(0xFF1F6B52))
                    }
                }
            }
        }
        WarningCard("🏥 何时就医", l.whenToSeeDoctor)
        EmergencyBanner(content.emergencyNotice)
        DisclaimerFooter(content.disclaimer)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun LessonStepCard(
    step: com.miniyou.app.data.content.LessonStep,
    isVaccineLesson: Boolean,
) {
    val colors = LocalKnowledgeColors.current
    val shape = RoundedCornerShape(22.dp)
    val stepArt = when {
        isVaccineLesson && step.order == 6 -> R.drawable.kb_step_arm
        isVaccineLesson && step.order == 7 -> R.drawable.kb_step_calendar
        else -> null
    }
    Surface(
        modifier = Modifier.knowledgeCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (stepArt != null) {
                KnowledgeArt(res = stepArt, modifier = Modifier.size(72.dp))
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(colors.badgePink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${step.order}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = step.title,
                        color = colors.ink,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = step.content,
                    color = colors.ink.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}
