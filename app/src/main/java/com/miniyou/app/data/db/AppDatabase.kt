package com.miniyou.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `belly_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `week` TEXT NOT NULL, `photo` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_belly_photos_date` ON `belly_photos` (`date`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `my_remedies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `symptom` TEXT NOT NULL, `remedyId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `rating` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_my_remedies_remedyId` ON `my_remedies` (`remedyId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_methods` (`methodId` TEXT NOT NULL, `remedyId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`methodId`))")
    }
}

@Database(
    entities = [
        Checkup::class, DiaryEntry::class, ShoppingItem::class, ShoppingCategory::class,
        CheckinItem::class, CheckinRecord::class, BellyPhoto::class, MyRemedy::class, FavoriteMethod::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkups(): CheckupDao
    abstract fun diary(): DiaryDao
    abstract fun shopping(): ShoppingDao
    abstract fun checkin(): CheckinDao
    abstract fun belly(): BellyDao
    abstract fun myRemedies(): MyRemedyDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "miniyou.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
