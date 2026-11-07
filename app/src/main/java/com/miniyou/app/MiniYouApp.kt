package com.miniyou.app

import android.app.Application
import com.miniyou.app.ai.AiService
import com.miniyou.app.data.SettingsStore
import com.miniyou.app.data.content.ContentRepository
import com.miniyou.app.data.db.AppDatabase
import com.miniyou.app.data.db.CheckinItem
import com.miniyou.app.data.db.ShoppingCategory
import com.miniyou.app.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val DefaultShoppingCategories = listOf("待产包", "新生儿服饰", "宝宝生活用品", "喂养用品", "孕妇用品", "洗护用品", "出行用品", "玩具早教", "其他")

class MiniYouApp : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var content: ContentRepository
        private set
    lateinit var ai: AiService
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.create(this)
        settings = SettingsStore(this)
        content = ContentRepository(this)
        ai = AiService(this, settings)
        Reminders.createChannels(this)
        appScope.launch {
            seedDefaults()
            Reminders.rescheduleAll(this@MiniYouApp)
        }
    }

    private suspend fun seedDefaults() {
        if (settings.seeded) return
        db.shopping().insertCategories(DefaultShoppingCategories.mapIndexed { i, n -> ShoppingCategory(n, i, builtin = true) })
        db.checkin().insertItems(
            listOf(
                CheckinItem(name = "叶酸", emoji = "🌿", note = "每日 0.4mg，建议孕前 3 个月至孕早期坚持", reminderMinutes = 8 * 60 + 30, sortOrder = 0),
                CheckinItem(name = "钙片", emoji = "🥛", note = "孕中晚期每日 1000–1200mg，与铁剂错开服用", reminderMinutes = 20 * 60, sortOrder = 1),
                CheckinItem(name = "DHA", emoji = "🐟", note = "每日 200mg 以上，随餐服用吸收更好", reminderMinutes = 12 * 60 + 30, sortOrder = 2),
                CheckinItem(name = "维生素D3", emoji = "☀️", note = "具体剂量遵医嘱", reminderMinutes = 9 * 60, sortOrder = 3),
            )
        )
        settings.seeded = true
    }
}
