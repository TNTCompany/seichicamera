package com.tnt.seichicamera.data.repository

import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import com.tnt.seichicamera.data.remote.AnitabiApi
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.domain.model.SacredPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BangumiRepository @Inject constructor(
    private val api: AnitabiApi,
    private val bangumiDao: BangumiDao,
    private val pointDao: SacredPointDao
) {
    open suspend fun getBangumiPoints(subjectId: Int): Result<Pair<Bangumi, List<SacredPoint>>> {
        // 1. Try local cache first
        val cachedBangumi = bangumiDao.getById(subjectId)
        if (cachedBangumi != null) {
            val cachedPoints = pointDao.getByBangumiId(subjectId)
            if (cachedPoints.isNotEmpty()) {
                return Result.success(
                    cachedBangumi.toDomain() to cachedPoints.map { it.toDomain() }
                )
            }
        }

        // 2. Fetch from API
        return try {
            val liteResponse = api.getBangumiLite(subjectId)
            val pointsResponse = api.getBangumiPoints(subjectId)
            
            val bangumiEntity = liteResponse.toBangumiEntity()
            val pointEntities = pointsResponse.map { it.toEntity(subjectId) }

            // 3. Save to local
            bangumiDao.insert(bangumiEntity)
            pointDao.insertAll(pointEntities)

            Result.success(
                bangumiEntity.toDomain() to pointEntities.map { it.toDomain() }
            )
        } catch (e: Exception) {
            // 4. If API fails, try cache even if empty
            val fallback = bangumiDao.getById(subjectId)
            if (fallback != null) {
                val fallbackPoints = pointDao.getByBangumiId(subjectId)
                Result.success(fallback.toDomain() to fallbackPoints.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    open suspend fun cacheOffline(subjectId: Int): Result<Unit> {
        return try {
            val liteResponse = api.getBangumiLite(subjectId)
            val pointsResponse = api.getBangumiPoints(subjectId)
            
            bangumiDao.insert(liteResponse.toBangumiEntity())
            pointDao.insertAll(pointsResponse.map { it.toEntity(subjectId) })
            bangumiDao.updateCachedStatus(subjectId, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun getCachedBangumis(): List<Bangumi> =
        bangumiDao.getAllCached().map { it.toDomain() }

    open suspend fun clearCache(subjectId: Int) {
        bangumiDao.deleteById(subjectId)
        // Points deleted by CASCADE
    }

    open suspend fun clearAllCache() {
        bangumiDao.deleteAll()
    }
}
