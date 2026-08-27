package com.tnt.seichicamera.data.repository

import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepository @Inject constructor(
    private val checkInDao: CheckInDao
) {
    suspend fun checkIn(pointId: String, photoUri: String, comparisonUri: String? = null): Long {
        return checkInDao.insert(
            CheckInEntity(
                pointId = pointId,
                photoUri = photoUri,
                timestamp = System.currentTimeMillis(),
                comparisonUri = comparisonUri
            )
        )
    }

    fun getCheckedInPointIds(): Flow<List<String>> =
        checkInDao.getAllCheckedInPointIds()

    suspend fun isCheckedIn(pointId: String): Boolean =
        checkInDao.getByPointId(pointId) != null
}
