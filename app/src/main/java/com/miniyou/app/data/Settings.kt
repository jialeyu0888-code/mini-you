package com.miniyou.app.data

import android.content.Context
import com.miniyou.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class UserSettings(
    /** 末次月经第一天（epochDay），预产期由此推算 */
    val lmpEpochDay: Long? = null,
    val nickname: String = "",
    /** 用户在设置页填写的 Key，为空时使用构建时注入的 Key */
    val customApiKey: String = "",
    val knowledgeDisclaimerSeen: Boolean = false,
) {
    val onboarded: Boolean get() = lmpEpochDay != null
    val lmp: LocalDate? get() = lmpEpochDay?.let(LocalDate::ofEpochDay)
    val effectiveApiKey: String get() = customApiKey.ifBlank { BuildConfig.ZHIPU_API_KEY }
    val usingCustomKey: Boolean get() = customApiKey.isNotBlank()
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<UserSettings> = _state.asStateFlow()
    val current: UserSettings get() = _state.value

    private fun read() = UserSettings(
        lmpEpochDay = if (prefs.contains("lmp")) prefs.getLong("lmp", 0) else null,
        nickname = prefs.getString("nickname", "") ?: "",
        customApiKey = prefs.getString("api_key", "") ?: "",
        knowledgeDisclaimerSeen = prefs.getBoolean("knowledge_disclaimer", false),
    )

    fun update(transform: (UserSettings) -> UserSettings) {
        val next = transform(_state.value)
        prefs.edit().apply {
            if (next.lmpEpochDay != null) putLong("lmp", next.lmpEpochDay) else remove("lmp")
            putString("nickname", next.nickname)
            putString("api_key", next.customApiKey.trim())
            putBoolean("knowledge_disclaimer", next.knowledgeDisclaimerSeen)
        }.apply()
        _state.value = next.copy(customApiKey = next.customApiKey.trim())
    }

    var seeded: Boolean
        get() = prefs.getBoolean("seeded", false)
        set(value) = prefs.edit().putBoolean("seeded", value).apply()
}
