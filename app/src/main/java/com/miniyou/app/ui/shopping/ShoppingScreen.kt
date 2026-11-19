package com.miniyou.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.RecordSelection
import com.miniyou.app.ui.components.RecordSelectionBar
import com.miniyou.app.ui.components.RecordSelectionMark
import com.miniyou.app.ui.components.rememberRecordSelection
import com.miniyou.app.ui.components.app
import kotlinx.coroutines.launch

private const val FAVORITES = -1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingScreen(nav: NavController, focus: String = "") {
    ShopTheme {
        val colors = LocalShopColors.current
        val app = app()
        val items by app.db.shopping().all().collectAsState(initial = emptyList())
        val selection = rememberRecordSelection()
        val favorites by rememberShoppingMarks().favorites.collectAsState()
        var status by rememberSaveable(focus) {
            mutableIntStateOf(when (focus) {
                "fav" -> FAVORITES
                "planned" -> ShoppingStatus.PLANNED
                "decided" -> ShoppingStatus.DECIDED
                else -> ShoppingStatus.BOUGHT
            })
        }
        var menuOpen by remember { mutableStateOf(false) }
        var manageCategories by rememberSaveable { mutableStateOf(false) }
        val categories by app.db.shopping().categories().collectAsState(initial = emptyList())
        val byStatus = remember(items) { items.groupBy { it.status } }
        val bought = byStatus[ShoppingStatus.BOUGHT].orEmpty()
        val decided = byStatus[ShoppingStatus.DECIDED].orEmpty()
        val planned = byStatus[ShoppingStatus.PLANNED].orEmpty()
        val closed = byStatus[ShoppingStatus.CLOSED].orEmpty()
        val shown = when (status) {
            FAVORITES -> items.filter { it.id in favorites }
            else -> byStatus[status].orEmpty()
        }

        AppScaffold(containerColor = colors.paper) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = 660.dp).fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item("header") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                ShopText("商品清单", 26, colors.ink, weight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val initialStatus = if (status == ShoppingStatus.CLOSED) ShoppingStatus.PLANNED else status.coerceAtLeast(0)
                                    nav.navigate(Routes.shoppingEdit(status = initialStatus))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.button, contentColor = colors.onButton),
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Text("添加商品")
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Outlined.MoreHoriz, "更多购物操作", tint = colors.ink)
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("AI 识图导入") },
                                        onClick = {
                                            menuOpen = false
                                            nav.navigate(Routes.shoppingEdit(ai = true))
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("我的收藏") },
                                        onClick = {
                                            menuOpen = false
                                            status = FAVORITES
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("管理分类") },
                                        onClick = {
                                            menuOpen = false
                                            manageCategories = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                    stickyHeader("items-header") {
                        Column(Modifier.fillMaxWidth().background(colors.paper)) {
                            StatusTabs(status, bought.size, decided.size, planned.size, closed.size) {
                                if (!selection.busy) {
                                    selection.stop()
                                    status = it
                                }
                            }
                            if (status == FAVORITES) ShopText("我的收藏", 16, colors.ink, weight = FontWeight.Bold)
                            if (selection.active) {
                                ShoppingSelectionBar(selection, shown)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { nav.navigate(Routes.SHOPPING_ANALYSIS) }) { Text("消费分析") }
                                    Box(Modifier.weight(1f)) { ShoppingSelectionBar(selection, shown) }
                                }
                            }
                        }
                    }
                    if (shown.isEmpty()) {
                        item("empty-items") {
                            ShopText(
                                when (status) {
                                    ShoppingStatus.BOUGHT -> "还没有已购记录。添加商品即可记下实付金额。"
                                    ShoppingStatus.DECIDED -> "确定要买的商品可以放在这里，方便估算预算。"
                                    ShoppingStatus.PLANNED -> "想买的商品可以先记在这里，之后再决定。"
                                    ShoppingStatus.CLOSED -> "暂无关闭的订单。"
                                    else -> "在商品详情里点爱心，即可加入收藏。"
                                },
                                13, colors.secondary, modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                    items(shown, key = { it.id }) { item ->
                        LedgerItemRow(item, nav, selection)
                    }
                    item("bottom-space") { Box(Modifier.padding(bottom = 8.dp)) }
                }
            }
        }
        if (manageCategories) CategoryManagerDialog(categories, items) { manageCategories = false }
    }
}

