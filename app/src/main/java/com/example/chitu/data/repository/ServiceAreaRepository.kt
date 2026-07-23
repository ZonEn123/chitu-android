package com.example.chitu.data.repository

import android.util.Log
import com.example.chitu.data.model.ServiceArea
import com.example.chitu.data.remote.AmapRetrofitClient

class ServiceAreaRepository {

    companion object {
        private const val TAG = "ServiceAreaRepo"
        private const val AMAP_KEY = "9fcbb09c1d96c0c957d5209033e23475"  // WebAPI Key，与定位 Key 不同
    }

    suspend fun searchNearbyServiceAreas(
        latitude: Double,
        longitude: Double,
        radius: Int = 50000
    ): List<ServiceArea>? {
        return try {
            val location = "$longitude,$latitude"
            Log.d(TAG, "搜索位置: $location, 半径: ${radius / 1000}km")

            val response = AmapRetrofitClient.amapApi.searchAround(
                key = AMAP_KEY,
                location = location,
                radius = radius
            )

            if (response.status != "1") {
                Log.e(TAG, "高德 API 错误: ${response.info}")
                return null
            }

            if (response.pois.isEmpty()) {
                Log.d(TAG, "附近没有找到服务区")
                return emptyList()
            }

            Log.d(TAG, "找到 ${response.pois.size} 个服务区")
            response.pois.map { poi ->
                val (lng, lat) = poi.location.split(",").map { it.toDouble() }
                ServiceArea(
                    id = poi.id,
                    name = poi.name,
                    address = poi.address,
                    latitude = lat,
                    longitude = lng,
                    distance = poi.distance.toFloat(),
                    type = poi.type,
                    province = poi.pname,
                    city = poi.cityname
                )
            }.sortedBy { it.distance }

        } catch (e: Exception) {
            Log.e(TAG, "搜索服务区失败", e)
            null
        }
    }
}
