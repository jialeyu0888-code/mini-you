package com.miniyou.app.ui.shopping

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniyou.app.MiniYouApp
import com.miniyou.app.ai.AiException
import com.miniyou.app.ai.ShoppingDraft
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.db.ShoppingCategory
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.AiFillCard
import com.miniyou.app.ui.components.AiProgressDialog
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.ConfirmDialog
import com.miniyou.app.ui.components.DeleteRecordButton
import com.miniyou.app.ui.components.MessageDialog
import com.miniyou.app.ui.components.PhotoStrip
import com.miniyou.app.ui.components.SectionTitle
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.components.rememberPhotoPicker
import com.miniyou.app.ui.theme.ContentWidth
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch

val Channels = listOf("淘宝", "天猫", "京东", "拼多多", "抖音", "小红书", "母婴店", "超市", "亲友赠送")

const val SHOPPING_AI_MAX_IMAGES = 8

data class BatchLine(
    val key: Int,
    val name: String,
    val category: String,
    val status: Int,
    val price: String,
    val quantity: String,
    val channel: String,
    val note: String,
    val photo: String?,
)

class ShoppingEditVM(private val app: MiniYouApp, private val id: Long, initialStatus: Int, autoAi: Boolean) : ViewModel() {
    var autoAiPending = autoAi
    var name by mutableStateOf("")
    var status by mutableStateOf(initialStatus)
    var category by mutableStateOf("待产包")
    var price by mutableStateOf("")
    var quantity by mutableStateOf("1")
    var channel by mutableStateOf("")
    var note by mutableStateOf("")
    var photo by mutableStateOf<String?>(null)
    private var existing by mutableStateOf<ShoppingItem?>(null)
    val canDelete get() = existing != null
    val batch = mutableStateListOf<BatchLine>()
    private var nextKey = 1
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)
    var batchSummary by mutableStateOf<String?>(null)

    init {
        if (id > 0) viewModelScope.launch {
            app.db.shopping().get(id)?.let { i ->
                existing = i
                name = i.name
                status = if (initialStatus == ShoppingStatus.BOUGHT) ShoppingStatus.BOUGHT else i.status
                category = i.category
                price = i.price?.let(::fmt).orEmpty()
                quantity = i.quantity.toString(); channel = i.channel; note = i.note; photo = i.photo
            }
        }
    }

    private fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

    fun setPhoto(context: Context, uri: Uri) = viewModelScope.launch { ImageStore.importImage(context, uri)?.let { photo = it } }

    fun runAi(context: Context, uris: List<Uri>) {
        aiLoading = true
        viewModelScope.launch {
            try {
                val cats = app.db.shopping().categoriesOnce().map { it.name }
                val drafts = app.ai.parseShopping(uris, cats)
                if (drafts.isEmpty()) throw AiException("没有在图片中识别到商品")
                val paths = uris.map { ImageStore.importImage(context, it) }
                if (drafts.size == 1 && batch.isEmpty()) {
                    apply(drafts.first())
                    photo = paths.getOrNull(drafts.first().imageIndex - 1) ?: paths.firstOrNull()
                    batchSummary = null
                } else {
                    batch.addAll(drafts.map { d ->
                        BatchLine(
                            key = nextKey++,
                            name = d.name,
                            category = d.category.ifBlank { category },
                            status = d.status,
                            price = d.price?.let(::fmt).orEmpty(),
                            quantity = d.quantity.toString(),
                            channel = d.channel,
                            note = d.note,
                            photo = paths.getOrNull(d.imageIndex - 1) ?: paths.firstOrNull(),
                        )
                    })
                    batchSummary = "从 ${uris.size} 张图片里识别出 ${drafts.size} 件，可删改后再导入"
                }
            } catch (e: AiException) { aiError = e.message } catch (e: Exception) { aiError = "识别失败：${e.message}" }
            finally { aiLoading = false }
        }
    }

    fun updateBatch(index: Int, line: BatchLine) {
        if (index in batch.indices) batch[index] = line
    }

    private fun apply(d: ShoppingDraft) {
        name = d.name; category = d.category.ifBlank { category }; status = d.status
        price = d.price?.let(::fmt).orEmpty(); quantity = d.quantity.toString()
        if (d.channel.isNotBlank()) channel = d.channel
        note = d.note
    }

    private fun current() = (existing ?: ShoppingItem(name = "", category = category)).copy(
        name = name.trim(), status = status, category = category, price = price.toDoubleOrNull(),
        quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1, channel = channel.trim(), note = note.trim(),
        photo = photo, updatedAt = System.currentTimeMillis(),
    )

    suspend fun save() { app.db.shopping().upsert(current()) }

    /** 把识别出的每一件作为独立记录导入，不改动正在编辑的原记录。 */
    suspend fun importBatch() {
        val now = System.currentTimeMillis()
        val items = batch.mapNotNull { line ->
            val title = line.name.trim()
            if (title.isBlank()) return@mapNotNull null
            ShoppingItem(
                name = title,
                category = line.category.ifBlank { "其他" },
                status = line.status,
                price = line.price.toDoubleOrNull(),
                quantity = line.quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                channel = line.channel.trim(),
                note = line.note.trim(),
                photo = line.photo,
                createdAt = now,
                updatedAt = now,
            )
        }
        if (items.isEmpty()) throw AiException("没有可导入的商品")
        app.db.shopping().insertAll(items)
    }

    suspend fun delete() { existing?.let { app.db.shopping().delete(it) } }

    suspend fun addCategory(n: String) {
        app.db.shopping().insertCategories(listOf(ShoppingCategory(n, 100)))
        category = n
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingEditScreen(id: Long, status: Int, autoAi: Boolean = false, onDone: () -> Unit, onDeleted: () -> Unit) {
    ShopTheme {
    val colors = LocalShopColors.current
    val app = app()
    val context = LocalContext.current
    val vm = viewModel(key = "shop-$id-$status-$autoAi") { ShoppingEditVM(app, id, status, autoAi) }
    val categories by app.db.shopping().categories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    var addCat by remember { mutableStateOf(false) }
    var pickCategory by remember { mutableStateOf(false) }
    var pickQty by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val aiPick = rememberPhotoPicker(
        multiple = true,
        maxCount = SHOPPING_AI_MAX_IMAGES,
        title = "可多选，最多 $SHOPPING_AI_MAX_IMAGES 张。一张图里有多件也会逐条识别",
    ) { vm.runAi(context, it) }
    val photoPick = rememberPhotoPicker(title = "商品图片") { it.firstOrNull()?.let { u -> vm.setPhoto(context, u) } }
    LaunchedEffect(Unit) { if (vm.autoAiPending) { vm.autoAiPending = false; aiPick() } }
    var categoryFor by remember { mutableStateOf<Int?>(null) }
    val save: () -> Unit = {
        scope.launch {
            if (vm.batch.isNotEmpty()) vm.importBatch() else if (vm.name.isNotBlank()) vm.save() else return@launch
            onDone()
        }
    }
    val title = if (id > 0) "编辑商品" else "添加商品"

    AppScaffold(containerColor = colors.paper, topBar = {
        BackTopBar(title, onDone, containerColor = colors.paper) {
            if (vm.canDelete) DeleteRecordButton("删除商品") { confirmDelete = true }
        }
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad)) {
            ShopPaper(Modifier.matchParentSize())
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 660.dp).fillMaxSize().imePadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AiFillCard("AI 识图填写", "从商品图片或订单截图提取信息", onClick = aiPick)
                    if (vm.batch.isNotEmpty()) {
                        vm.batchSummary?.let { ShopText(it, 13, colors.secondary) }
                        vm.batch.forEachIndexed { index, line ->
                            BatchLineCard(
                                line = line,
                                onChange = { vm.updateBatch(index, it) },
                                onPickCategory = { categoryFor = index },
                                onRemove = { vm.batch.removeAt(index) },
                            )
                        }
                        TextButton(onClick = { vm.batch.clear(); vm.batchSummary = null }) { Text("清空识别结果，改为手动填写") }
                    } else {
                        ShopField("商品名称", vm.name, { vm.name = it })
                        ShopField("分类", vm.category, {}, readOnly = true, onClick = { pickCategory = true })
                        ShopText("新建分类", 12, colors.accent, modifier = Modifier.clickable { addCat = true })
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val priceLabel = if (vm.status == ShoppingStatus.BOUGHT) "实付单价（元）" else "预计单价（元）"
                            ShopField(priceLabel, vm.price, { vm.price = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.weight(1f), KeyboardType.Decimal)
                            Column(Modifier.weight(1f)) {
                                ShopText("数量", 12, colors.secondary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StepperButton("−") {
                                        val n = (vm.quantity.toIntOrNull() ?: 1) - 1
                                        if (n >= 1) vm.quantity = n.toString()
                                    }
                                    ShopText(vm.quantity.ifBlank { "1" }, 18, colors.ink, weight = FontWeight.Bold, modifier = Modifier.clickable { pickQty = true })
                                    StepperButton("+") {
                                        val n = (vm.quantity.toIntOrNull() ?: 1) + 1
                                        if (n <= 99) vm.quantity = n.toString()
                                    }
                                }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShoppingStatus.labels.forEachIndexed { i, label ->
                                val on = vm.status == i
                                Text(
                                    label,
                                    color = if (on) colors.onButton else colors.ink,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (on) colors.button else colors.card)
                                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                                        .clickable { vm.status = i }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                        ShopField("备注（可选）", vm.note, { vm.note = it }, placeholder = "如：收纳尿布、湿巾等日常用品")
                        ShopField("购买渠道", vm.channel, { vm.channel = it }, placeholder = "淘宝 / 京东 / 母婴店")
                        PhotoStrip(listOfNotNull(vm.photo), onAdd = if (vm.photo == null) photoPick else null, onRemove = { vm.photo = null })
                    }
                    Spacer(Modifier.height(8.dp))
                }
                val batchCount = vm.batch.count { it.name.isNotBlank() }
                Button(
                    onClick = save,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = if (vm.batch.isNotEmpty()) batchCount > 0 else vm.name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.button, contentColor = colors.onButton),
                ) {
                    Text(if (vm.batch.isNotEmpty()) "导入全部 $batchCount 件" else "保存")
                }
            }
        }
    }

    if (pickCategory) {
        CategoryPickerDialog(
            categories = categories.map { it.name }.ifEmpty { listOf(vm.category) },
            selected = vm.category,
            onDismiss = { pickCategory = false },
        ) { name ->
            vm.category = name
            pickCategory = false
        }
    }
    if (pickQty) {
        QuantityDialog(vm.quantity.toIntOrNull() ?: 1, onDismiss = { pickQty = false }) {
            vm.quantity = it.toString()
            pickQty = false
        }
    }

    if (addCat) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addCat = false },
            title = { Text("新建分类") },
            text = { OutlinedTextField(text, { text = it.take(10) }, singleLine = true, placeholder = { Text("如：月子用品") }) },
            confirmButton = { TextButton(onClick = { val n = text.trim(); if (n.isNotBlank()) scope.launch { vm.addCategory(n) }; addCat = false }) { Text("添加") } },
            dismissButton = { TextButton(onClick = { addCat = false }) { Text("取消") } },
        )
    }
    if (confirmDelete) ConfirmDialog("删除商品", "确定删除「${vm.name}」吗？", onDismiss = { confirmDelete = false }) {
        scope.launch { vm.delete(); onDeleted() }
    }
    categoryFor?.let { index ->
        val line = vm.batch.getOrNull(index)
        if (line != null) {
            AlertDialog(
                onDismissRequest = { categoryFor = null },
                title = { Text("选择分类") },
                text = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { c ->
                            FilterChip(
                                selected = line.category == c.name,
                                onClick = { vm.updateBatch(index, line.copy(category = c.name)); categoryFor = null },
                                label = { Text("${categoryEmoji(c.name)} ${c.name}") },
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { categoryFor = null }) { Text("关闭") } },
            )
        }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在识别商品…")
    vm.aiError?.let { MessageDialog("识别失败", it) { vm.aiError = null } }
    }
}

