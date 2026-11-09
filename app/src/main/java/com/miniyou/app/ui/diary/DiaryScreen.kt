package com.miniyou.app.ui.diary

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
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
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.DiaryEntry
import com.miniyou.app.data.shortCn
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout
import com.miniyou.app.ui.theme.ActionRose
import com.miniyou.app.ui.theme.DarkActionRose
import com.miniyou.app.ui.theme.OnDarkActionRose
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

data class Mood(val label: String, val emoji: String, @DrawableRes val iconRes: Int)

val Moods = listOf(
    Mood("开心", "😊", R.drawable.diary_emoji_happy),
    Mood("幸福", "🥰", R.drawable.diary_emoji_bliss),
    Mood("平静", "😌", R.drawable.diary_emoji_calm),
    Mood("期待", "🤩", R.drawable.diary_emoji_expect),
    Mood("疲惫", "😴", R.drawable.diary_emoji_tired),
    Mood("难受", "🤢", R.drawable.diary_emoji_sick),
    Mood("低落", "😢", R.drawable.diary_emoji_sad),
    Mood("烦躁", "😤", R.drawable.diary_emoji_annoyed),
    Mood("焦虑", "😰", R.drawable.diary_emoji_anxious),
)

fun moodEmoji(label: String): String = when {
    label.contains("疲惫") -> "😴"
    else -> Moods.firstOrNull { it.label == label }?.emoji ?: "😊"
}

@DrawableRes
fun moodDrawable(label: String): Int = when {
    label.contains("开心") -> R.drawable.diary_emoji_happy
    label.contains("幸福") -> R.drawable.diary_emoji_bliss
    label.contains("平静") -> R.drawable.diary_emoji_calm
    label.contains("期待") -> R.drawable.diary_emoji_expect
    label.contains("疲惫") -> R.drawable.diary_emoji_tired
    label.contains("难受") -> R.drawable.diary_emoji_sick
    label.contains("低落") -> R.drawable.diary_emoji_sad
    label.contains("烦躁") -> R.drawable.diary_emoji_annoyed
    label.contains("焦虑") -> R.drawable.diary_emoji_anxious
    else -> R.drawable.diary_emoji_happy
}

internal data class DiaryColors(
    val paper: Color,
    val ink: Color,
    val secondaryInk: Color,
    val accent: Color,
    val card: Color,
    val board: Color,
    val innerSheet: Color,
    val pillBg: Color,
    val pillInk: Color,
    val activeCoral: Color,
    val onActiveCoral: Color,
    val lightPinkMark: Color,
    val outline: Color,
    val SaturdayRed: Color,
)

internal val LightDiaryColors = DiaryColors(
    paper = Color(0xFFFDF3EC),
    ink = Color(0xFF562B1E),
    secondaryInk = Color(0xFF7D574E),
    accent = Color(0xFFB8453E),
    card = Color(0xFFFFFBF8),
    board = Color(0xFFF6D5D2),
    innerSheet = Color(0xFFFFF8F3),
    pillBg = Color(0xFFFCE2E3),
    pillInk = Color(0xFF6B3529),
    activeCoral = ActionRose,
    onActiveCoral = Color.White,
    lightPinkMark = Color(0xFFFCE2E3),
    outline = Color(0xFFEBD2C6),
    SaturdayRed = Color(0xFFD95B5B),
)

internal val DarkDiaryColors = DiaryColors(
    paper = Color(0xFF211815),
    ink = Color(0xFFFFE5D8),
    secondaryInk = Color(0xFFE0BDB2),
    accent = Color(0xFFFFAA9D),
    card = Color(0xFF30221E),
    board = Color(0xFF472D2B),
    innerSheet = Color(0xFF382723),
    pillBg = Color(0xFF593635),
    pillInk = Color(0xFFFFDFD8),
    activeCoral = DarkActionRose,
    onActiveCoral = OnDarkActionRose,
    lightPinkMark = Color(0xFF5B3837),
    outline = Color(0xFF624238),
    SaturdayRed = Color(0xFFFF9494),
)

