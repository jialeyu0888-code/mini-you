package com.miniyou.app.ui.shopping

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Composable
private fun ledgerAccent() = if (isSystemInDarkTheme()) Color(0xFFABE1CC) else Color(0xFF23584D)

private val chartColors = listOf(
    Color(0xFF438473), Color(0xFFC4776C), Color(0xFFB48A49),
    Color(0xFF8186A8), Color(0xFF6C9EA7), Color(0xFFA67B9D),
)

@Composable
internal fun SpendingOverview(bought: List<ShoppingItem>) {
    val colors = LocalShopColors.current
    val spent = remember(bought) { bought.amounts() }
    val distribution = remember(bought) {
        bought.groupBy { it.ledgerCategory() }
            .map { (category, items) -> category to items.amounts().total }
            .filter { (_, amount) -> amount.signum() > 0 }
            .sortedWith(compareByDescending<Pair<String, BigDecimal>> { it.second }.thenBy { it.first })
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(colors.pink)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ShopText("累计已花费", 14, colors.ink, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                ShopText("已购买 · ${bought.size} 项", 12, colors.ink)
            }
            ShopText(ledgerMoney(spent.total), 34, colors.ink, weight = FontWeight.Bold)
            if (spent.unpriced > 0) {
                ShopText("${spent.unpriced} 项未记录实付金额", 12, colors.ink)
            }
        }
        LedgerCard {
            ShopText("钱花在哪儿", 18, colors.ink, weight = FontWeight.Bold)
            if (distribution.isEmpty()) {
                ShopText("有已购支出后，这里会显示各分类的金额和占比。", 13, colors.secondary)
            } else {
                val shares = distribution.map { (_, amount) ->
                    amount.divide(spent.total, MathContext.DECIMAL64)
                }
                Canvas(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)) {
                    var start = 0f
                    shares.forEachIndexed { index, share ->
                        val end = if (index == shares.lastIndex) size.width else start + share.toFloat() * size.width
                        drawRect(
                            chartColors[index % chartColors.size],
                            Offset(start, 0f),
                            Size((end - start).coerceAtLeast(0f), size.height),
                        )
                        start = end
                    }
                }
                val visible = if (expanded) distribution else distribution.take(3)
                visible.forEachIndexed { index, (category, amount) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(8.dp).background(chartColors[index % chartColors.size], CircleShape))
                        ShopText(category, 13, colors.ink, modifier = Modifier.weight(1f))
                        ShopText(ledgerMoney(amount), 13, colors.ink, weight = FontWeight.SemiBold)
                        ShopText(percentLabel(shares[index]), 12, colors.secondary)
                    }
                }
                if (distribution.size > 3) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "收起分类" else "查看全部 ${distribution.size} 个分类", color = colors.accent)
                    }
                }
            }
        }
    }
}

private fun percentLabel(share: BigDecimal): String {
    val percent = share.movePointRight(2)
    return if (percent < BigDecimal("0.1")) "<0.1%"
    else percent.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%"
}

