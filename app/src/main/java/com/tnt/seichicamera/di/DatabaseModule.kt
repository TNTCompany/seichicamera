package com.tnt.seichicamera.di

import android.content.Context
import androidx.room.Room
import com.tnt.seichicamera.data.local.AppDatabase
import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "seichi_camera.db"
        ).build()

    @Provides
    fun provideBangumiDao(db: AppDatabase): BangumiDao = db.bangumiDao()

    @Provides
    fun provideSacredPointDao(db: AppDatabase): SacredPointDao = db.sacredPointDao()

    @Provides
    fun provideCheckInDao(db: AppDatabase): CheckInDao = db.checkInDao()
}
