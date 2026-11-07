package com.miniyou.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.miniyou.app.MainActivity
import com.miniyou.app.MiniYouApp
import com.miniyou.app.R
import com.miniyou.app.data.db.CheckinItem
import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.shortCn
import com.miniyou.app.data.toDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object Reminders {
    private const val CHANNEL_CHECKUP = "checkup"
    private const val CHANNEL_CHECKIN = "checkin"
    private const val TYPE_CHECKUP = "checkup"
    private const val TYPE_CHECKIN = "checkin"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_CHECKUP, "产检提醒", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_CHECKIN, "每日打卡提醒", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pending(context: Context, requestCode: Int, intent: Intent?): PendingIntent? {
        val base = Intent(context, ReminderReceiver::class.java)
        return if (intent == null) {
            PendingIntent.getBroadcast(context, requestCode, base, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun setAlarm(context: Context, at: LocalDateTime, requestCode: Int, intent: Intent) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis <= System.currentTimeMillis()) return
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, requestCode, intent) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun cancel(context: Context, requestCode: Int) {
        val pi = pending(context, requestCode, null) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }

    private fun checkupCode(id: Long, slot: Int) = (100_000 + id * 2 + slot).toInt()
    private fun checkinCode(id: Long) = (500_000 + id).toInt()

    /** 产检前一天 20:00 和当天 8:00 各提醒一次。 */
    fun scheduleCheckup(context: Context, checkup: Checkup) {
        cancelCheckup(context, checkup.id)
        val next = checkup.nextCheckupDate?.toDate() ?: return
        val note = checkup.nextCheckupNote.ifBlank { "记得带上产检本和医保卡哦" }
        listOf(
            next.minusDays(1).atTime(20, 0) to "明天（${next.shortCn()}）要去产检啦",
            next.atTime(8, 0) to "今天要去产检啦",
        ).forEachIndexed { slot, (at, title) ->
            val intent = Intent(context, ReminderReceiver::class.java)
                .putExtra("type", TYPE_CHECKUP).putExtra("id", checkup.id)
                .putExtra("title", title).putExtra("text", note)
            setAlarm(context, at, checkupCode(checkup.id, slot), intent)
        }
    }

    fun cancelCheckup(context: Context, id: Long) {
        cancel(context, checkupCode(id, 0)); cancel(context, checkupCode(id, 1))
    }

    fun scheduleCheckin(context: Context, item: CheckinItem) {
        cancelCheckin(context, item.id)
        if (!item.reminderEnabled) return
        val time = LocalTime.of(item.reminderMinutes / 60, item.reminderMinutes % 60)
        var at = LocalDate.now().atTime(time)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("type", TYPE_CHECKIN).putExtra("id", item.id)
            .putExtra("title", "${item.emoji} 该吃${item.name}啦")
            .putExtra("text", item.note.ifBlank { "完成后记得在「小小你」里打卡哦" })
        setAlarm(context, at, checkinCode(item.id), intent)
    }

    fun cancelCheckin(context: Context, id: Long) = cancel(context, checkinCode(id))

    suspend fun rescheduleAll(context: Context) {
        val db = (context.applicationContext as MiniYouApp).db
        val today = LocalDate.now().toEpochDay()
        db.checkups().allOnce().filter { (it.nextCheckupDate ?: -1) >= today }.forEach { scheduleCheckup(context, it) }
        db.checkin().itemsOnce().forEach { scheduleCheckin(context, it) }
    }

    internal fun show(context: Context, type: String, id: Long, title: String, text: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, if (type == TYPE_CHECKUP) CHANNEL_CHECKUP else CHANNEL_CHECKIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE8798F.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify((if (type == TYPE_CHECKUP) 1_000_000 else 2_000_000) + id.toInt(), n)
    }

    internal suspend fun onFire(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val id = intent.getLongExtra("id", -1)
        val title = intent.getStringExtra("title").orEmpty()
        val text = intent.getStringExtra("text").orEmpty()
        if (type == TYPE_CHECKIN) {
            val dao = (context.applicationContext as MiniYouApp).db.checkin()
            val item = dao.item(id) ?: return
            if (dao.count(id, LocalDate.now().toEpochDay()) == 0) show(context, type, id, title, text)
            scheduleCheckin(context, item)
        } else {
            show(context, type, id, title, text)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { Reminders.onFire(context, intent) } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { Reminders.rescheduleAll(context) } finally { pending.finish() }
        }
    }
}