@Composable
private fun StatusTabs(
    selected: Int,
    bought: Int,
    decided: Int,
    planned: Int,
    closed: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalShopColors.current
    val options = buildList {
        add(Triple(ShoppingStatus.BOUGHT, "已购买", bought))
        add(Triple(ShoppingStatus.DECIDED, "已决定未买", decided))
        add(Triple(ShoppingStatus.PLANNED, "计划购买", planned))
        if (closed > 0 || selected == ShoppingStatus.CLOSED) add(Triple(ShoppingStatus.CLOSED, "交易关闭", closed))
    }
    Row(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (status, name, count) ->
            val active = selected == status
            val shape = RoundedCornerShape(14.dp)
            val ink = if (active) colors.accent else colors.ink
            Column(
                Modifier.weight(1f).clip(shape)
                    .background(if (active) colors.pink else colors.card)
                    .border(1.dp, if (active) colors.accent.copy(alpha = 0.35f) else colors.line, shape)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(status) })
                    .padding(horizontal = 3.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShopText(name, 12, ink, weight = FontWeight.SemiBold, center = true)
                ShopText("$count 项", 11, if (active) ink else colors.secondary)
            }
        }
    }
}

@Composable
internal fun ShoppingSelectionBar(selection: RecordSelection, items: List<ShoppingItem>) {
    val dao = app().db.shopping()
    RecordSelectionBar(selection, items.map { it.id }, "商品") { ids ->
        dao.deleteMany(items.filter { it.id in ids })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerItemRow(item: ShoppingItem, nav: NavController, selection: RecordSelection) {
    val colors = LocalShopColors.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.card)
            .border(1.dp, colors.line, shape).combinedClickable(
                onClick = {
                    if (selection.active) selection.toggle(item.id) else nav.navigate(Routes.shoppingDetail(item.id))
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    selection.beginWith(item.id)
                },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShopThumb(item, Modifier.size(52.dp))
        Column(Modifier.weight(1f)) {
            ShopText(item.name, 14, colors.ink, weight = FontWeight.Bold, maxLines = 2)
            ShopText(listOfNotNull(item.brand.takeIf { it.isNotBlank() }, item.ledgerCategory(), "× ${item.quantity}")
                .joinToString("  ·  "), 11, colors.secondary, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            ShopText(
                item.recordedAmount?.let(::ledgerMoney) ?: if (item.status == ShoppingStatus.BOUGHT) "未记金额" else "待估价",
                14, colors.ink, weight = FontWeight.SemiBold,
            )
            if (item.recordedAmount != null) {
                val amountLabel = when (item.status) {
                    ShoppingStatus.BOUGHT -> "实付"
                    ShoppingStatus.CLOSED -> "商品金额"
                    else -> "预计"
                }
                ShopText(amountLabel, 11, colors.secondary)
            }
        }
        if (selection.active) RecordSelectionMark(selection, item.id, "商品")
        else ShoppingItemActions(item, nav, Modifier.align(Alignment.Top))
    }
}

@Composable
internal fun ShoppingItemActions(item: ShoppingItem, nav: NavController, modifier: Modifier = Modifier) {
    val app = app()
    val scope = rememberCoroutineScope()
    RecordActionsMenu(
        "商品", "确定删除「${item.name}」吗？",
        onEdit = { nav.navigate(Routes.shoppingEdit(item.id)) },
        onDelete = { scope.launch { app.db.shopping().delete(item) } },
        modifier = modifier,
        tint = LocalShopColors.current.secondary,
    )
}
