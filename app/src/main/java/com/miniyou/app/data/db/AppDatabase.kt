package com.miniyou.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun stringsToJson(list: List<String>): String = JSONArray(list).toString()

    @TypeConverter
    fun jsonToStrings(json: String): List<String> {
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }
    }

    @TypeConverter
    fun indicatorsToJson(list: List<Indicator>): String = JSONArray().apply {
        list.forEach {
            put(
                JSONObject()
                    .put("category", it.category).put("name", it.name).put("value", it.value)
                    .put("unit", it.unit).put("reference", it.reference).put("flag", it.flag)
            )
        }
    }.toString()

    @TypeConverter
    fun jsonToIndicators(json: String): List<Indicator> {
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let {
                Indicator(
                    category = it.optString("category"),
                    name = it.optString("name"),
                    value = it.optString("value"),
                    unit = it.optString("unit"),
                    reference = it.optString("reference"),
                    flag = it.optString("flag"),
                )
            }
        }
    }
}

@Database(
    entities = [
        Checkup::class, DiaryEntry::class, ShoppingItem::class, ShoppingCategory::class,
        CheckinItem::class, CheckinRecord::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkups(): CheckupDao
    abstract fun diary(): DiaryDao
    abstract fun shopping(): ShoppingDao
    abstract fun checkin(): CheckinDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "miniyou.db").build()
    }
}
