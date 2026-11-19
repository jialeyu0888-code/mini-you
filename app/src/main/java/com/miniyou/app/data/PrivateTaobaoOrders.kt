package com.miniyou.app.data

import android.content.Context
import android.util.AtomicFile
import androidx.room.withTransaction
import com.miniyou.app.data.db.AppDatabase
import com.miniyou.app.data.db.ShoppingImportBatch
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.data.db.ShoppingOrderDetails
import com.miniyou.app.data.db.ShoppingStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal object PrivateTaobaoOrders {
    private const val batchId = "taobao-pregnancy-2026-v1"
    private const val assetDirectory = "private-taobao"

    suspend fun importOnce(context: Context, database: AppDatabase) {
        val dao = database.shopping()
        if (dao.hasImportBatch(batchId)) return
        val records = context.assets.open("$assetDirectory/orders.json").bufferedReader().use {
            JSONArray(it.readText())
        }
        val photos = mutableMapOf<String, String>()
        database.withTransaction {
            if (dao.hasImportBatch(batchId)) return@withTransaction
            for (index in 0 until records.length()) {
                val row = records.getJSONObject(index)
                val date = row.getLong("purchasedAt")
                val quantity = row.getInt("quantity")
                val state = row.getString("orderState")
                val photo = if (row.isNull("photo")) null else {
                    val filename = row.getString("photo")
                    photos.getOrPut(filename) { copyPhoto(context, filename) }
                }
                val sku = row.getString("sku")
                val id = dao.upsert(
                    ShoppingItem(
                        name = row.getString("name"),
                        category = row.getString("category"),
                        status = if (state == "交易成功") ShoppingStatus.BOUGHT else ShoppingStatus.CLOSED,
                        price = row.optionalLong("ledgerCents")?.let { it / (100.0 * quantity) },
                        quantity = quantity,
                        channel = "淘宝",
                        note = "",
                        photo = photo,
                        createdAt = date,
                        updatedAt = date,
                        brand = row.getString("brand"),
                        productUrl = row.getString("url"),
                    )
                )
                dao.insertOrderDetails(
                    ShoppingOrderDetails(
                        shoppingId = id,
                        orderNumber = row.getString("orderNumber"),
                        orderLine = row.getInt("line"),
                        productId = row.getString("productId"),
                        purchasedAt = date,
                        orderState = state,
                        shop = row.getString("shop"),
                        brand = row.getString("brand"),
                        sku = sku,
                        productUrl = row.getString("url"),
                        pregnancyProduct = row.getBoolean("pregnancyProduct"),
                        itemAmountCents = row.optionalLong("itemAmountCents"),
                        orderPaidCents = row.optionalLong("orderPaidCents"),
                        orderShippingCents = row.optionalLong("orderShippingCents"),
                    )
                )
            }
            dao.recordImportBatch(ShoppingImportBatch(batchId))
        }
    }

    private fun JSONObject.optionalLong(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun copyPhoto(context: Context, filename: String): String {
        require(filename.matches(Regex("[0-9]+\\.jpg")))
        val directory = File(context.filesDir, assetDirectory).apply { mkdirs() }
        val destination = File(directory, filename)
        val storage = AtomicFile(destination)
        val output = storage.startWrite()
        try {
            context.assets.open("$assetDirectory/photos/$filename").use { it.copyTo(output) }
            storage.finishWrite(output)
        } catch (failure: Exception) {
            storage.failWrite(output)
            throw failure
        }
        return destination.absolutePath
    }
}
