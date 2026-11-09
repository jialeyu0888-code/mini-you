package com.miniyou.app.ui.shopping

import com.miniyou.app.data.db.ShoppingItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal val ShoppingItem.recordedAmount: BigDecimal?
    get() = price?.takeIf { it.isFinite() && it >= 0 }?.toBigDecimal()
        ?.multiply(quantity.toBigDecimal())?.setScale(2, RoundingMode.HALF_UP)

internal data class ShoppingAmounts(val total: BigDecimal, val unpriced: Int)

internal fun List<ShoppingItem>.amounts(): ShoppingAmounts {
    val prices = mapNotNull { it.recordedAmount }
    return ShoppingAmounts(prices.fold(BigDecimal.ZERO, BigDecimal::add), size - prices.size)
}

internal fun ShoppingItem.ledgerCategory() = category.trim().ifBlank { "其他" }

internal fun ledgerMoney(amount: BigDecimal): String =
    "¥" + DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.CHINA)).format(amount)

internal fun parseShoppingBudget(text: String): BigDecimal? =
    text.trim().takeIf { it.matches(Regex("[0-9]{1,10}(\\.[0-9]{1,2})?")) }
        ?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
