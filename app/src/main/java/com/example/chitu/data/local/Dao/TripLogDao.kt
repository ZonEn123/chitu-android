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
    suspend fun insert(tripLog: TripLog)

    @Update
    suspend fun update(tripLog: TripLog)

    @Delete
    suspend fun delete(tripLog: TripLog)

    @Query("DELETE FROM trip_log WHERE id = :tripId")
    suspend fun deleteById(tripId: Long)

    @Query("UPDATE trip_log SET remark = :remark WHERE id = :tripId")
    suspend fun updateRemark(tripId: Long, remark: String)

    @Query("SELECT * FROM trip_log ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripLog>>

    @Query("SELECT * FROM trip_log WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripLog?
}