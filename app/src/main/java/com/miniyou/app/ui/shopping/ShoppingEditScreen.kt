package com.miniyou.app.ui.shopping

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    private var existing: ShoppingItem? = null
    val extraDrafts = mutableStateListOf<ShoppingDraft>()
    private var aiPhoto: String? = null
    var aiLoading by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)

    init {
        if (id > 0) viewModelScope.launch {
            app.db.shopping().get(id)?.let { i ->
                existing = i
                name = i.name; status = i.status; category = i.category; price = i.price?.let(::fmt).orEmpty()
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
                apply(drafts.first())
                extraDrafts.clear(); extraDrafts.addAll(drafts.drop(1))
                aiPhoto = ImageStore.importImage(context, uris.first())
                if (photo == null) photo = aiPhoto
            } catch (e: AiException) { aiError = e.message } catch (e: Exception) { aiError = "识别失败：${e.message}" }
            finally { aiLoading = false }
        }
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

    suspend fun saveAll() {
        save()
        app.db.shopping().insertAll(extraDrafts.map {
            ShoppingItem(name = it.name, category = it.category, status = it.status, price = it.price, quantity = it.quantity,
                channel = it.channel, note = it.note, photo = aiPhoto)
        })
    }

    suspend fun delete() { existing?.let { app.db.shopping().delete(it) } }

    suspend fun addCategory(n: String) {
        app.db.shopping().insertCategories(listOf(ShoppingCategory(n, 100)))
        category = n
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingEditScreen(id: Long, status: Int, autoAi: Boolean = false, onDone: () -> Unit) {
    val app = app()
    val context = LocalContext.current
    val vm = viewModel(key = "shop-$id-$status-$autoAi") { ShoppingEditVM(app, id, status, autoAi) }
    val categories by app.db.shopping().categories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val layout = LocalWindowLayout.current
    var addCat by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val aiPick = rememberPhotoPicker(title = "拍商品照片或选择订单截图") { vm.runAi(context, it) }
    val photoPick = rememberPhotoPicker(title = "商品图片") { it.firstOrNull()?.let { u -> vm.setPhoto(context, u) } }
    LaunchedEffect(Unit) { if (vm.autoAiPending) { vm.autoAiPending = false; aiPick() } }
    val save: () -> Unit = { if (vm.name.isNotBlank()) scope.launch { vm.save(); onDone() } }

    AppScaffold(topBar = {
        BackTopBar(if (id > 0) "编辑商品" else "添加商品", onDone) {
            if (id > 0) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "删除") }
            TextButton(onClick = save, enabled = vm.name.isNotBlank()) { Text("保存") }
        }
    }) { pad ->
        ContentWidth(maxWidth = 760.dp, modifier = Modifier.padding(pad)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AiFillCard("AI 识图快速填写", "拍商品或订单截图，自动识别名称、价格、数量、渠道", onClick = aiPick)
                if (vm.extraDrafts.isNotEmpty()) {
                    SoftCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text("识别到 ${vm.extraDrafts.size + 1} 件商品", style = MaterialTheme.typography.titleSmall)
                        Text("表单中是第 1 件，其余：" + vm.extraDrafts.joinToString("、") { it.name }, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { scope.launch { vm.saveAll(); onDone() } }, enabled = vm.name.isNotBlank()) {
                            Text("一键保存全部 ${vm.extraDrafts.size + 1} 件")
                        }
                    }
                }
                OutlinedTextField(vm.name, { vm.name = it }, label = { Text("商品名称") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))

                SectionTitle("状态")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ShoppingStatus.labels.forEachIndexed { i, l ->
                        SegmentedButton(vm.status == i, { vm.status = i }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(l) }
                    }
                }

                SectionTitle("分类")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { c ->
                        FilterChip(vm.category == c.name, { vm.category = c.name }, label = { Text("${categoryEmoji(c.name)} ${c.name}") })
                    }
                    AssistChip(onClick = { addCat = true }, label = { Text("自定义") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(vm.price, { vm.price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("单价（元）") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(vm.quantity, { vm.quantity = it.filter(Char::isDigit).take(4) }, label = { Text("数量") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                }

                SectionTitle("购买渠道")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Channels.forEach { ch -> FilterChip(vm.channel == ch, { vm.channel = if (vm.channel == ch) "" else ch }, label = { Text(ch) }) }
                }
                OutlinedTextField(vm.channel, { vm.channel = it }, label = { Text("渠道（可手动输入）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(vm.note, { vm.note = it }, label = { Text("备注（规格、尺码、颜色等）") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(14.dp))

                SectionTitle("商品图片")
                PhotoStrip(listOfNotNull(vm.photo), onAdd = if (vm.photo == null) photoPick else null, onRemove = { vm.photo = null })
                Button(onClick = save, enabled = vm.name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) { Text("保存") }
                Spacer(Modifier.height(24.dp))
            }
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
        scope.launch { vm.delete(); onDone() }
    }
    if (vm.aiLoading) AiProgressDialog("AI 正在识别商品…")
    vm.aiError?.let { MessageDialog("识别失败", it) { vm.aiError = null } }
}