internal val LocalDiaryColors = compositionLocalOf { LightDiaryColors }
internal val DiaryWarmShadow = Color(0xFFB88170).copy(alpha = 0.45f)
internal val DiaryDarkIllustration = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.76f, 0.68f, 0.64f, 1f) })

@Composable
internal fun DiaryPaperTexture(@DrawableRes res: Int, modifier: Modifier, alpha: Float) {
    val texture = ImageBitmap.imageResource(res)
    val brush = remember(texture) { ShaderBrush(ImageShader(texture, TileMode.Mirror, TileMode.Mirror)) }
    Box(modifier.background(brush, alpha = alpha))
}

@Composable
internal fun DiaryText(
    text: String,
    size: Int,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    center: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size.sp, lineHeight = (size + 5).sp, fontWeight = weight),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

internal fun Modifier.diaryCardShadow(shape: RoundedCornerShape): Modifier =
    fillMaxWidth().shadow(4.dp, shape, clip = false, ambientColor = DiaryWarmShadow, spotColor = DiaryWarmShadow)

private enum class DiaryFilter(val label: String, @DrawableRes val icon: Int) {
    ALL("全部日记", R.drawable.diary_filter_book),
    MOOD("心情", R.drawable.diary_filter_mood),
    BODY("身体感受", R.drawable.diary_filter_body),
    KICK("胎动", R.drawable.diary_filter_kick),
    OTHER("其他", R.drawable.diary_filter_other),
}

private fun DiaryEntry.matchesFilter(filter: DiaryFilter, query: String): Boolean {
    if (query.isNotBlank()) {
        val q = query.trim()
        val hit = content.contains(q, ignoreCase = true) ||
            mood.contains(q, ignoreCase = true) ||
            tags.any { it.contains(q, ignoreCase = true) }
        if (!hit) return false
    }
    return when (filter) {
        DiaryFilter.ALL -> true
        DiaryFilter.MOOD -> mood in listOf("开心", "幸福", "平静", "期待", "低落", "烦躁", "焦虑") &&
            tags.none { it in listOf("身体感受", "孕吐", "失眠", "睡眠") }
        DiaryFilter.BODY -> tags.any { it in listOf("身体感受", "孕吐", "失眠", "睡眠") } || mood.contains("疲惫") || mood == "难受"
        DiaryFilter.KICK -> tags.any { it.contains("胎动") } || content.contains("胎动")
        DiaryFilter.OTHER -> tags.any { it in listOf("产检", "小记录", "美食", "宝宝用品", "家人", "胎教", "散步", "运动") }
    }
}

