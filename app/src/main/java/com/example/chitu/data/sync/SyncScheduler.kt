package com.example.chitu.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.chitu.worker.TripSyncWorker
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val TAG = "SyncScheduler"
    private const val WORK_NAME = "trip_sync_work"

    /**
     * 启动定时同步任务
     * - 每 15 分钟执行一次
     * - 仅在网络连接时执行
     */
    fun scheduleSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<TripSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.MINUTES)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                2, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

        Log.d(TAG, "✅ 定时同步任务已启动（每15分钟）")
    }

    /**
     * 立即触发一次同步
     */
    fun triggerSyncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TripSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        Log.d(TAG, "🔄 手动触发同步任务")
    }

    /**
     * 停止定时同步任务
     */
    fun stopSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "⏹️ 同步任务已停止")
    }
}
