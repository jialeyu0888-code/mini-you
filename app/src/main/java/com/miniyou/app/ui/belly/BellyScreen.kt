package com.miniyou.app.ui.belly

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.miniyou.app.MiniYouApp
import com.miniyou.app.ai.AiException
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.cn
import com.miniyou.app.data.db.BellyPhoto
import com.miniyou.app.data.toDate
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.DeleteRecordButton
import com.miniyou.app.ui.components.DateField
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

private fun weekLabel(week: String) = if (week.isBlank()) "孕周未填" else "孕 $week 周"

@Composable
fun BellyScreen(nav: NavController) {
    val app = app()
    val scope = rememberCoroutineScope()
    val photos by app.db.belly().all().collectAsState(initial = null)
    val layout = LocalWindowLayout.current
    var viewing by remember { mutableStateOf<Int?>(null) }

    val colors = com.miniyou.app.ui.checkup.prenatalColors()
    androidx.compose.runtime.CompositionLocalProvider(com.miniyou.app.ui.checkup.LocalPrenatalColors provides colors) {
        AppScaffold(containerColor = colors.paper) { pad ->
            val list = photos ?: return@AppScaffold
            Column(Modifier.fillMaxSize().padding(pad)) {
                com.miniyou.app.ui.checkup.PrenatalHeader("孕肚记录", { nav.popBackStack() }) {
                    com.miniyou.app.ui.checkup.PrenatalTextAction("记录孕肚") { nav.navigate(Routes.bellyEdit()) }
                }
                Text(
                    "共 ${list.size} 条记录",
                    Modifier.padding(start = 20.dp, bottom = 8.dp),
                    color = colors.muted, fontSize = 13.sp,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (layout.expanded) 2 else 1),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (list.size >= 2) GrowthCompare(list.last(), list.first())
                        else com.miniyou.app.ui.checkup.PrenatalCard(color = colors.pinkCard) {
                            Text("给孕肚拍张照吧", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "建议每 1–2 周在同一时间、同一位置、同一角度拍一张。",
                                color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    items(list.size, key = { list[it].id }) { i ->
                        val photo = list[i]
                        BellyCard(
                            photo,
                            onClick = { viewing = i },
                            onEdit = { nav.navigate(Routes.bellyEdit(photo.id)) },
                            onDelete = { scope.launch { deleteBellyPhoto(app, photo) } },
                        )
                    }
                }
            }
            viewing?.let { start ->
                BellyViewer(list, start, onDismiss = { viewing = null }, onEdit = { viewing = null; nav.navigate(Routes.bellyEdit(it.id)) })
            }
        }
    }
}

@Composable
private fun GrowthCompare(first: BellyPhoto, latest: BellyPhoto) {
    val colors = com.miniyou.app.ui.checkup.LocalPrenatalColors.current
    com.miniyou.app.ui.checkup.PrenatalCard(color = colors.pinkCard) {
        Text("孕肚成长", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "从${weekLabel(first.week)}到${weekLabel(latest.week)}",
            color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GrowthStat(signedChange(first.waistCm, latest.waistCm), "腹围变化", Modifier.weight(1f))
            GrowthStat(signedChange(first.weightKg, latest.weightKg), "体重变化", Modifier.weight(1f))
        }
    }
}

private fun signedChange(start: String, end: String): String {
    val a = start.toDoubleOrNull() ?: return "—"
    val b = end.toDoubleOrNull() ?: return "—"
    val diff = b - a
    return "%s%.1f".format(if (diff >= 0) "+" else "", diff)
}

@Composable
private fun GrowthStat(value: String, label: String, modifier: Modifier) {
    val colors = com.miniyou.app.ui.checkup.LocalPrenatalColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.muted, fontSize = 12.sp)
    }
}

