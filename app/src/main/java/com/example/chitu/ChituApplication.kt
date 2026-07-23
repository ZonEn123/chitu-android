package com.example.chitu

import android.app.Application
import android.util.Log
import com.amap.api.location.AMapLocationClient

class ChituApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("ChituApplication", "onCreate: 初始化高德定位 SDK 隐私合规")

        // 高德定位 SDK 6.x+ 必须在任何 SDK 接口调用前设置隐私合规
        // 假设用户已同意隐私政策（实际项目中应从 SharedPreferences 读取用户授权状态）
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
    }
}
