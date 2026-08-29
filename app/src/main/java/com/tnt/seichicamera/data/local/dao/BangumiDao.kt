package com.tnt.seichicamera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnt.seichicamera.data.local.entity.BangumiEntity

@Dao
interface BangumiDao {
    @Query("SELECT * FROM bangumi WHERE id = :id")
    suspend fun getById(id: Int): BangumiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bangumi: BangumiEntity)

    @Query("DELETE FROM bangumi WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM bangumi WHERE isCached = 1")
    suspend fun getAllCached(): List<BangumiEntity>

    @Query("UPDATE bangumi SET isCached = :isCached WHERE id = :id")
    suspend fun updateCachedStatus(id: Int, isCached: Boolean)

    @Query("DELETE FROM bangumi")
    suspend fun deleteAll()
}
