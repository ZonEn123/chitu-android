package com.example.chitu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull  // ✅ 导入 firstOrNull
import kotlinx.coroutines.flow.map

// 扩展属性，用于获取 DataStore 实例
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    /**
     * 保存 Token 和 UserId
     * 使用 suspend 函数，因为 DataStore 的 edit 是挂起函数
     */
    suspend fun saveToken(token: String, userId: Long) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId.toString()
        }
    }

    /**
     * 获取 Token 的 Flow，用于实时监听变化（如自动登录状态变化）
     * 返回一个 Flow<String?>，上游数据变化时会自动发射新值
     */
    fun getTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    /**
     * 获取 UserId 的 Flow
     */
    fun getUserIdFlow(): Flow<Long?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]?.toLongOrNull()
        }
    }

    /**
     * 同步获取 Token（一次性读取）
     * 用于 SplashScreen 等只需读取一次的场景
     * 使用 firstOrNull() 获取流的第一个值，然后流会自动取消收集
     */
    suspend fun getToken(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[TOKEN_KEY] }
            .firstOrNull()  // ✅ 获取第一个值，没有则返回 null
    }

    /**
     * 同步获取 UserId（一次性读取）
     */
    suspend fun getUserId(): Long? {
        return context.dataStore.data
            .map { preferences -> preferences[USER_ID_KEY]?.toLongOrNull() }
            .firstOrNull()
    }

    /**
     * 清除所有数据（退出登录时调用）
     */
    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}