@Composable
private fun ShopField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    placeholder: String = "",
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalShopColors.current
    Column(modifier) {
        ShopText(label, 12, colors.secondary)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            if (readOnly) {
                ShopText(value.ifBlank { "请选择" }, 15, colors.ink)
            } else {
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = label != "备注（可选）",
                    textStyle = androidx.compose.ui.text.TextStyle(color = colors.ink, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) ShopText(placeholder, 14, colors.secondary)
                        inner()
                    },
                )
            }
        }
    }
}

@Composable
private fun BatchLineCard(
    line: BatchLine,
    onChange: (BatchLine) -> Unit,
    onPickCategory: () -> Unit,
    onRemove: () -> Unit,
) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (line.photo != null) {
                coil.compose.AsyncImage(
                    java.io.File(line.photo), null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(10.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, "移除") }
        }
        OutlinedTextField(line.name, { onChange(line.copy(name = it)) }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                line.price, { onChange(line.copy(price = it.filter { c -> c.isDigit() || c == '.' })) },
                label = { Text("单价") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                line.quantity, { onChange(line.copy(quantity = it.filter(Char::isDigit).take(4))) },
                label = { Text("数量") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ShoppingStatus.labels.forEachIndexed { i, label ->
                FilterChip(selected = line.status == i, onClick = { onChange(line.copy(status = i)) }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = onPickCategory, label = { Text("${categoryEmoji(line.category)} ${line.category.ifBlank { "选分类" }}") })
            if (line.channel.isNotBlank()) {
                FilterChip(selected = false, onClick = {}, label = { Text(line.channel) })
            }
        }
    }
}
