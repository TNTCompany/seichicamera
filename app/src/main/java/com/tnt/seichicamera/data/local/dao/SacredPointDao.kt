package com.tnt.seichicamera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnt.seichicamera.data.local.entity.SacredPointEntity

@Dao
interface SacredPointDao {
    @Query("SELECT * FROM sacred_point WHERE bangumiId = :bangumiId")
    suspend fun getByBangumiId(bangumiId: Int): List<SacredPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<SacredPointEntity>)

    @Query("DELETE FROM sacred_point WHERE bangumiId = :bangumiId")
    suspend fun deleteByBangumiId(bangumiId: Int)

    @Query("SELECT * FROM sacred_point WHERE id = :id")
    suspend fun getById(id: String): SacredPointEntity?
}