@Composable
internal fun BudgetPreview(
    spent: ShoppingAmounts,
    decided: List<ShoppingItem>,
    planned: List<ShoppingItem>,
    budget: BigDecimal?,
    onBudgetChanged: (BigDecimal?) -> Unit,
) {
    val colors = LocalShopColors.current
    val pending = remember(decided, planned) { decided + planned }
    var customSelection by rememberSaveable { mutableStateOf<LongArray?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var editingBudget by rememberSaveable { mutableStateOf(false) }
    val chosenIds = remember(customSelection) { customSelection?.toSet() }
    val chosen = if (chosenIds == null) decided else pending.filter { it.id in chosenIds }
    val estimate = chosen.amounts()
    val projected = spent.total + estimate.total
    val unknown = spent.unpriced + estimate.unpriced
    LedgerCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ShopText("预算试算", 18, colors.ink, weight = FontWeight.Bold)
                ShopText(budget?.let { "总预算 ${ledgerMoney(it)}" } ?: "还未设置总预算", 12, colors.secondary)
            }
            TextButton(onClick = { editingBudget = true }) {
                Text(if (budget == null) "设置预算" else "修改预算", color = colors.accent)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(ledgerAccent().copy(alpha = 0.10f)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BudgetMetric(
                "还需支出",
                (if (estimate.total.signum() > 0) "+" else "") + ledgerMoney(estimate.total),
                Modifier.weight(1f),
            )
            BudgetMetric("买后累计", ledgerMoney(projected), Modifier.weight(1f))
        }
        if (budget != null) {
            val remaining = budget - projected
            val label = when {
                remaining.signum() < 0 -> "预计超出 ${ledgerMoney(remaining.abs())}"
                remaining.signum() == 0 -> "预计刚好用完预算"
                unknown > 0 -> "暂余 ${ledgerMoney(remaining)}"
                else -> "预计剩余 ${ledgerMoney(remaining)}"
            }
            ShopText(
                label, 14,
                if (remaining.signum() < 0) {
                    if (isSystemInDarkTheme()) Color(0xFFFFAB9C) else Color(0xFF9D3738)
                } else ledgerAccent(),
                weight = FontWeight.Bold,
            )
        }
        if (unknown > 0) {
            ShopText("${unknown} 项缺少金额，以上按已记录金额估算。", 12, colors.secondary)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val selectionLabel = if (customSelection == null) "仅已决定 · ${chosen.size} 项" else "自选 · ${chosen.size} 项"
            ShopText(selectionLabel, 12, colors.secondary, modifier = Modifier.weight(1f))
            if (pending.isNotEmpty()) {
                TextButton(onClick = { picking = true }) { Text("选择待购商品", color = colors.accent) }
            }
        }
    }
    if (editingBudget) {
        BudgetLimitDialog(budget, onDismiss = { editingBudget = false }) {
            onBudgetChanged(it)
            editingBudget = false
        }
    }
    if (picking) {
        BudgetItemsDialog(
            pending = pending,
            selected = chosen.map { it.id }.toLongArray(),
            useDecided = customSelection == null,
            onDismiss = { picking = false },
        ) {
            customSelection = it
            picking = false
        }
    }
}

@Composable
private fun BudgetMetric(label: String, value: String, modifier: Modifier) {
    val colors = LocalShopColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ShopText(label, 12, colors.secondary)
        ShopText(value, 20, colors.ink, weight = FontWeight.Bold)
    }
}

@Composable
private fun LedgerCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalShopColors.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.card)
            .border(1.dp, colors.line, shape).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun BudgetLimitDialog(
    budget: BigDecimal?,
    onDismiss: () -> Unit,
    onSave: (BigDecimal?) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(budget?.stripTrailingZeros()?.toPlainString().orEmpty()) }
    val amount = parseShoppingBudget(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("购物总预算") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(13) },
                label = { Text("总预算（元）") },
                supportingText = { Text("输入大于 0 的金额，最多两位小数。留空可清除预算。") },
                isError = value.isNotBlank() && amount == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(enabled = value.isBlank() || amount != null, onClick = { onSave(amount) }) {
                Text("保存预算")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun BudgetItemsDialog(
    pending: List<ShoppingItem>,
    selected: LongArray,
    useDecided: Boolean,
    onDismiss: () -> Unit,
    onApply: (LongArray?) -> Unit,
) {
    val colors = LocalShopColors.current
    var draft by rememberSaveable { mutableStateOf(if (useDecided) null else selected) }
    val checkedIds = remember(pending, draft) {
        draft?.toSet() ?: pending.filter { it.status == ShoppingStatus.DECIDED }.map { it.id }.toSet()
    }
    val chosen = pending.filter { it.id in checkedIds }
    val estimate = chosen.amounts()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要试算的商品") },
        text = {
            Column {
                Text("已选 ${chosen.size} 项 · ${ledgerMoney(estimate.total)}" +
                    if (estimate.unpriced > 0) " · ${estimate.unpriced} 项待估价" else "")
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { draft = null }) { Text("仅已决定") }
                    TextButton(onClick = { draft = pending.map { it.id }.toLongArray() }) { Text("全选") }
                    TextButton(onClick = { draft = longArrayOf() }) { Text("清空") }
                }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(pending, key = { it.id }) { item ->
                        val checked = item.id in checkedIds
                        Row(
                            Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) { include ->
                                draft = (if (include) checkedIds + item.id else checkedIds - item.id).toLongArray()
                            }.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked, onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = colors.button, checkmarkColor = colors.onButton),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(item.name)
                                ShopText(
                                    if (item.status == ShoppingStatus.DECIDED) "已决定未买" else "计划购买",
                                    12, colors.secondary,
                                )
                            }
                            Text(item.recordedAmount?.let(::ledgerMoney) ?: "待估价")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(draft) }) { Text("应用试算") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
