package com.example.chitu.data.sync

import android.content.Context
import android.util.Log
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.local.database.TripLogDatabase
import com.example.chitu.data.local.entity.TripLog
import com.example.chitu.data.remote.RetrofitClient
import com.example.chitu.data.remote.dto.TripSyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class TripSyncManager(private val context: Context) {

    private val db by lazy { TripLogDatabase.getInstance(context) }

    companion object {
        private const val TAG = "TripSyncManager"
    }

    /**
     * 立即同步单条行程
     * @param token JWT Token
     * @param trip 本地行程数据
     * @return true-同步成功 false-同步失败（需要重试）
     */
    suspend fun syncTripNow(token: String, trip: TripLog): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = TripSyncRequest(
                    clientId = trip.clientId,
                    startTime = trip.startTime,
                    endTime = trip.endTime,
                    durationSeconds = trip.durationSeconds,
                    startLocation = trip.startLocation,
                    endLocation = trip.endLocation,
                    distanceMeters = trip.distanceMeters,
                    tripStatus = trip.tripStatus,
                    fatigueFlag = trip.fatigueFlag,
                    remark = trip.remark,
                    startLatitude = trip.startLatitude,
                    startLongitude = trip.startLongitude,
                    endLatitude = trip.endLatitude,
                    endLongitude = trip.endLongitude
                )

                val response = RetrofitClient.authApi.syncTrip("Bearer $token", request)

                if (response.code == 200) {
                    // 同步成功，更新本地状态
                    db.tripLogDao().updateSyncStatus(trip.id, 1)
                    Log.d(TAG, "✅ 行程 ${trip.id} 同步成功")
                    true
                } else {
                    db.tripLogDao().updateSyncStatus(trip.id, 2)
                    Log.e(TAG, "❌ 行程 ${trip.id} 同步失败: ${response.message}")
                    false
                }

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    db.tripLogDao().updateSyncStatus(trip.id, 2)
                    Log.e(TAG, "❌ Token 过期，停止同步")
                } else {
                    db.tripLogDao().updateSyncStatus(trip.id, 2)
                    Log.e(TAG, "❌ HTTP 错误: ${e.message}")
                }
                false

            } catch (e: IOException) {
                // 网络异常，标记同步失败（WorkManager 会兜底重试）
                db.tripLogDao().updateSyncStatus(trip.id, 2)
                Log.e(TAG, "❌ 网络异常: ${e.message}")
                false

            } catch (e: Exception) {
                db.tripLogDao().updateSyncStatus(trip.id, 2)
                Log.e(TAG, "❌ 未知异常: ${e.message}")
                false
            }
        }
    }

    /**
     * 批量同步所有未同步的行程（WorkManager 使用）
     */
    suspend fun syncAllUnsynced(token: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val unsyncedTrips = db.tripLogDao().getUnsyncedTrips()
                if (unsyncedTrips.isEmpty()) {
                    return@withContext 0
                }

                var successCount = 0
                unsyncedTrips.forEach { trip ->
                    val success = syncTripNow(token, trip)
                    if (success) successCount++
                }

                Log.d(TAG, "批量同步完成: $successCount/${unsyncedTrips.size}")
                successCount

            } catch (e: Exception) {
                Log.e(TAG, "批量同步失败", e)
                0
            }
        }
    }

    /** 获取 Token */
    suspend fun getToken(): String? {
        return try {
            TokenManager(context).getToken()
        } catch (e: Exception) {
            null
        }
    }
}
