package com.example.chitu

import android.app.Application
import android.util.Log
import com.amap.api.location.AMapLocationClient
import com.example.chitu.data.sync.SyncScheduler

class ChituApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("ChituApplication", "onCreate: 初始化高德定位 SDK 隐私合规")

        // 高德定位 SDK 隐私合规
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        // 启动定时同步任务（WorkManager 兜底）
        SyncScheduler.scheduleSync(this)
        Log.d("ChituApplication", "✅ 应用启动，同步任务已调度")
    }
}
