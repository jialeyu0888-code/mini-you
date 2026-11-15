package com.miniyou.app.ui.checkup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState as rememberRowScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.db.CheckupPlan
import com.miniyou.app.data.shortCn
import com.miniyou.app.data.toDate
import com.miniyou.app.data.weekdayCn
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.diary.DiaryPhotoImage
import com.miniyou.app.ui.diary.DiaryPhotoSelection
import com.miniyou.app.ui.diary.DiaryPhotoViewer
import com.miniyou.app.ui.theme.LocalWindowLayout
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@Composable
fun CheckupListScreen(nav: NavController) {
    val app = app()
    val scope = rememberCoroutineScope()
    val list by app.db.checkups().all().collectAsState(initial = null)
    val plans by app.checkupPlans.all.collectAsState(initial = emptyList())
    val settings by app.settings.state.collectAsState()
    val pregnancy = settings.lmp?.let { PregnancyInfo(it) }
    val layout = LocalWindowLayout.current
    val colors = prenatalColors()
    var selected by rememberSaveable { mutableLongStateOf(-1L) }
    var viewingPhotos by remember { mutableStateOf<DiaryPhotoSelection?>(null) }
    val today = LocalDate.now()
    val upcoming = plans.filter { it.date >= today.toEpochDay() }
    val next = upcoming.firstOrNull()

    CompositionLocalProvider(LocalPrenatalColors provides colors) {
        AppScaffold(containerColor = colors.paper) { pad ->
            val records = list
            val open: (Checkup) -> Unit = { if (layout.expanded) selected = it.id else nav.navigate(Routes.checkupDetail(it.id)) }
            val listPane: @Composable (Modifier) -> Unit = { modifier ->
                Box(modifier) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        item {
                            CheckupListHeader(
                                count = records?.size ?: 0,
                                dark = isSystemInDarkTheme(),
                            )
                        }
                        item {
                            WeekPlanCard(
                                pregnancy = pregnancy,
                                next = next,
                                today = today,
                                onOpenPlans = { nav.navigate(Routes.CHECKUP_PLANS) },
                                onAddPlan = { nav.navigate(Routes.checkupPlanEdit()) },
                                modifier = Modifier.padding(horizontal = 14.dp),
                            )
                        }
                        item {
                            AnalysisEntryCard(
                                onAnalyze = { nav.navigate(Routes.CHECKUP_ANALYSIS) },
                                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp),
                            )
                        }
                        item {
                            RecordsHeader(
                                onSchedule = { nav.navigate(Routes.SCHEDULE) },
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
                            )
                        }
                        if (records == null) {
                            item { Text("正在读取产检记录", Modifier.padding(20.dp), color = colors.muted, fontSize = 13.sp) }
                        } else if (records.isEmpty()) {
                            item {
                                PrenatalCard(Modifier.padding(horizontal = 14.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        PrenatalArt(R.drawable.checkup_form_bunny, Modifier.size(96.dp, 80.dp))
                                        Text("还没有产检记录", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            "点击右下角「记录产检」，拍一张产检单，AI 帮你填好",
                                            color = colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp),
                                        )
                                    }
                                }
                            }
                        } else {
                            items(records, key = { it.id }) { checkup ->
                                val week = checkup.gestationalWeek.ifBlank {
                                    pregnancy?.gestationAt(checkup.date.toDate()).orEmpty()
                                }
                                CheckupRecordCard(
                                    checkup, week,
                                    highlighted = selected == checkup.id && layout.expanded,
                                    onClick = { open(checkup) },
                                    onPreviewPhoto = { idx -> viewingPhotos = DiaryPhotoSelection(checkup.photos, idx) },
                                    onEdit = { nav.navigate(Routes.checkupEdit(checkup.id)) },
                                    onDelete = {
                                        scope.launch {
                                            app.checkupPlans.deleteCheckup(checkup)
                                            if (selected == checkup.id) selected = -1L
                                        }
                                    },
                                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RecordFab(onClick = { nav.navigate(Routes.checkupEdit()) })
                        AiFab(onClick = { nav.navigate(Routes.checkupEdit(ai = true)) })
                    }
                }
            }
            if (layout.expanded) {
                Row(Modifier.padding(pad).fillMaxSize()) {
                    listPane(Modifier.weight(0.46f).fillMaxHeight())
                    VerticalDivider(color = colors.outline)
                    Box(Modifier.weight(0.54f).fillMaxHeight()) {
                        val loaded = records.orEmpty()
                        val sel = loaded.firstOrNull { it.id == selected } ?: loaded.firstOrNull()
                        if (sel != null) CheckupDetailContent(sel, nav, onDeleted = { selected = -1 })
                        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("选择一条产检记录", color = colors.muted)
                        }
                    }
                }
            } else {
                listPane(Modifier.padding(pad).fillMaxSize())
            }
            viewingPhotos?.let { DiaryPhotoViewer(it) { viewingPhotos = null } }
        }
    }
}

