package com.example.chitu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "driving")

class DataStoreManager(private val context: Context) {

    companion object {
        private val START_TIMESTAMP_KEY = longPreferencesKey("start_timestamp")
    }

    suspend fun saveStartTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[START_TIMESTAMP_KEY] = timestamp
        }
    }

    // ✅ 使用 first() 获取第一个值
    suspend fun getStartTimestamp(): Long? {
        return context.dataStore.data
            .map { preferences ->
                preferences[START_TIMESTAMP_KEY]
            }
            .first()
    }

    suspend fun clearStartTimestamp() {
        context.dataStore.edit { preferences ->
            preferences.remove(START_TIMESTAMP_KEY)
        }
    }
}