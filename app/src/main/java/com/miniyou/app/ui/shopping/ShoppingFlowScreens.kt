package com.miniyou.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.miniyou.app.R
import com.miniyou.app.data.db.ShoppingCategory
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.LocalWindowLayout
import kotlinx.coroutines.launch

@Composable
fun ShoppingBoardScreen(nav: NavController) {
    ShopTheme {
        val colors = LocalShopColors.current
        val app = app()
        val items by app.db.shopping().all().collectAsState(initial = emptyList())
        val marks = rememberShoppingMarks()
        val favorites by marks.favorites.collectAsState()
        val bought = items.filter { it.status == ShoppingStatus.BOUGHT }
        val open = items.filter { it.status != ShoppingStatus.BOUGHT }
        val spent = bought.sumOf { it.total }
        val budget = open.sumOf { it.total }
        AppScaffold(containerColor = colors.paper) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                ShopPaper(Modifier.matchParentSize())
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = colors.ink, modifier = Modifier.clickable { nav.popBackStack() })
                        ShopText("  Mini YOU", 16, colors.ink, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            ShopText("购物清单", 28, colors.ink, weight = FontWeight.Bold)
                            ShopText("为小小的你\n准备每一份需要", 13, colors.secondary)
                        }
                        ShopArt(R.drawable.shop_checklist_hero, null, Modifier.size(120.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.card).padding(vertical = 12.dp),
                    ) {
                        BoardStat("¥${spent.toInt()}", "已花费", Modifier.weight(1f))
                        BoardStat("¥${budget.toInt()}", "待购预算", Modifier.weight(1f))
                        BoardStat("${bought.size} / ${items.size}", "已买 / 全部", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.button)
                            .clickable { nav.navigate(Routes.shoppingEdit(ai = true)) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ShopText("✦  AI 识图批量导入", 15, colors.onButton, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        ShopText("订单截图逐条入清单 ›", 11, colors.onButton)
                    }
                    Spacer(Modifier.height(14.dp))
                    ShopText("常用入口", 14, colors.ink, weight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val entries = listOf(
                        Triple(R.drawable.shop_entry_list, "购物清单", "管理清单、数量、价格") to Routes.SHOPPING_LIST,
                        Triple(R.drawable.shop_entry_bought, "已购买", "已买 ${bought.size} 件") to Routes.SHOPPING_BOUGHT,
                        Triple(R.drawable.shop_entry_fav, "我的收藏", "收藏 ${favorites.size} 件") to "shopping?focus=fav",
                    )
                    entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (card, route) ->
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(colors.card)
                                        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                                        .clickable { nav.navigate(route) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ShopArt(card.first, null, Modifier.size(46.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        ShopText(card.second, 14, colors.ink, weight = FontWeight.Bold)
                                        ShopText(card.third, 11, colors.secondary, maxLines = 2)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardStat(value: String, label: String, modifier: Modifier) {
    val colors = LocalShopColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ShopText(value, 18, colors.accent, weight = FontWeight.Bold, center = true)
        ShopText(label, 11, colors.secondary, center = true)
    }
}

@Composable
fun ShoppingChecklistScreen(nav: NavController) {
    ShopTheme {
        val colors = LocalShopColors.current
        val app = app()
        val scope = rememberCoroutineScope()
        val layout = LocalWindowLayout.current
        val items by app.db.shopping().all().collectAsState(initial = emptyList())
        val categories by app.db.shopping().categories().collectAsState(initial = emptyList())
        var filter by remember { mutableStateOf("全部") }
        var manage by remember { mutableStateOf(false) }
        var qtyFor by remember { mutableStateOf<ShoppingItem?>(null) }
        var boughtName by remember { mutableStateOf<String?>(null) }
        val open = items.filter { it.status != ShoppingStatus.BOUGHT }
        val names = listOf("全部") + categories.map { it.name }.ifEmpty { open.map { it.category }.distinct() }
        val shown = if (filter == "全部") open else open.filter { it.category == filter }
        val groups = shown.groupBy { it.category.ifBlank { "其他" } }
        AppScaffold(containerColor = colors.paper) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                ShopPaper(Modifier.matchParentSize())
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = colors.ink, modifier = Modifier.clickable { nav.popBackStack() })
                        ShopText("购物清单", 18, colors.ink, weight = FontWeight.Bold, modifier = Modifier.weight(1f), center = true)
                        Icon(Icons.Outlined.MoreHoriz, "管理分类", tint = colors.ink, modifier = Modifier.padding(start = 8.dp).clickable { manage = true })
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                ShopText("购物清单", 26, colors.ink, weight = FontWeight.Bold)
                                ShopText("共 ${open.size} 件待购", 12, colors.secondary)
                            }
                            ShopArt(R.drawable.shop_discover_hero, null, Modifier.size(96.dp))
                        }
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            names.distinct().forEach { name ->
                                val count = if (name == "全部") open.size else open.count { it.category == name }
                                val on = filter == name
                                Text(
                                    if (name == "全部") "全部（$count）" else "$name（$count）",
                                    color = if (on) colors.onButton else colors.ink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (on) colors.button else colors.card)
                                        .border(1.dp, if (on) colors.button else colors.line, RoundedCornerShape(16.dp))
                                        .clickable { filter = name }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                        if (groups.isEmpty()) {
                            ShopText("清单还是空的。添加一件，或用 AI 识图导入订单。", 13, colors.secondary, modifier = Modifier.padding(24.dp), center = true)
                        }
                        val groupColumns = if (layout.expanded) 2 else 1
                        val entries = groups.entries.toList()
                        entries.chunked(groupColumns).forEach { rowGroups ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowGroups.forEach { (category, rows) ->
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .padding(bottom = 10.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(colors.card)
                                            .padding(10.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ShopArt(categoryArt(category), null, Modifier.size(28.dp))
                                            ShopText("  $category（${rows.size}）", 14, colors.ink, weight = FontWeight.Bold)
                                        }
                                        rows.forEach { item ->
                                            ChecklistRow(
                                                item = item,
                                                nav = nav,
                                                onCheck = {
                                                    scope.launch {
                                                        app.db.shopping().upsert(item.copy(status = ShoppingStatus.BOUGHT, updatedAt = System.currentTimeMillis()))
                                                    }
                                                    boughtName = item.name
                                                },
                                                onQty = { qtyFor = item },
                                                onDelta = { delta ->
                                                    val next = (item.quantity + delta).coerceIn(1, 99)
                                                    scope.launch {
                                                        app.db.shopping().upsert(item.copy(quantity = next, updatedAt = System.currentTimeMillis()))
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                                if (rowGroups.size < groupColumns) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    PinkButton(
                        "添加物品",
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { nav.navigate(Routes.shoppingEdit()) }
                }
            }
        }
        if (manage) CategoryManagerDialog(categories, items) { manage = false }
        qtyFor?.let { item ->
            QuantityDialog(item.quantity, onDismiss = { qtyFor = null }) { qty ->
                scope.launch { app.db.shopping().upsert(item.copy(quantity = qty, updatedAt = System.currentTimeMillis())) }
                qtyFor = null
            }
        }
        boughtName?.let { name -> BoughtDialog(name) { boughtName = null } }
    }
}

@Composable
private fun ChecklistRow(
    item: ShoppingItem,
    nav: NavController,
    onCheck: () -> Unit,
    onQty: () -> Unit,
    onDelta: (Int) -> Unit,
) {
    val colors = LocalShopColors.current
    val open = { nav.navigate(Routes.shoppingDetail(item.id)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ShopCheck(false, onCheck)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clickable(onClick = open)) { ShopThumb(item, Modifier.size(46.dp)) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).clickable(onClick = open)) {
            ShopText(item.name, 14, colors.ink, weight = FontWeight.Bold, maxLines = 1)
            ShopText(item.category, 11, colors.secondary, maxLines = 1)
            ShopText(item.price?.let(::money) ?: "待估价", 13, colors.accent, weight = FontWeight.Bold)
            if (item.status == ShoppingStatus.DECIDED) TagChip("决定未买")
        }
        Column(horizontalAlignment = Alignment.End) {
            key(item.id) { ShoppingItemActions(item, nav) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepperButton("−") { onDelta(-1) }
                Text(
                    "${item.quantity}",
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onQty).width(22.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                StepperButton("+") { onDelta(1) }
            }
        }
    }
}

@Composable
fun ShoppingBoughtScreen(nav: NavController) {
    ShopTheme {
        val colors = LocalShopColors.current
        val items by app().db.shopping().all().collectAsState(initial = emptyList())
        val bought = items.filter { it.status == ShoppingStatus.BOUGHT }
        AppScaffold(containerColor = colors.paper) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                ShopPaper(Modifier.matchParentSize())
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = colors.ink, modifier = Modifier.clickable { nav.popBackStack() })
                        ShopText("已购买", 18, colors.ink, weight = FontWeight.Bold, modifier = Modifier.weight(1f), center = true)
                        Spacer(Modifier.width(24.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            ShopText("已购买", 26, colors.ink, weight = FontWeight.Bold)
                            ShopText("已买来的物品\n都准备好啦", 13, colors.secondary)
                        }
                        ShopArt(R.drawable.shop_bought_header_bunny, null, Modifier.size(110.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (bought.isEmpty()) {
                        ShopText("还没有已购买的记录。在清单里勾选一件，就会出现在这里。", 13, colors.secondary, center = true)
                    }
                    bought.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.card)
                                .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                                .clickable { nav.navigate(Routes.shoppingDetail(item.id)) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ShopThumb(item, Modifier.size(48.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                ShopText(item.name, 14, colors.ink, weight = FontWeight.Bold, maxLines = 1)
                                ShopText(
                                    "${item.price?.let(::money) ?: "—"} × ${item.quantity}  合计 ${money(item.total)}",
                                    12, colors.secondary,
                                )
                            }
                            ShopText("已买", 12, colors.accent, weight = FontWeight.Bold)
                            key(item.id) { ShoppingItemActions(item, nav, Modifier.align(Alignment.Top)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryManagerDialog(
    categories: List<ShoppingCategory>,
    items: List<ShoppingItem>,
    onDismiss: () -> Unit,
) {
    val app = app()
    val scope = rememberCoroutineScope()
    val colors = LocalShopColors.current
    var text by remember { mutableStateOf("") }
    ShopDialog(onDismiss) {
        ShopText("管理分类", 18, colors.ink, weight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                text,
                { text = it.take(10) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("新分类") },
            )
            Icon(Icons.Filled.Add, "添加", tint = colors.accent, modifier = Modifier.padding(start = 6.dp).clickable {
                val name = text.trim()
                if (name.isNotBlank()) {
                    scope.launch { app.db.shopping().insertCategories(listOf(ShoppingCategory(name, categories.size))) }
                    text = ""
                }
            })
        }
        categories.forEach { category ->
            val used = items.count { it.category == category.name }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                ShopText("${categoryEmoji(category.name)} ${category.name}", 14, colors.ink, modifier = Modifier.weight(1f))
                ShopText("$used 件", 12, colors.secondary)
                if (!category.builtin && used == 0) {
                    key(category.name) {
                        RecordActionsMenu(
                            "分类", "确定删除「${category.name}」吗？",
                            onDelete = { scope.launch { app.db.shopping().deleteCategory(category) } },
                            tint = colors.secondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PinkButton("完成", Modifier.fillMaxWidth(), onDismiss)
    }
}
