package com.miniyou.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 产检单上的单项指标（血常规、尿常规、B超数据等）。 */
data class Indicator(
    val category: String = "其他",
    val name: String = "",
    val value: String = "",
    val unit: String = "",
    val reference: String = "",
    /** "偏高" / "偏低" / "异常" / "" */
    val flag: String = "",
) {
    val abnormal: Boolean get() = flag.isNotBlank()
}

@Entity(tableName = "checkups")
data class Checkup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** LocalDate.toEpochDay() */
    val date: Long,
    val hospital: String = "",
    /** 例如 "12+3" */
    val gestationalWeek: String = "",
    val checkupType: String = "",
    val weightKg: String = "",
    val bpSystolic: String = "",
    val bpDiastolic: String = "",
    val fetalHeartRate: String = "",
    val indicators: List<Indicator> = emptyList(),
    val doctorAdvice: String = "",
    val nextCheckupDate: Long? = null,
    val nextCheckupNote: String = "",
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "diary", indices = [Index("date")])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val mood: String = "",
    val tags: List<String> = emptyList(),
    val content: String = "",
    val photos: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

object ShoppingStatus {
    const val PLANNED = 0
    const val DECIDED = 1
    const val BOUGHT = 2
    val labels = listOf("计划购买", "决定未买", "已购买")
}

@Entity(tableName = "shopping")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val status: Int = ShoppingStatus.PLANNED,
    val price: Double? = null,
    val quantity: Int = 1,
    val channel: String = "",
    val note: String = "",
    val photo: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val total: Double get() = (price ?: 0.0) * quantity
}

@Entity(tableName = "shopping_categories")
data class ShoppingCategory(
    @PrimaryKey val name: String,
    val sortOrder: Int = 0,
    val builtin: Boolean = false,
)

@Entity(tableName = "checkin_items")
data class CheckinItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "💊",
    val note: String = "",
    val reminderEnabled: Boolean = false,
    /** 一天中的分钟数，例如 9:00 = 540 */
    val reminderMinutes: Int = 9 * 60,
    val sortOrder: Int = 0,
)

/** 孕肚纪念照。 */
@Entity(tableName = "belly_photos", indices = [Index("date")])
data class BellyPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    /** 例如 "20+3" */
    val week: String = "",
    val photo: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

object MyRemedyRating {
    val labels = listOf("有点用", "有效", "非常有效")
}

/** 孕妈自己记录的有效缓解方法。remedyId 对应内置症状 id，自定义症状时为空。 */
@Entity(tableName = "my_remedies", indices = [Index("remedyId")])
data class MyRemedy(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symptom: String,
    val remedyId: String = "",
    val title: String,
    val content: String = "",
    /** 0..2，见 [MyRemedyRating] */
    val rating: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** 内置攻略中被标记为「对我有效」的方法。 */
@Entity(tableName = "favorite_methods")
data class FavoriteMethod(
    @PrimaryKey val methodId: String,
    val remedyId: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "checkin_records", primaryKeys = ["itemId", "date"], indices = [Index("date")])
data class CheckinRecord(
    val itemId: Long,
    val date: Long,
    val time: Long = System.currentTimeMillis(),
)
