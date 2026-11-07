package com.miniyou.app.ui.belly

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.DateField
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.SoftCard
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
    val photos by app.db.belly().all().collectAsState(initial = null)
    val layout = LocalWindowLayout.current
    var viewing by remember { mutableStateOf<Int?>(null) }

    AppScaffold(
        topBar = { BackTopBar("孕肚记录", { nav.popBackStack() }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.bellyEdit()) },
                icon = { Icon(Icons.Default.AddAPhoto, null) }, text = { Text("记录孕肚") })
        },
    ) { pad ->
        val list = photos ?: return@AppScaffold
        LazyVerticalGrid(
            columns = GridCells.Adaptive(if (layout.expanded) 200.dp else 150.dp),
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, top = 4.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (list.size >= 2) GrowthCompare(list.last(), list.first())
                else SoftCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("📸 给孕肚拍张照吧", style = MaterialTheme.typography.titleMedium)
                    Text("建议每 1–2 周在同一时间、同一位置、同一角度拍一张，几个月后就能看到宝宝长大的过程。",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (list.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("🤰", "还没有孕肚照片", "点右下角「记录孕肚」，拍照或从相册选一张")
                }
            }
            items(list.size, key = { list[it].id }) { i ->
                BellyCard(list[i]) { viewing = i }
            }
        }
        viewing?.let { start ->
            BellyViewer(list, start, onDismiss = { viewing = null }, onEdit = { viewing = null; nav.navigate(Routes.bellyEdit(it.id)) })
        }
    }
}

@Composable
private fun GrowthCompare(first: BellyPhoto, latest: BellyPhoto) {
    SoftCard {
        Text("成长对比", style = MaterialTheme.typography.titleMedium)
        Text("从${weekLabel(first.week)}到${weekLabel(latest.week)}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 520.dp)) {
            listOf(first, latest).forEach { p ->
                Box(Modifier.weight(1f).aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp))) {
                    AsyncImage(File(p.photo), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    WeekBadge(p, Modifier.align(Alignment.BottomStart))
                }
            }
        }
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
private fun BellyCard(p: BellyPhoto, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            AsyncImage(File(p.photo), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            WeekBadge(p, Modifier.align(Alignment.BottomStart))
        }
        if (p.note.isNotBlank()) {
            Text(p.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp))
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
                ImageStore.delete(p.photo)
                app.db.belly().delete(p)
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
    var autoPickerShown = false
    private var existing: BellyPhoto? = null
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)

    private val info get() = app.settings.current.lmp?.let { PregnancyInfo(it) }

    init {
        if (id > 0) {
            autoPickerShown = true
            viewModelScope.launch {
                app.db.belly().get(id)?.let { p ->
                    existing = p; photo = p.photo; date = p.date.toDate(); week = p.week; note = p.note
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
        ))
    }

    suspend fun delete() {
        existing?.let { ImageStore.delete(it.photo); app.db.belly().delete(it) }
    }
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
    LaunchedEffect(Unit) { if (!vm.autoPickerShown) { vm.autoPickerShown = true; pick() } }
    val save: () -> Unit = { if (vm.photo != null) scope.launch { vm.save(); onDone() } }

    AppScaffold(topBar = {
        BackTopBar(if (id > 0) "编辑孕肚照" else "记录孕肚", onDone) {
            if (id > 0) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "删除") }
            TextButton(onClick = save, enabled = vm.photo != null) { Text("保存") }
        }
    }) { pad ->
        ContentWidth(maxWidth = 640.dp, modifier = Modifier.padding(pad)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(3f / 4f).align(Alignment.CenterHorizontally)
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("拍摄日期", vm.date, { it?.let(vm::onDateChange) }, Modifier.weight(1.3f))
                    OutlinedTextField(vm.week, { vm.week = it }, label = { Text("孕周（如 20+3）") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                }
                OutlinedTextField(
                    vm.note, { vm.note = it }, label = { Text("想对宝宝说的话（选填）") }, minLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                )
                FilledTonalButton(onClick = vm::aiCaption, enabled = vm.photo != null) {
                    Icon(Icons.Default.AutoAwesome, null); Text(" AI 看照片写一句纪念文案")
                }
                Button(onClick = save, enabled = vm.photo != null, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) {
                    Text("保存")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmDelete) ConfirmDialog("删除孕肚照", "删除后无法恢复，确定删除吗？", onDismiss = { confirmDelete = false }) {
        scope.launch { vm.delete(); onDone() }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在写纪念文案…")
    vm.aiError?.let { MessageDialog("生成失败", it) { vm.aiError = null } }
}
