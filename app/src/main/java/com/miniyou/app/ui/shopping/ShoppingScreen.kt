package com.miniyou.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.miniyou.app.data.db.ShoppingCategory
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.AiFillCard
import com.miniyou.app.ui.components.EmptyState
import com.miniyou.app.ui.components.MainTopBar
import com.miniyou.app.ui.components.Pill
import com.miniyou.app.ui.components.SoftCard
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch
import java.io.File

fun categoryEmoji(name: String) = when (name) {
    "待产包" -> "🧳"
    "新生儿服饰" -> "👕"
    "宝宝生活用品" -> "🛏️"
    "喂养用品" -> "🍼"
    "孕妇用品" -> "🤰"
    "洗护用品" -> "🛁"
    "出行用品" -> "🚼"
    "玩具早教" -> "🧸"
    "其他" -> "📦"
    else -> "🏷️"
}

fun money(v: Double) = if (v % 1.0 == 0.0) "¥%.0f".format(v) else "¥%.2f".format(v)

@Composable
fun ShoppingScreen(nav: NavController) {
    val app = app()
    val items by app.db.shopping().all().collectAsState(initial = emptyList())
    val categories by app.db.shopping().categories().collectAsState(initial = emptyList())
    val layout = LocalWindowLayout.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var manage by remember { mutableStateOf(false) }

    val shown = items.filter { it.status == tab && (category == null || it.category == category) }
    val spent = items.filter { it.status == ShoppingStatus.BOUGHT }.sumOf { it.total }
    val budget = items.filter { it.status != ShoppingStatus.BOUGHT }.sumOf { it.total }

    AppScaffold(
        topBar = {
            MainTopBar("购物记录", "共 ${items.size} 件") {
                TextButton(onClick = { manage = true }) { Icon(Icons.Outlined.Category, null); Text(" 分类") }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.shoppingEdit(status = tab)) }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("添加") })
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(layout.gridColumns),
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(start = layout.horizontalPadding, end = layout.horizontalPadding, top = 4.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SoftCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row {
                        Stat("已花费", money(spent), Modifier.weight(1f))
                        Stat("待购预算", money(budget), Modifier.weight(1f))
                        Stat("已买 / 全部", "${items.count { it.status == ShoppingStatus.BOUGHT }} / ${items.size}", Modifier.weight(1f))
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                AiFillCard("AI 识图批量导入", "最多 8 张。一张订单截图里的多件商品会逐条导入") {
                    nav.navigate(Routes.shoppingEdit(status = tab, ai = true))
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                        ShoppingStatus.labels.forEachIndexed { i, l ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text("$l ${items.count { it.status == i }}") })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(category == null, { category = null }, label = { Text("全部") }) }
                        items(categories, key = { it.name }) { c ->
                            val n = items.count { it.status == tab && it.category == c.name }
                            FilterChip(category == c.name, { category = if (category == c.name) null else c.name },
                                label = { Text("${categoryEmoji(c.name)} ${c.name}${if (n > 0) " $n" else ""}") })
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("🛍️", "这里还空空的", "拍商品照片或订单截图，AI 帮你快速记下名称、价格和渠道")
                }
            }
            items(shown, key = { it.id }) { item ->
                ShoppingCard(item, onClick = { nav.navigate(Routes.shoppingDetail(item.id)) }) {
                    scope.launch {
                        app.db.shopping().upsert(item.copy(status = (item.status + 1).coerceAtMost(2), updatedAt = System.currentTimeMillis()))
                    }
                }
            }
        }
    }

    if (manage) CategoryManager(categories, items) { manage = false }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ShoppingCard(item: ShoppingItem, onClick: () -> Unit, onAdvance: () -> Unit) {
    SoftCard(onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (item.photo != null) AsyncImage(File(item.photo), null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp))
                else Text(categoryEmoji(item.category), fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(item.category)
                    if (item.channel.isNotBlank()) Pill(item.channel, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(Modifier.height(4.dp))
                val priceText = item.price?.let { "${money(it)} × ${item.quantity}" } ?: "数量 ${item.quantity}"
                Text(priceText + (item.price?.let { "  合计 ${money(item.total)}" } ?: ""), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.status < ShoppingStatus.BOUGHT) {
                FilledTonalButton(onClick = onAdvance, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text(if (item.status == ShoppingStatus.PLANNED) "决定买" else "已买 ✓")
                }
            }
        }
    }
}

@Composable
private fun CategoryManager(categories: List<ShoppingCategory>, items: List<ShoppingItem>, onDismiss: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理分类") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(text, { text = it.take(10) }, singleLine = true, placeholder = { Text("新分类名称") }, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val n = text.trim()
                        if (n.isNotBlank()) scope.launch { app.db.shopping().insertCategories(listOf(ShoppingCategory(n, categories.size))) }
                        text = ""
                    }) { Text("添加") }
                }
                Spacer(Modifier.height(8.dp))
                categories.forEach { c ->
                    val used = items.count { it.category == c.name }
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${categoryEmoji(c.name)} ${c.name}", modifier = Modifier.weight(1f))
                        Text("$used 件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!c.builtin && used == 0) {
                            IconButton(onClick = { scope.launch { app.db.shopping().deleteCategory(c) } }) { Icon(Icons.Outlined.Delete, "删除") }
                        } else Spacer(Modifier.width(48.dp))
                    }
                }
                Text("内置分类和仍有商品的分类不能删除", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}