@Composable
private fun WeekBadge(p: BellyPhoto, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column {
            Text(weekLabel(p.week), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(p.date.toDate().cn(), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun BellyCard(p: BellyPhoto, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = com.miniyou.app.ui.checkup.LocalPrenatalColors.current
    com.miniyou.app.ui.checkup.PrenatalCard(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                File(p.photo), null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(weekLabel(p.week), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(p.date.toDate().cn(), color = colors.muted, fontSize = 12.sp)
                if (p.note.isNotBlank()) {
                    Text(p.note, color = colors.ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            RecordActionsMenu(
                "孕肚照", "删除后无法恢复，确定删除吗？",
                onEdit = onEdit, onDelete = onDelete,
                modifier = Modifier.align(Alignment.Top), tint = colors.muted,
            )
        }
    }
}

@Composable
private fun BellyViewer(list: List<BellyPhoto>, start: Int, onDismiss: () -> Unit, onEdit: (BellyPhoto) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (list.size - 1).coerceAtLeast(0))) { list.size }
    var deleting by remember { mutableStateOf<BellyPhoto?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                val p = list[i]
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    AsyncImage(File(p.photo), null, contentScale = ContentScale.Fit, modifier = Modifier.weight(1f).fillMaxWidth())
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(weekLabel(p.week), color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(p.date.toDate().cn(), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        if (p.note.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(p.note, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = { onEdit(p) }) { Icon(Icons.Outlined.Edit, null, tint = Color.White); Text(" 编辑", color = Color.White) }
                            TextButton(onClick = { deleting = p }) { Icon(Icons.Outlined.Delete, null, tint = Color.White); Text(" 删除", color = Color.White) }
                            Spacer(Modifier.weight(1f))
                            Text("${i + 1} / ${list.size}", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White)
            }
        }
    }
    deleting?.let { p ->
        ConfirmDialog("删除孕肚照", "确定删除${weekLabel(p.week)}的这张照片吗？", onDismiss = { deleting = null }) {
            scope.launch {
                deleteBellyPhoto(app, p)
                if (list.size <= 1) onDismiss()
            }
        }
    }
}

class BellyEditVM(private val app: MiniYouApp, private val id: Long) : ViewModel() {
    var photo by mutableStateOf<String?>(null)
    var date by mutableStateOf(LocalDate.now())
    var week by mutableStateOf("")
    var note by mutableStateOf("")
    var waist by mutableStateOf("")
    var weight by mutableStateOf("")
    var autoPickerShown = false
    private var existing by mutableStateOf<BellyPhoto?>(null)
    val canDelete get() = existing != null
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)

    private val info get() = app.settings.current.lmp?.let { PregnancyInfo(it) }

    init {
        if (id > 0) {
            autoPickerShown = true
            viewModelScope.launch {
                app.db.belly().get(id)?.let { p ->
                    existing = p; photo = p.photo; date = p.date.toDate(); week = p.week; note = p.note
                    waist = p.waistCm; weight = p.weightKg
                }
            }
        } else week = info?.gestationAt(date).orEmpty()
    }

    fun onDateChange(d: LocalDate) {
        val auto = info?.gestationAt(date)
        date = d
        if (week.isBlank() || week == auto) week = info?.gestationAt(d).orEmpty()
    }

    fun setPhoto(context: Context, uri: Uri) = viewModelScope.launch {
        ImageStore.importImage(context, uri)?.let { photo = it }
    }

    fun aiCaption() {
        val p = photo ?: return
        aiLoading = true
        viewModelScope.launch {
            try {
                val c = app.ai.bellyCaption(listOf(Uri.fromFile(File(p))), week)
                if (c.isNotBlank()) note = c
            } catch (e: AiException) { aiError = e.message } catch (e: Exception) { aiError = "生成失败：${e.message}" }
            finally { aiLoading = false }
        }
    }

    suspend fun save() {
        val p = photo ?: return
        app.db.belly().upsert((existing ?: BellyPhoto(date = date.toEpochDay(), photo = p)).copy(
            date = date.toEpochDay(), week = week.trim(), photo = p, note = note.trim(),
            waistCm = waist.trim(), weightKg = weight.trim(),
        ))
    }

    suspend fun delete() {
        existing?.let { deleteBellyPhoto(app, it) }
    }
}

private suspend fun deleteBellyPhoto(app: MiniYouApp, photo: BellyPhoto) {
    app.db.belly().delete(photo)
    ImageStore.delete(photo.photo)
}

@Composable
fun BellyEditScreen(id: Long, onDone: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val vm = viewModel(key = "belly-$id") { BellyEditVM(app, id) }
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    var confirmDelete by remember { mutableStateOf(false) }
    val pick = rememberPhotoPicker(title = "拍孕肚或从相册选择") { it.firstOrNull()?.let { u -> vm.setPhoto(context, u) } }
    LaunchedEffect(Unit) { vm.autoPickerShown = true }
    val save: () -> Unit = { if (vm.photo != null) scope.launch { vm.save(); onDone() } }

    val colors = com.miniyou.app.ui.checkup.prenatalColors()
    androidx.compose.runtime.CompositionLocalProvider(com.miniyou.app.ui.checkup.LocalPrenatalColors provides colors) {
    AppScaffold(containerColor = colors.paper, topBar = {
        BackTopBar(if (id > 0) "编辑孕肚照" else "记录孕肚", onDone, containerColor = colors.paper) {
            if (vm.canDelete) com.miniyou.app.ui.checkup.PrenatalTextAction("删除", colors.danger) { confirmDelete = true }
        }
    }) { pad ->
        ContentWidth(maxWidth = 640.dp, modifier = Modifier.padding(pad).consumeWindowInsets(pad).imePadding()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.widthIn(max = 320.dp).fillMaxWidth()
                        .then(if (vm.photo == null) Modifier.height(164.dp) else Modifier.aspectRatio(3f / 4f))
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = pick),
                    contentAlignment = Alignment.Center,
                ) {
                    val p = vm.photo
                    if (p != null) {
                        AsyncImage(File(p), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Surface(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)) {
                            Text("点击更换照片", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("拍一张孕肚照", style = MaterialTheme.typography.titleMedium)
                            Text("或从相册选择", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                com.miniyou.app.ui.checkup.PrenatalDateField(
                    "拍摄日期", vm.date, { it?.let(vm::onDateChange) }, "请选择日期", required = true,
                )
                com.miniyou.app.ui.checkup.PrenatalInput("孕周", vm.week, { vm.week = it }, "例如 20+5")
                com.miniyou.app.ui.checkup.PrenatalInput(
                    "腹围（选填）", vm.waist, { vm.waist = it.filter { ch -> ch.isDigit() || ch == '.' } }, "例如 92.0 cm", numeric = true,
                )
                com.miniyou.app.ui.checkup.PrenatalInput(
                    "体重（选填）", vm.weight, { vm.weight = it.filter { ch -> ch.isDigit() || ch == '.' } }, "例如 54.8 kg", numeric = true,
                )
                com.miniyou.app.ui.checkup.PrenatalInput(
                    "记录此刻的感受（选填）", vm.note, { vm.note = it }, "比如：肚子又大了一点，宝宝在动～", minLines = 3,
                )
                FilledTonalButton(onClick = vm::aiCaption, enabled = vm.photo != null) {
                    Icon(Icons.Default.AutoAwesome, null); Text(" AI 看照片写一句纪念文案")
                }
                com.miniyou.app.ui.checkup.PrenatalButton("保存记录", save, enabled = vm.photo != null)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmDelete) com.miniyou.app.ui.checkup.PrenatalConfirmDialog("删除孕肚照", "删除后无法恢复，确定删除吗？", onDismiss = { confirmDelete = false }) {
        scope.launch { vm.delete(); onDone() }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在写纪念文案…")
    vm.aiError?.let { MessageDialog("生成失败", it) { vm.aiError = null } }
    }
}
