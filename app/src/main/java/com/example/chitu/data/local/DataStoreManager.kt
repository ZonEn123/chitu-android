package com.example.chitu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "driving")

class DataStoreManager(private val context: Context) {

    companion object {
        private val START_TIMESTAMP_KEY = longPreferencesKey("start_timestamp")

        // 设置缓存 key
        private val DARK_MODE_KEY = intPreferencesKey("dark_mode")
        private val SOUND_ENABLED_KEY = intPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED_KEY = intPreferencesKey("vibration_enabled")
        private val REMINDER_INTERVAL_KEY = intPreferencesKey("reminder_interval")
    }

    // ========== 驾驶时间戳 ==========

    suspend fun saveStartTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[START_TIMESTAMP_KEY] = timestamp
        }
    }

    suspend fun getStartTimestamp(): Long? {
        return context.dataStore.data
            .map { preferences -> preferences[START_TIMESTAMP_KEY] }
            .first()
    }

    suspend fun clearStartTimestamp() {
        context.dataStore.edit { preferences ->
            preferences.remove(START_TIMESTAMP_KEY)
        }
    }

    // ========== 设置缓存 ==========

    /** 批量保存设置到本地 DataStore（保存到后端后同步调用） */
    suspend fun saveSettings(
        darkMode: Int? = null,
        soundEnabled: Int? = null,
        vibrationEnabled: Int? = null,
        reminderInterval: Int? = null
    ) {
        context.dataStore.edit { preferences ->
            darkMode?.let { preferences[DARK_MODE_KEY] = it }
            soundEnabled?.let { preferences[SOUND_ENABLED_KEY] = it }
            vibrationEnabled?.let { preferences[VIBRATION_ENABLED_KEY] = it }
            reminderInterval?.let { preferences[REMINDER_INTERVAL_KEY] = it }
        }
    }

    /** 读取深色模式（Flow，供 MainActivity collectAsState） */
    fun getDarkMode(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_KEY] ?: 0
        }
    }

    /** 读取声音开关（Flow） */
    fun getSoundEnabled(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[SOUND_ENABLED_KEY] ?: 1
        }
    }

    /** 读取震动开关（Flow） */
    fun getVibrationEnabled(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[VIBRATION_ENABLED_KEY] ?: 1
        }
    }

    /** 读取疲劳提醒间隔（Flow） */
    fun getReminderInterval(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[REMINDER_INTERVAL_KEY] ?: 240
        }
    }

    /** 一次性读取声音开关（用于非 Flow 环境，如 Service） */
    suspend fun getSoundEnabledOnce(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[SOUND_ENABLED_KEY] ?: 1
        }.first()
    }

    /** 一次性读取震动开关 */
    suspend fun getVibrationEnabledOnce(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[VIBRATION_ENABLED_KEY] ?: 1
        }.first()
    }

    /** 清除用户设置缓存（退出登录时调用），保留 startTimestamp 用于驾驶恢复 */
    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(DARK_MODE_KEY)
            preferences.remove(SOUND_ENABLED_KEY)
            preferences.remove(VIBRATION_ENABLED_KEY)
            preferences.remove(REMINDER_INTERVAL_KEY)
        }
    }
}
