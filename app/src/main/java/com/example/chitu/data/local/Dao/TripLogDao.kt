package com.example.chitu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.chitu.data.local.entity.TripLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TripLogDao {

    @Insert
    suspend fun insert(tripLog: TripLog): Long

    @Update
    suspend fun update(tripLog: TripLog)

    @Delete
    suspend fun delete(tripLog: TripLog)

    @Query("DELETE FROM trip_log WHERE id = :tripId")
    suspend fun deleteById(tripId: Long)

    @Query("UPDATE trip_log SET remark = :remark WHERE id = :tripId")
    suspend fun updateRemark(tripId: Long, remark: String)

    // 更新同步状态
    @Query("UPDATE trip_log SET syncStatus = :status WHERE id = :tripId")
    suspend fun updateSyncStatus(tripId: Long, status: Int)

    // 更新服务器ID + 标记已同步
    @Query("UPDATE trip_log SET serverId = :serverId, syncStatus = 1 WHERE id = :tripId")
    suspend fun markAsSynced(tripId: Long, serverId: Long)

    // 查询未同步的行程
    @Query("SELECT * FROM trip_log WHERE syncStatus IN (0, 2) ORDER BY startTime ASC")
    suspend fun getUnsyncedTrips(): List<TripLog>

    // 按用户查询行程（用户数据隔离）
    @Query("SELECT * FROM trip_log WHERE userId = :userId ORDER BY startTime DESC")
    fun getTripsByUserId(userId: Long): Flow<List<TripLog>>

    @Query("SELECT * FROM trip_log ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripLog>>

    @Query("SELECT * FROM trip_log WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripLog?

    @Query("SELECT * FROM trip_log ORDER BY startTime DESC")
    suspend fun getAllTripsFromDb(): List<TripLog>
}