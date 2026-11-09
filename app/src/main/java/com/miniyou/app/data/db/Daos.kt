package com.miniyou.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckupDao {
    @Query("SELECT * FROM checkups ORDER BY date DESC, id DESC")
    fun all(): Flow<List<Checkup>>

    @Query("SELECT * FROM checkups ORDER BY date DESC, id DESC")
    suspend fun allOnce(): List<Checkup>

    @Query("SELECT TRIM(hospital) FROM checkups WHERE TRIM(hospital) != '' GROUP BY TRIM(hospital) ORDER BY MAX(date) DESC, MAX(id) DESC")
    fun hospitals(): Flow<List<String>>

    @Query("SELECT * FROM checkups WHERE id = :id")
    fun observe(id: Long): Flow<Checkup?>

    @Query("SELECT * FROM checkups WHERE id = :id")
    suspend fun get(id: Long): Checkup?

    @Upsert
    suspend fun upsert(item: Checkup): Long

    @Query("UPDATE checkups SET nextCheckupDate = :date, nextCheckupNote = :note WHERE id = :id")
    suspend fun updateFollowUp(id: Long, date: Long?, note: String)

    @Delete
    suspend fun delete(item: Checkup)
}

@Dao
interface CheckupPlanDao {
    @Query("SELECT * FROM checkup_plans ORDER BY date, id")
    fun all(): Flow<List<CheckupPlan>>

    @Query("SELECT * FROM checkup_plans WHERE date >= :today ORDER BY date, id")
    suspend fun upcoming(today: Long): List<CheckupPlan>

    @Query("SELECT * FROM checkup_plans WHERE id = :id")
    suspend fun get(id: Long): CheckupPlan?

    @Query("SELECT * FROM checkup_plans WHERE sourceCheckupId = :id")
    suspend fun forCheckup(id: Long): CheckupPlan?

    @Upsert
    suspend fun upsert(plan: CheckupPlan): Long

    @Delete
    suspend fun delete(plan: CheckupPlan)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary ORDER BY date DESC, createdAt DESC")
    fun all(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary WHERE date = :date ORDER BY createdAt DESC, id DESC")
    suspend fun byDate(date: Long): List<DiaryEntry>

    @Query("SELECT * FROM diary WHERE id = :id")
    suspend fun get(id: Long): DiaryEntry?

    @Upsert
    suspend fun upsert(item: DiaryEntry): Long

    @Delete
    suspend fun delete(item: DiaryEntry)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping ORDER BY updatedAt DESC")
    fun all(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping")
    suspend fun allOnce(): List<ShoppingItem>

    @Query("SELECT * FROM shopping WHERE id = :id")
    fun observe(id: Long): Flow<ShoppingItem?>

    @Query("SELECT * FROM shopping WHERE id = :id")
    suspend fun get(id: Long): ShoppingItem?

    @Upsert
    suspend fun upsert(item: ShoppingItem): Long

    @Insert
    suspend fun insertAll(items: List<ShoppingItem>)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("SELECT * FROM shopping_categories ORDER BY sortOrder, name")
    fun categories(): Flow<List<ShoppingCategory>>

    @Query("SELECT * FROM shopping_categories ORDER BY sortOrder, name")
    suspend fun categoriesOnce(): List<ShoppingCategory>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(items: List<ShoppingCategory>)

    @Delete
    suspend fun deleteCategory(item: ShoppingCategory)
}

@Dao
interface BellyDao {
    @Query("SELECT * FROM belly_photos ORDER BY date DESC, id DESC")
    fun all(): Flow<List<BellyPhoto>>

    @Query("SELECT * FROM belly_photos WHERE id = :id")
    suspend fun get(id: Long): BellyPhoto?

    @Upsert
    suspend fun upsert(item: BellyPhoto): Long

    @Delete
    suspend fun delete(item: BellyPhoto)
}

@Dao
interface MyRemedyDao {
    @Query("SELECT * FROM my_remedies ORDER BY updatedAt DESC")
    fun all(): Flow<List<MyRemedy>>

    @Query("SELECT * FROM my_remedies WHERE id = :id")
    suspend fun get(id: Long): MyRemedy?

    @Upsert
    suspend fun upsert(item: MyRemedy): Long

    @Delete
    suspend fun delete(item: MyRemedy)

    @Query("SELECT * FROM favorite_methods ORDER BY createdAt DESC")
    fun favorites(): Flow<List<FavoriteMethod>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteMethod)

    @Query("DELETE FROM favorite_methods WHERE methodId = :methodId")
    suspend fun removeFavorite(methodId: String)
}

@Dao
interface CheckinDao {
    @Query("SELECT * FROM checkin_items ORDER BY sortOrder, id")
    fun items(): Flow<List<CheckinItem>>

    @Query("SELECT * FROM checkin_items ORDER BY sortOrder, id")
    suspend fun itemsOnce(): List<CheckinItem>

    @Query("SELECT * FROM checkin_items WHERE id = :id")
    suspend fun item(id: Long): CheckinItem?

    @Upsert
    suspend fun upsertItem(item: CheckinItem): Long

    @Insert
    suspend fun insertItems(items: List<CheckinItem>)

    @Delete
    suspend fun deleteItem(item: CheckinItem)

    @Query("DELETE FROM checkin_records WHERE itemId = :itemId")
    suspend fun deleteRecordsOf(itemId: Long)

    @Transaction
    suspend fun deleteWithRecords(item: CheckinItem) {
        deleteRecordsOf(item.id)
        deleteItem(item)
    }

    @Query("SELECT * FROM checkin_records")
    fun records(): Flow<List<CheckinRecord>>

    @Query("SELECT COUNT(*) FROM checkin_records WHERE itemId = :itemId AND date = :date")
    suspend fun count(itemId: Long, date: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CheckinRecord)

    @Query("DELETE FROM checkin_records WHERE itemId = :itemId AND date = :date")
    suspend fun deleteRecord(itemId: Long, date: Long)
}
