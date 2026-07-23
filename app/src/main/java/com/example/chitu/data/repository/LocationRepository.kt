package com.example.chitu.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.example.chitu.data.model.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocationRepository(private val context: Context) {

    companion object {
        private const val TAG = "LocationRepo"
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private var cachedLocation: Pair<Double, Double>? = null
    private var cacheTimestamp: Long = 0L

    suspend fun getCurrentLocation(): LocationResult {
        if (cachedLocation != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
            val (lat, lng) = cachedLocation!!
            return LocationResult.Success(lat, lng)
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            return LocationResult.PermissionDenied
        }

        return withContext(Dispatchers.IO) {
            val latch = CountDownLatch(1)
            var result: LocationResult = LocationResult.Error("定位超时")

            val locationClient = AMapLocationClient(context.applicationContext)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true
                isNeedAddress = false
            }
            locationClient.setLocationOption(option)
            locationClient.setLocationListener { amapLocation ->
                if (amapLocation != null && amapLocation.errorCode == 0) {
                    val lat = amapLocation.latitude
                    val lng = amapLocation.longitude
                    cachedLocation = Pair(lat, lng)
                    cacheTimestamp = System.currentTimeMillis()
                    result = LocationResult.Success(lat, lng)
                } else {
                    result = LocationResult.Error(amapLocation?.errorInfo ?: "定位失败")
                }
                latch.countDown()
            }
            locationClient.startLocation()
            latch.await(10, TimeUnit.SECONDS)
            locationClient.stopLocation()
            locationClient.onDestroy()
            result
        }
    }

    fun clearCache() {
        cachedLocation = null
        cacheTimestamp = 0L
    }
}
