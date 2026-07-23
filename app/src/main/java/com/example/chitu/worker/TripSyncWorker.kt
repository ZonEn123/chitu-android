package com.example.chitu.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.sync.TripSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TripSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TripSyncWorker"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 WorkManager 开始执行同步任务")

                // 1. 获取 Token
                val tokenManager = TokenManager(applicationContext)
                val token = tokenManager.getToken()

                if (token.isNullOrBlank()) {
                    Log.d(TAG, "⚠️ 无 Token，暂停同步")
                    return@withContext Result.success()
                }

                // 2. 执行同步
                val syncManager = TripSyncManager(applicationContext)
                val syncedCount = syncManager.syncAllUnsynced(token)

                Log.d(TAG, "✅ 同步完成，共上传 $syncedCount 条行程")
                Result.success()

            } catch (e: Exception) {
                Log.e(TAG, "❌ 同步任务异常", e)
                Result.retry()
            }
        }
    }
}