@Composable
fun DiaryScreen(nav: NavController) {
    val app = app()
    val scope = rememberCoroutineScope()
    val entries by app.db.diary().all().collectAsState(initial = emptyList())
    val settings by app.settings.state.collectAsState()
    val layout = LocalWindowLayout.current
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkDiaryColors else LightDiaryColors

    val latestDate = remember(entries) { entries.maxOfOrNull { it.date }?.toDate() ?: LocalDate.now() }
    var selectedDay by rememberSaveable { mutableStateOf(latestDate.toEpochDay()) }
    var initializedLatest by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(entries) {
        if (!initializedLatest && entries.isNotEmpty()) {
            val maxDay = entries.maxOf { it.date }
            selectedDay = maxDay
            initializedLatest = true
        }
    }
    var month by remember(selectedDay) { mutableStateOf(YearMonth.from(selectedDay.toDate())) }
    var dayFilterOnly by rememberSaveable { mutableStateOf(false) }
    var activeFilter by rememberSaveable { mutableStateOf(DiaryFilter.ALL) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var viewingPhoto by remember { mutableStateOf<DiaryPhotoSelection?>(null) }

    val selected = selectedDay.toDate()
    val byDay = remember(entries) { entries.groupBy { it.date } }
    val info = settings.lmp?.let { PregnancyInfo(it, selected) }
    val gestationLabel = info?.gestationAt(selected)?.takeIf { it.isNotBlank() }?.let { "孕 $it 周" } ?: "孕 20+5 周"

    val monthEntries = remember(entries, month) {
        entries.filter { YearMonth.from(it.date.toDate()) == month }.sortedWith(compareByDescending<DiaryEntry> { it.date }.thenByDescending { it.id })
    }
    val displayedEntries = remember(entries, monthEntries, selectedDay, dayFilterOnly, activeFilter, searchQuery) {
        val base = if (dayFilterOnly) {
            byDay[selectedDay].orEmpty().sortedByDescending { it.id }
        } else if (monthEntries.isNotEmpty()) monthEntries else entries
        base.filter { it.matchesFilter(activeFilter, searchQuery) }
    }

    val edit: (DiaryEntry) -> Unit = { nav.navigate(Routes.diaryEdit(id = it.id)) }
    val delete: (DiaryEntry) -> Unit = { entry -> scope.launch { deleteDiaryEntry(app, entry) } }

    CompositionLocalProvider(LocalDiaryColors provides colors) {
        AppScaffold(containerColor = colors.paper) { pad ->
            val direction = LocalLayoutDirection.current
            Box(Modifier.fillMaxSize()) {
                if (!dark) {
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.verticalGradient(
                                0f to Color(0xFFFFF4E9),
                                0.55f to Color(0xFFFDF2EB),
                                1f to Color(0xFFFFF8EE),
                            ),
                        ),
                    )
                    DiaryPaperTexture(R.drawable.home_paper, Modifier.matchParentSize(), 0.30f)
                }

                // Bottom meadow anchored above bottom bar
                DiaryListMeadow(
                    expanded = layout.expanded,
                    dark = dark,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = pad.calculateBottomPadding()),
                )

                val calendarSection: @Composable () -> Unit = {
                    val calendarMarks = remember(monthEntries, selectedDay) {
                        // Show the most recent 2 marked days in calendar grid when >2 entries exist in the same week, or all marks
                        val sortedDays = monthEntries.map { it.date }.distinct().sortedDescending()
                        val displayDays = if (sortedDays.size > 2) sortedDays.take(2).toSet() else sortedDays.toSet()
                        byDay.filterKeys { it in displayDays }.mapValues { it.value.first().mood }
                    }
                    DiaryListCalendarCard(
                        month = month,
                        selected = selected,
                        marks = calendarMarks,
                        gestationLabel = gestationLabel,
                        dark = dark,
                        onSelect = { d ->
                            val day = d.toEpochDay()
                            if (selectedDay == day && dayFilterOnly) {
                                dayFilterOnly = false
                            } else {
                                selectedDay = day
                                dayFilterOnly = true
                            }
                        },
                        onMonth = { m ->
                            month = m
                            dayFilterOnly = false
                        },
                    )
                }

                val filterRow: @Composable () -> Unit = {
                    DiaryFilterBar(
                        active = activeFilter,
                        dayFilterOnly = dayFilterOnly,
                        selectedDate = selected,
                        onSelect = { activeFilter = it },
                        onClearDayFilter = { dayFilterOnly = false },
                    )
                }

                if (layout.expanded) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = pad.calculateStartPadding(direction),
                                top = pad.calculateTopPadding(),
                                end = pad.calculateEndPadding(direction),
                                bottom = pad.calculateBottomPadding(),
                            ),
                    ) {
                        DiaryListHeader(
                            dark = dark,
                            onSearch = { searchOpen = true },
                            onToday = {
                                val target = entries.maxOfOrNull { it.date }?.toDate() ?: LocalDate.now()
                                selectedDay = target.toEpochDay()
                                month = YearMonth.from(target)
                                dayFilterOnly = false
                            },
                        )
                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            LazyColumn(
                                Modifier.weight(0.46f).fillMaxHeight(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item { calendarSection() }
                                item { MonthMoodSummary(monthEntries) }
                            }
                            VerticalDivider(color = colors.outline.copy(alpha = 0.5f))
                            LazyColumn(
                                Modifier.weight(0.54f).fillMaxHeight(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item { filterRow() }
                                if (displayedEntries.isEmpty()) {
                                    item {
                                        EmptyDiaryCard(
                                            firstEntry = entries.isEmpty(),
                                            onWrite = { nav.navigate(Routes.diaryEdit(date = selectedDay)) },
                                        )
                                    }
                                } else {
                                    itemsIndexed(displayedEntries, key = { _, e -> e.id }) { idx, entry ->
                                        DiaryTimelineRow(
                                            entry = entry,
                                            index = idx,
                                            isFirst = idx == 0,
                                            isLast = idx == displayedEntries.lastIndex,
                                            settingsLmp = settings.lmp,
                                            dark = dark,
                                            onClick = { edit(entry) },
                                            onDelete = { delete(entry) },
                                            onPreviewPhoto = { viewingPhoto = DiaryPhotoSelection(entry.photos, it) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = pad.calculateStartPadding(direction),
                                top = pad.calculateTopPadding(),
                                end = pad.calculateEndPadding(direction),
                                bottom = pad.calculateBottomPadding(),
                            ),
                        contentPadding = PaddingValues(bottom = 86.dp),
                    ) {
                        item {
                            DiaryListHeader(
                                dark = dark,
                                onSearch = { searchOpen = true },
                                onToday = {
                                    val target = entries.maxOfOrNull { it.date }?.toDate() ?: LocalDate.now()
                                    selectedDay = target.toEpochDay()
                                    month = YearMonth.from(target)
                                    dayFilterOnly = false
                                },
                            )
                        }
                        item {
                            Box(Modifier.padding(horizontal = 14.dp)) {
                                calendarSection()
                            }
                        }
                        item {
                            Spacer(Modifier.height(7.dp))
                            filterRow()
                            Spacer(Modifier.height(6.dp))
                        }
                        if (displayedEntries.isEmpty()) {
                            item {
                                Box(Modifier.padding(horizontal = 14.dp)) {
                                    EmptyDiaryCard(
                                        firstEntry = entries.isEmpty(),
                                        onWrite = { nav.navigate(Routes.diaryEdit(date = selectedDay)) },
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(displayedEntries, key = { _, e -> e.id }) { idx, entry ->
                                Box(Modifier.padding(horizontal = 14.dp, vertical = 3.dp)) {
                                    DiaryTimelineRow(
                                        entry = entry,
                                        index = idx,
                                        isFirst = idx == 0,
                                        isLast = idx == displayedEntries.lastIndex,
                                        settingsLmp = settings.lmp,
                                        dark = dark,
                                        onClick = { edit(entry) },
                                        onDelete = { delete(entry) },
                                        onPreviewPhoto = { viewingPhoto = DiaryPhotoSelection(entry.photos, it) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (displayedEntries.isNotEmpty()) {
                    DiaryWriteFab(
                        onClick = { nav.navigate(Routes.diaryEdit(date = selectedDay)) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = 15.dp + pad.calculateEndPadding(direction),
                                bottom = 18.dp + pad.calculateBottomPadding(),
                            ),
                    )
                }
            }
        }

        if (searchOpen) {
            AlertDialog(
                onDismissRequest = { searchOpen = false },
                title = { Text("搜索日记") },
                text = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("输入关键词、心情或标签…") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "清空")
                                }
                            }
                        },
                    )
                },
                confirmButton = {
                    TextButton(onClick = { searchOpen = false }) { Text("完成") }
                },
                dismissButton = {
                    if (searchQuery.isNotEmpty()) {
                        TextButton(onClick = { searchQuery = ""; searchOpen = false }) { Text("重置") }
                    }
                },
            )
        }

        viewingPhoto?.let { selection ->
            DiaryPhotoViewer(selection) { viewingPhoto = null }
        }
    }
}

@Composable
private fun DiaryListHeader(
    dark: Boolean,
    onSearch: () -> Unit,
    onToday: () -> Unit,
) {
    val colors = LocalDiaryColors.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showArt = maxWidth >= 350.dp && LocalDensity.current.fontScale < 1.2f
        val artWidth = (maxWidth * 0.40f).coerceAtMost(212.dp)
        if (showArt) Box(Modifier.matchParentSize()) {
            Image(
                painter = painterResource(R.drawable.diary_list_hero),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 30.dp)
                    .wrapContentSize(Alignment.BottomEnd, unbounded = true)
                    .size(artWidth, artWidth * (248f / 409f)),
                alpha = if (dark) 0.78f else 1f,
                colorFilter = if (dark) DiaryDarkIllustration else null,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 90.dp)
                .padding(start = 20.dp, end = if (showArt) artWidth + 30.dp else 16.dp, bottom = 5.dp),
        ) {
            Image(
                painter = painterResource(if (dark) R.drawable.home_wordmark_dark else R.drawable.home_wordmark),
                contentDescription = "Mini YOU",
                modifier = Modifier.width(120.dp).height(32.dp),
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DiaryText("孕期日记", 24, colors.ink, weight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Image(
                    painter = painterResource(R.drawable.diary_title_hearts),
                    contentDescription = null,
                    modifier = Modifier.size(25.dp, 31.dp).offset(y = (-5).dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            DiaryText("记录这一段，和小小的你一起成长 ♡", 12, colors.secondaryInk, maxLines = 2)
        }
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
                Image(
                    painter = painterResource(R.drawable.diary_top_search),
                    contentDescription = "搜索日记",
                    modifier = Modifier.size(23.dp),
                    colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
                )
            }
            IconButton(onClick = onToday, modifier = Modifier.size(36.dp)) {
                Image(
                    painter = painterResource(R.drawable.diary_top_calendar),
                    contentDescription = "回到最新月份",
                    modifier = Modifier.size(23.dp),
                    colorFilter = if (dark) ColorFilter.tint(colors.ink) else null,
                )
            }
        }
    }
}

@Composable
private fun DiaryListCalendarCard(
    month: YearMonth,
    selected: LocalDate,
    marks: Map<Long, String>,
    gestationLabel: String,
    dark: Boolean,
    onSelect: (LocalDate) -> Unit,
    onMonth: (YearMonth) -> Unit,
) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier.diaryCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.65f)),
    ) {
        Box {
            if (!dark) {
                DiaryPaperTexture(R.drawable.home_paper, Modifier.matchParentSize(), 0.22f)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    MonthCalendar(
                        month = month,
                        selected = selected,
                        marks = marks,
                        showSelectedRays = true,
                        highlightSaturdays = true,
                        onSelect = onSelect,
                        onMonth = onMonth,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .width(102.dp)
                        .height(145.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.diary_calendar_polaroid),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alpha = if (dark) 0.85f else 1f,
                    )
                    Text(
                        text = gestationLabel,
                        style = TextStyle(
                            color = Color(0xFF6B3526),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 26.dp)
                            .rotate(-6f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryFilterBar(
    active: DiaryFilter,
    dayFilterOnly: Boolean,
    selectedDate: LocalDate,
    onSelect: (DiaryFilter) -> Unit,
    onClearDayFilter: () -> Unit,
) {
    val colors = LocalDiaryColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dayFilterOnly) {
            Surface(
                onClick = onClearDayFilter,
                shape = RoundedCornerShape(50),
                color = colors.activeCoral,
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DiaryText("${selectedDate.shortCn()} ✕", 12, colors.onActiveCoral, weight = FontWeight.Bold)
                }
            }
        }
        DiaryFilter.entries.forEach { filter ->
            val selected = active == filter && !dayFilterOnly
            val bg = if (selected) colors.activeCoral else colors.card
            val contentColor = if (selected) colors.onActiveCoral else colors.ink
            Surface(
                onClick = {
                    onClearDayFilter()
                    onSelect(filter)
                },
                shape = RoundedCornerShape(50),
                color = bg,
                border = if (selected) null else BorderStroke(1.dp, colors.outline),
                shadowElevation = if (selected) 2.dp else 1.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(
                        painter = painterResource(filter.icon),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        colorFilter = if (selected && filter != DiaryFilter.ALL) ColorFilter.tint(colors.onActiveCoral) else null,
                    )
                    DiaryText(
                        text = filter.label,
                        size = 12,
                        color = contentColor,
                        weight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaryTimelineRow(
    entry: DiaryEntry,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    settingsLmp: LocalDate?,
    dark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPreviewPhoto: (Int) -> Unit,
) {
    val colors = LocalDiaryColors.current
    val date = entry.date.toDate()
    val week = settingsLmp?.let { PregnancyInfo(it, date).gestationAt(date) }?.takeIf { it.isNotBlank() } ?: "20+5"
    val lineColor = colors.outline

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Left timeline axis + date column
        Box(Modifier.width(84.dp).padding(top = 6.dp)) {
            Canvas(
                Modifier
                    .width(16.dp)
                    .height(112.dp)
                    .align(Alignment.TopStart),
            ) {
                val cx = size.width / 2f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                if (!isFirst) {
                    drawLine(
                        color = lineColor,
                        start = Offset(cx, 0f),
                        end = Offset(cx, 12f),
                        strokeWidth = 2.5f,
                        pathEffect = dashEffect,
                    )
                }
                if (!isLast) {
                    drawLine(
                        color = lineColor,
                        start = Offset(cx, 24f),
                        end = Offset(cx, size.height),
                        strokeWidth = 2.5f,
                        pathEffect = dashEffect,
                    )
                }
            }
            // Marker dot or heart
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isFirst) {
                    Text("♥", color = colors.activeCoral, fontSize = 12.sp, lineHeight = 12.sp)
                } else {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0BEB5)),
                    )
                }
            }
            Column(
                Modifier
                    .padding(start = 15.dp)
                    .clickable { onClick() },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DiaryText(
                    text = "${date.monthValue}月${date.dayOfMonth}日",
                    size = 12,
                    color = colors.ink,
                    weight = FontWeight.Bold,
                    maxLines = 1,
                )
                DiaryText(
                    text = date.weekdayCn(),
                    size = 11,
                    color = colors.secondaryInk,
                    maxLines = 1,
                )
                Spacer(Modifier.height(1.dp))
                Surface(color = colors.pillBg, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "孕 $week 周",
                        style = TextStyle(color = colors.pillInk, fontSize = 9.5.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        maxLines = 1,
                    )
                }
            }
        }

        Box(Modifier.weight(1f)) {
            DiaryTimelineCard(
                entry = entry,
                index = index,
                dark = dark,
                onClick = onClick,
                onDelete = onDelete,
                onPreviewPhoto = onPreviewPhoto,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaryTimelineCard(
    entry: DiaryEntry,
    index: Int,
    dark: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onPreviewPhoto: (Int) -> Unit,
) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(20.dp)

    val moodTitle = when (entry.mood) {
        "疲惫" -> "有点疲惫"
        else -> entry.mood.ifBlank { "日记" }
    }
    val moodIcon = when {
        entry.mood.contains("开心") -> R.drawable.diary_mood_happy
        entry.mood.contains("平静") -> R.drawable.diary_mood_calm
        entry.mood.contains("疲惫") -> R.drawable.diary_mood_tired
        else -> moodDrawable(entry.mood)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.diaryCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
    ) {
        Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(moodIcon),
                            contentDescription = entry.mood,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DiaryText(moodTitle, 14, colors.ink, weight = FontWeight.Bold)
                                if (entry.mood == "开心") {
                                    Spacer(Modifier.width(4.dp))
                                    Image(
                                        painter = painterResource(R.drawable.diary_card_heart),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp, 16.dp),
                                    )
                                }
                            }
                            if (entry.tags.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    entry.tags.forEach { tag ->
                                        Surface(color = colors.pillBg, shape = RoundedCornerShape(50)) {
                                            DiaryText(
                                                text = "#$tag",
                                                size = 10,
                                                color = colors.pillInk,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp),
                                                weight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (entry.content.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.content,
                            style = TextStyle(color = colors.ink, fontSize = 11.sp, lineHeight = 15.5.sp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Right visual artwork / photo polaroid
                Box(
                    Modifier
                        .width(96.dp)
                        .height(82.dp)
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (entry.photos.isNotEmpty()) {
                        val pager = rememberPagerState { entry.photos.size }
                        HorizontalPager(
                            state = pager,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = entry.photos.size > 1,
                        ) { page ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Surface(
                                    onClick = { onPreviewPhoto(page) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(92.dp, 78.dp).rotate(if (index % 2 == 0) -3f else 3f),
                                ) {
                                    DiaryPhotoImage(
                                        photo = entry.photos[page],
                                        description = "日记照片 ${page + 1}/${entry.photos.size}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(6.dp)),
                                    )
                                }
                            }
                        }
                        if (entry.photos.size > 1) {
                            Text(
                                text = "${pager.currentPage + 1}/${entry.photos.size}",
                                color = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(start = 5.dp, bottom = 4.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    } else {
                        val artRes = when {
                            entry.mood == "开心" -> R.drawable.diary_card_polaroid_sunset
                            entry.tags.contains("产检") -> R.drawable.diary_card_polaroid_ultrasound
                            entry.mood.contains("疲惫") -> R.drawable.diary_card_cat
                            index % 3 == 0 -> R.drawable.diary_card_polaroid_sunset
                            index % 3 == 1 -> R.drawable.diary_card_polaroid_ultrasound
                            else -> R.drawable.diary_card_cat
                        }
                        Image(
                            painter = painterResource(artRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            alpha = if (dark) 0.86f else 1f,
                        )
                    }
                }
            }

            if (onDelete != null) {
                RecordActionsMenu(
                    recordName = "日记",
                    deleteMessage = "删除后无法恢复，确定删除吗？",
                    onEdit = onClick,
                    onDelete = onDelete,
                    tint = colors.secondaryInk,
                    modifier = Modifier.align(Alignment.TopEnd).size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyDiaryCard(firstEntry: Boolean, onWrite: () -> Unit) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().diaryCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DiaryText(
                if (firstEntry) "开始写第一篇孕期日记吧" else "还没有符合条件的孕期日记",
                14, colors.ink, weight = FontWeight.Bold,
            )
            if (firstEntry) {
                DiaryText("记下今天的心情、胎动或生活小事", 12, colors.secondaryInk)
            }
            Spacer(Modifier.height(8.dp))
            Surface(onClick = onWrite, color = colors.activeCoral, shape = RoundedCornerShape(50)) {
                DiaryText(
                    "写日记",
                    13,
                    colors.onActiveCoral,
                    Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    weight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DiaryWriteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier
            .shadow(6.dp, shape, clip = false, ambientColor = DiaryWarmShadow, spotColor = DiaryWarmShadow),
        shape = shape,
        color = colors.activeCoral,
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.diary_fab_pencil),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
            DiaryText("写日记", 16, colors.onActiveCoral, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiaryListMeadow(expanded: Boolean, dark: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().height(if (expanded) 96.dp else 76.dp)) {
        val width = maxWidth
        val height = width * (152f / 849f)
        Image(
            painter = painterResource(R.drawable.diary_meadow),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentSize(Alignment.BottomCenter, unbounded = true)
                .size(width, height),
            contentScale = ContentScale.FillBounds,
            alpha = if (dark) 0.36f else 1f,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthMoodSummary(list: List<DiaryEntry>) {
    if (list.isEmpty()) return
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier.diaryCardShadow(shape),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.diary_summary_sprout), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                DiaryText("本月小结 · ${list.size} 篇日记", 14, colors.ink, weight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                list.groupingBy { it.mood }.eachCount().entries.sortedByDescending { it.value }.forEach { (m, n) ->
                    Surface(color = colors.pillBg, shape = RoundedCornerShape(50)) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Image(painterResource(moodDrawable(m)), null, Modifier.size(15.dp))
                            DiaryText("$m $n 天", 12, colors.pillInk, weight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryCard(e: DiaryEntry, showDate: Boolean = false, onClick: () -> Unit) {
    DiaryTimelineCard(
        entry = e,
        index = 0,
        dark = isSystemInDarkTheme(),
        onClick = onClick,
        onDelete = null,
        onPreviewPhoto = {},
    )
}

@Composable
fun MonthCalendar(
    month: YearMonth,
    selected: LocalDate,
    marks: Map<Long, String>,
    showSelectedRays: Boolean = false,
    highlightSaturdays: Boolean = false,
    compact: Boolean = false,
    onSelect: (LocalDate) -> Unit,
    onMonth: (YearMonth) -> Unit,
) {
    val colors = LocalDiaryColors.current
    Column {
        Row(
            Modifier.fillMaxWidth().height(if (compact) 28.dp else 30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onMonth(month.minusMonths(1)) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上个月", tint = colors.ink)
            }
            Text(
                text = "${month.year} 年 ${month.monthValue} 月",
                style = TextStyle(color = colors.ink, fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { onMonth(month.plusMonths(1)) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下个月", tint = colors.ink)
            }
        }
        Spacer(Modifier.height(1.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Text(
                    text = w,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = TextStyle(color = colors.secondaryInk, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        val offset = month.atDay(1).dayOfWeek.value - 1
        val cells = offset + month.lengthOfMonth()
        val rowCount = (cells + 6) / 7
        (0 until rowCount).forEach { row ->
            val rowHasMark = (0 until 7).any { col ->
                val dNum = row * 7 + col - offset + 1
                dNum in 1..month.lengthOfMonth() && (marks[month.atDay(dNum).toEpochDay()] != null || month.atDay(dNum) == selected)
            }
            val rowHeight = when {
                compact && rowHasMark -> 27.dp
                compact -> 18.dp
                rowHasMark -> 33.dp
                else -> 23.dp
            }
            Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                (0 until 7).forEach { col ->
                    val dayNum = row * 7 + col - offset + 1
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            val markMood = marks[date.toEpochDay()]
                            val isSel = date == selected
                            val isSaturday = col == 5
                            val numColor = when {
                                isSel -> colors.onActiveCoral
                                highlightSaturdays && isSaturday && markMood == null -> colors.SaturdayRed
                                else -> colors.ink
                            }
                            val markDisplayIcon = when {
                                !isSel && markMood == "幸福" -> R.drawable.diary_emoji_happy
                                else -> moodDrawable(markMood ?: "开心")
                            }
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier
                                        .size(
                                            width = if (compact) 31.dp else 33.dp,
                                            height = if (markMood != null || isSel) (if (compact) 29.dp else 32.dp) else (if (compact) 19.dp else 21.dp),
                                        )
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            when {
                                                isSel -> colors.activeCoral
                                                markMood != null -> colors.lightPinkMark
                                                else -> Color.Transparent
                                            },
                                        )
                                        .clickable { onSelect(date) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 11.5.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = if (isSel || markMood != null) FontWeight.Bold else FontWeight.Medium,
                                            color = numColor,
                                        )
                                        if (markMood != null || isSel) {
                                            Image(
                                                painter = painterResource(markDisplayIcon),
                                                contentDescription = null,
                                                modifier = Modifier.size(if (compact) 11.5.dp else 12.5.dp),
                                            )
                                        }
                                    }
                                }
                                if (isSel && showSelectedRays) {
                                    Image(
                                        painter = painterResource(R.drawable.diary_calendar_rays),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-8).dp)
                                            .size(17.dp, 13.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