@Composable
private fun CheckupListHeader(count: Int, dark: Boolean) {
    val colors = LocalPrenatalColors.current
    Box(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        PrenatalArt(
            R.drawable.checkup_list_hero,
            Modifier.align(Alignment.CenterEnd).padding(end = 4.dp, top = 6.dp).size(120.dp, 78.dp),
        )
        Column(Modifier.padding(start = 16.dp, top = 2.dp, end = 128.dp, bottom = 4.dp)) {
            ImageWordmark(dark)
            Text(
                "产检记录",
                color = colors.ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp,
            )
            Text(
                "共 $count 次产检",
                color = colors.ink.copy(alpha = 0.72f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ImageWordmark(dark: Boolean) {
    androidx.compose.foundation.Image(
        painterResource(if (dark) R.drawable.home_wordmark_dark else R.drawable.home_wordmark),
        "Mini YOU",
        Modifier.width(96.dp).height(24.dp),
    )
}

@Composable
private fun WeekPlanCard(
    pregnancy: PregnancyInfo?,
    next: CheckupPlan?,
    today: LocalDate,
    onOpenPlans: () -> Unit,
    onAddPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPrenatalColors.current
    PrenatalCard(
        modifier, color = colors.pinkCard, onClick = onOpenPlans,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                PrenatalArt(R.drawable.checkup_fetus, Modifier.size(30.dp))
            }
            Column(Modifier.padding(start = 8.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    pregnancy?.weekLabel ?: "完善孕期信息后显示孕周",
                    color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp, maxLines = 1,
                )
                if (next != null) {
                    val date = next.date.toDate()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrenatalIcon(R.drawable.checkup_ic_calendar, Modifier.size(13.dp))
                        Text(
                            "下次产检 ${date.shortCn()} ${date.weekdayCn()}",
                            Modifier.padding(start = 4.dp), color = colors.muted, fontSize = 12.sp, maxLines = 1,
                        )
                    }
                } else {
                    Text("还没有下次产检安排", color = colors.muted, fontSize = 12.sp, maxLines = 1)
                }
            }
            if (next != null) {
                val days = ChronoUnit.DAYS.between(today, next.date.toDate())
                Text(
                    daysUntilLabel(days),
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.accent).padding(horizontal = 8.dp, vertical = 3.dp),
                    color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
            } else {
                PrenatalTextAction("新增计划", onClick = onAddPlan)
            }
        }
    }
}

@Composable
private fun AnalysisEntryCard(onAnalyze: () -> Unit, modifier: Modifier) {
    val colors = LocalPrenatalColors.current
    PrenatalCard(modifier, onClick = onAnalyze, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrenatalIcon(R.drawable.checkup_ic_weight, Modifier.size(16.dp))
            Text("产检分析", Modifier.padding(start = 6.dp), color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "体重 · 血压 · 指标趋势",
                Modifier.padding(start = 8.dp).weight(1f),
                color = colors.muted, fontSize = 11.sp, maxLines = 1,
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AiFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPrenatalColors.current
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.card,
        border = BorderStroke(1.dp, colors.outline),
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrenatalArt(R.drawable.checkup_ai_camera, Modifier.size(24.dp, 20.dp))
            Text("AI 识图", Modifier.padding(start = 5.dp), color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecordsHeader(onSchedule: () -> Unit, modifier: Modifier) {
    val colors = LocalPrenatalColors.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PrenatalIcon(R.drawable.checkup_ic_calendar, Modifier.size(16.dp))
        Text("最近产检记录", Modifier.padding(start = 4.dp).weight(1f), color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.clickable(onClick = onSchedule).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PrenatalIcon(R.drawable.checkup_ic_plan, Modifier.size(14.dp))
            Text("产检时间表", Modifier.padding(start = 3.dp), color = colors.muted, fontSize = 12.sp)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CheckupRecordCard(
    checkup: Checkup,
    week: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onPreviewPhoto: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalPrenatalColors.current
    val date = checkup.date.toDate()
    PrenatalCard(
        modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        border = if (highlighted) androidx.compose.foundation.BorderStroke(1.5.dp, colors.accent) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(week.ifBlank { "—" }, color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("孕周", color = colors.muted, fontSize = 10.sp)
            }
            CheckupPhotoThumb(checkup, onPreviewPhoto, Modifier.padding(end = 4.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(checkup.checkupType.ifBlank { "常规产检" }, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrenatalIcon(R.drawable.checkup_ic_calendar, Modifier.size(12.dp))
                    Text(date.cn(), Modifier.padding(start = 3.dp), color = colors.muted, fontSize = 11.sp)
                }
                if (checkup.hospital.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrenatalIcon(R.drawable.checkup_ic_hospital, Modifier.size(12.dp))
                        Text(checkup.hospital, Modifier.padding(start = 3.dp), color = colors.muted, fontSize = 11.sp, maxLines = 1)
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberRowScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (checkup.weightKg.isNotBlank()) PrenatalPill("体重 ${checkup.weightKg}kg")
                    if (checkup.bpSystolic.isNotBlank()) PrenatalPill("血压 ${checkup.bpSystolic}/${checkup.bpDiastolic}")
                    if (checkup.fetalHeartRate.isNotBlank()) PrenatalPill("胎心 ${checkup.fetalHeartRate}")
                    if (checkup.indicators.isNotEmpty()) PrenatalPill("${checkup.indicators.size} 项指标")
                    val abnormal = checkup.indicators.count { it.abnormal }
                    if (abnormal > 0) PrenatalPill("$abnormal 项异常", colors.danger.copy(alpha = 0.14f), colors.danger)
                }
            }
            RecordActionsMenu(
                "产检记录", DeleteCheckupMessage,
                onEdit = onEdit, onDelete = onDelete,
                modifier = Modifier.align(Alignment.Top), tint = colors.muted,
            )
        }
    }
}

/** 列表卡左侧的产检单缩略图：有真实照片时显示照片（多图可滑动切换），否则回退彩绘。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CheckupPhotoThumb(checkup: Checkup, onPreview: (Int) -> Unit, modifier: Modifier) {
    if (checkup.photos.isEmpty()) {
        PrenatalArt(checkupKindArt(checkup.checkupType), modifier.padding(horizontal = 2.dp).size(36.dp, 32.dp))
        return
    }
    Box(modifier.width(48.dp).height(42.dp), contentAlignment = Alignment.Center) {
        if (checkup.photos.size == 1) {
            CheckupPhotoFrame(checkup.photos, 0, onPreview, Modifier.size(46.dp, 40.dp))
        } else {
            val pager = rememberPagerState { checkup.photos.size }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CheckupPhotoFrame(checkup.photos, page, onPreview, Modifier.size(46.dp, 40.dp))
                }
            }
            Text(
                "${pager.currentPage + 1}/${checkup.photos.size}",
                Modifier.align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 5.dp),
                color = Color.White,
                fontSize = 8.sp,
                lineHeight = 10.sp,
            )
        }
    }
}

@Composable
private fun CheckupPhotoFrame(photos: List<String>, page: Int, onPreview: (Int) -> Unit, modifier: Modifier) {
    Surface(onClick = { onPreview(page) }, modifier = modifier, shape = RoundedCornerShape(10.dp), color = Color.White, shadowElevation = 1.dp) {
        DiaryPhotoImage(
            photo = photos[page],
            description = "产检单 ${page + 1}/${photos.size}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(7.dp)),
        )
    }
}

@Composable
private fun RecordFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPrenatalColors.current
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.accent,
        shadowElevation = 8.dp,
    ) {
        Text(
            "＋ 记录产检",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            color = colors.onAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
