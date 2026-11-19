package com.miniyou.app.data

import android.content.Context
import androidx.room.withTransaction
import com.miniyou.app.data.db.AppDatabase
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.db.CheckupPlan
import com.miniyou.app.notify.Reminders
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CheckupPlans(private val context: Context, private val db: AppDatabase) {
    private val plans = db.checkupPlans()
    private val checkups = db.checkups()
    private val lock = Mutex()

    val all = plans.all()

    suspend fun get(id: Long): CheckupPlan? = plans.get(id)

    suspend fun save(plan: CheckupPlan) = update {
        val saved = db.withTransaction {
            val current = if (plan.id > 0) checkNotNull(plans.get(plan.id)) else null
            val updated = plan.copy(sourceCheckupId = current?.sourceCheckupId)
            updated.sourceCheckupId?.let { checkups.updateFollowUp(it, updated.date, updated.note) }
            persist(updated)
        }
        Reminders.scheduleCheckupPlan(context, saved)
    }

    suspend fun delete(id: Long) = deletePlans(setOf(id))

    suspend fun deletePlans(ids: Set<Long>) = update {
        db.withTransaction {
            ids.forEach { id ->
                plans.get(id)?.let { plan ->
                    plan.sourceCheckupId?.let { checkups.updateFollowUp(it, null, "") }
                    plans.delete(plan)
                }
            }
        }
        ids.forEach { Reminders.cancelCheckupPlan(context, it) }
    }
    suspend fun saveCheckup(checkup: Checkup) = update {
        var removedPlanId: Long? = null
        val savedPlan = db.withTransaction {
            val insertedId = checkups.upsert(checkup)
            val checkupId = if (checkup.id > 0) checkup.id else insertedId
            val current = plans.forCheckup(checkupId)
            val date = checkup.nextCheckupDate
            if (date == null) {
                if (current != null) {
                    plans.delete(current)
                    removedPlanId = current.id
                }
                null
            } else {
                val plan = current ?: CheckupPlan(
                    date = date, hospital = checkup.hospital, sourceCheckupId = checkupId,
                )
                persist(plan.copy(date = date, note = checkup.nextCheckupNote))
            }
        }
        removedPlanId?.let { Reminders.cancelCheckupPlan(context, it) }
        savedPlan?.let { Reminders.scheduleCheckupPlan(context, it) }
    }

    suspend fun deleteCheckup(checkup: Checkup) = deleteCheckups(listOf(checkup))

    suspend fun deleteCheckups(records: List<Checkup>) = update {
        val planIds = db.withTransaction {
            records.mapNotNull { checkup ->
                val plan = plans.forCheckup(checkup.id)
                if (plan != null) plans.delete(plan)
                checkups.delete(checkup)
                plan?.id
            }
        }
        planIds.forEach { Reminders.cancelCheckupPlan(context, it) }
        records.flatMap { it.photos }.distinct().forEach(ImageStore::delete)
    }

    suspend fun reschedule(): Unit = lock.withLock {
        plans.upcoming(LocalDate.now().toEpochDay()).forEach { Reminders.scheduleCheckupPlan(context, it) }
    }

    private suspend fun update(action: suspend () -> Unit): Unit = lock.withLock {
        // Keep the reminder in sync if the screen closes while a database change is committing.
        withContext(NonCancellable) { action() }
    }

    private suspend fun persist(plan: CheckupPlan): CheckupPlan {
        val insertedId = plans.upsert(plan)
        return if (plan.id > 0) plan else plan.copy(id = insertedId)
    }
}
