package com.tnt.seichicamera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckInEntity): Long

    @Query("SELECT * FROM check_in WHERE pointId = :pointId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getByPointId(pointId: String): CheckInEntity?

    @Query("SELECT DISTINCT pointId FROM check_in")
    fun getAllCheckedInPointIds(): Flow<List<String>>

    @Query("SELECT * FROM check_in ORDER BY timestamp DESC")
    fun getAllCheckIns(): Flow<List<CheckInEntity>>
}
