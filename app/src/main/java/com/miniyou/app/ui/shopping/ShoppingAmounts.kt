package com.miniyou.app.ui.shopping

import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.net.URI

internal val ShoppingItem.recordedAmount: BigDecimal?
    get() = price?.takeIf { it.isFinite() && it >= 0 }?.toBigDecimal()
        ?.multiply(quantity.toBigDecimal())?.setScale(2, RoundingMode.HALF_UP)

internal data class SpendingImpact(val change: BigDecimal, val projected: BigDecimal, val planned: Boolean)

internal fun spendingImpact(
    current: BigDecimal,
    previous: BigDecimal,
    status: Int,
    unitPrice: String,
    quantity: Int,
): SpendingImpact? {
    if (status == ShoppingStatus.CLOSED) return SpendingImpact(-previous, current - previous, false)
    val line = unitPrice.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
        ?.multiply(quantity.toBigDecimal())?.setScale(2, RoundingMode.HALF_UP) ?: return null
    val planned = status != ShoppingStatus.BOUGHT
    return SpendingImpact(if (planned) line else line - previous, current - previous + line, planned)
}

internal fun normalizedProductUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    val candidate = if ("://" in trimmed) trimmed else "https://$trimmed"
    return runCatching { URI(candidate) }.getOrNull()
        ?.takeIf { it.scheme.equals("https", true) || it.scheme.equals("http", true) }
        ?.takeIf { it.host?.contains('.') == true && it.userInfo == null }?.toASCIIString()
}

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
