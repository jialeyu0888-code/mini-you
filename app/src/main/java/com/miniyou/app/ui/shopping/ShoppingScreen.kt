package com.miniyou.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.RecordActionsMenu
import com.miniyou.app.ui.components.app
import kotlinx.coroutines.launch

private const val FAVORITES = -1

@Composable
fun ShoppingScreen(nav: NavController, focus: String = "") {
    ShopTheme {
        val colors = LocalShopColors.current
        val app = app()
        val items by app.db.shopping().all().collectAsState(initial = emptyList())
        val settings by app.settings.state.collectAsState()
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
                                ShopText("购物账本", 26, colors.ink, weight = FontWeight.Bold)
                                ShopText("消费与预算", 12, colors.secondary)
                            }
                            Button(
                                onClick = { nav.navigate(Routes.shoppingEdit(status = status.coerceAtLeast(0))) },
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
                    item("spending") { SpendingOverview(bought) }
                    item("budget") {
                        BudgetPreview(
                            spent = bought.amounts(),
                            decided = decided,
                            planned = planned,
                            budget = settings.shoppingBudget?.toBigDecimalOrNull(),
                            onBudgetChanged = { amount ->
                                app.settings.update { it.copy(shoppingBudget = amount?.toPlainString()) }
                            },
                        )
                    }
                    item("items-header") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShopText(
                                if (status == FAVORITES) "我的收藏" else "商品清单",
                                20, colors.ink, weight = FontWeight.Bold,
                            )
                            StatusTabs(status, bought.size, decided.size, planned.size) { status = it }
                            val amounts = shown.amounts()
                            if (shown.isNotEmpty() && status != FAVORITES) {
                                val label = if (status == ShoppingStatus.BOUGHT) "已记录实付" else "已估价小计"
                                ShopText(
                                    "$label ${ledgerMoney(amounts.total)}" +
                                        if (amounts.unpriced == 0) "" else "  ·  ${amounts.unpriced} 项缺少金额",
                                    12, colors.secondary,
                                )
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
                                    else -> "在商品详情里点爱心，即可加入收藏。"
                                },
                                13, colors.secondary, modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                    items(shown, key = { it.id }) { item ->
                        LedgerItemRow(item, nav)
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
    onSelect: (Int) -> Unit,
) {
    val colors = LocalShopColors.current
    val options = listOf(
        Triple(ShoppingStatus.BOUGHT, "已购买", bought),
        Triple(ShoppingStatus.DECIDED, "已决定未买", decided),
        Triple(ShoppingStatus.PLANNED, "计划购买", planned),
    )
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
private fun LedgerItemRow(item: ShoppingItem, nav: NavController) {
    val colors = LocalShopColors.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.card)
            .border(1.dp, colors.line, shape).clickable { nav.navigate(Routes.shoppingDetail(item.id)) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShopThumb(item, Modifier.size(42.dp))
        Column(Modifier.weight(1f)) {
            ShopText(item.name, 14, colors.ink, weight = FontWeight.Bold, maxLines = 2)
            ShopText("${item.ledgerCategory()}  ·  × ${item.quantity}", 11, colors.secondary, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            ShopText(
                item.recordedAmount?.let(::ledgerMoney) ?: if (item.status == ShoppingStatus.BOUGHT) "未记金额" else "待估价",
                14, colors.ink, weight = FontWeight.SemiBold,
            )
            if (item.recordedAmount != null) {
                ShopText(if (item.status == ShoppingStatus.BOUGHT) "实付" else "预计", 11, colors.secondary)
            }
        }
        ShoppingItemActions(item, nav, Modifier.align(Alignment.Top))
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
