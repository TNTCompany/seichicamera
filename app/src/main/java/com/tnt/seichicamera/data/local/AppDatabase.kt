package com.tnt.seichicamera.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tnt.seichicamera.data.local.converter.Converters
import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity

@Database(
    entities = [BangumiEntity::class, SacredPointEntity::class, CheckInEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bangumiDao(): BangumiDao
    abstract fun sacredPointDao(): SacredPointDao
    abstract fun checkInDao(): CheckInDao
}
