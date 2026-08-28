package com.tnt.seichicamera.data.repository

import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import com.tnt.seichicamera.data.remote.AnitabiApi
import com.tnt.seichicamera.data.remote.dto.BangumiResponse
import com.tnt.seichicamera.data.remote.dto.LitePoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BangumiRepositoryTest {

    private lateinit var fakeApi: FakeAnitabiApi
    private lateinit var fakeBangumiDao: FakeBangumiDao
    private lateinit var fakePointDao: FakePointDao
    private lateinit var repository: BangumiRepository

    private val testLiteResponse = BangumiResponse(
        id = 100,
        titleCn = "摇曳露营",
        title = "Laid-Back Camp",
        city = "Yamanashi",
        cover = "cover100",
        zoom = 12f
    )
    
    private val testPointsResponse = listOf(
        com.tnt.seichicamera.data.remote.dto.PointDetailItem(
            id = "pt_1",
            name = "Kouan Camping Ground",
            geo = listOf(35.47, 138.57),
            image = "img1.jpg",
            originURL = "https://anitabi.cn/map?point=pt_1",
            ep = kotlinx.serialization.json.JsonPrimitive("1")
        )
    )

    @Before
    fun setUp() {
        fakeApi = FakeAnitabiApi()
        fakeBangumiDao = FakeBangumiDao()
        fakePointDao = FakePointDao()
        repository = BangumiRepository(fakeApi, fakeBangumiDao, fakePointDao)
    }

    @Test
    fun `getBangumiPoints returns cached data when cache exists`() = runTest {
        val cachedBangumi = BangumiEntity(id = 100, title = "Cached Camp", coverUrl = "cached.jpg", region = "Yamanashi", zoom = 12f, cachedAt = 1000L, isCached = true)
        val cachedPoint = SacredPointEntity(id = "pt_1", bangumiId = 100, name = "Cached Point", latitude = 35.47, longitude = 138.57, imageUrls = listOf("cached_img.jpg"), originUrl = null, ep = null)
        fakeBangumiDao.items[100] = cachedBangumi
        fakePointDao.items["pt_1"] = cachedPoint

        val result = repository.getBangumiPoints(100)

        assertTrue(result.isSuccess)
        val (bangumi, points) = result.getOrThrow()
        assertEquals("Cached Camp", bangumi.title)
        assertEquals(1, points.size)
        assertEquals("Cached Point", points[0].name)
        // API should not have been called
        assertEquals(0, fakeApi.callCount)
    }

    @Test
    fun `getBangumiPoints fetches from network and caches when cache is empty`() = runTest {
        fakeApi.liteResponseToReturn = testLiteResponse
        fakeApi.pointsResponseToReturn = testPointsResponse

        val result = repository.getBangumiPoints(100)

        assertTrue(result.isSuccess)
        val (bangumi, points) = result.getOrThrow()
        assertEquals("摇曳露营", bangumi.title)
        assertEquals(1, points.size)
        assertEquals("Kouan Camping Ground", points[0].name)
        assertEquals(1, fakeApi.callCount)

        // Verify stored in DB
        assertNotNull(fakeBangumiDao.items[100])
        assertNotNull(fakePointDao.items["pt_1"])
    }

    @Test
    fun `getBangumiPoints falls back to cache on network failure`() = runTest {
        fakeApi.shouldThrow = true
        val fallbackBangumi = BangumiEntity(id = 100, title = "Fallback Camp", coverUrl = "cover.jpg", region = "Yamanashi", zoom = 12f, cachedAt = 1000L, isCached = false)
        val fallbackPoint = SacredPointEntity(id = "pt_1", bangumiId = 100, name = "Fallback Point", latitude = 35.47, longitude = 138.57, imageUrls = emptyList(), originUrl = null, ep = null)
        fakeBangumiDao.items[100] = fallbackBangumi
        fakePointDao.items["pt_1"] = fallbackPoint

        val result = repository.getBangumiPoints(100)

        assertTrue(result.isSuccess)
        val (bangumi, _) = result.getOrThrow()
        assertEquals("Fallback Camp", bangumi.title)
    }

    @Test
    fun `getBangumiPoints returns failure on network failure when no cache exists`() = runTest {
        fakeApi.shouldThrow = true

        val result = repository.getBangumiPoints(100)

        assertTrue(result.isFailure)
    }

    @Test
    fun `cacheOffline saves data and marks as cached`() = runTest {
        fakeApi.liteResponseToReturn = testLiteResponse
        fakeApi.pointsResponseToReturn = testPointsResponse

        val result = repository.cacheOffline(100)

        assertTrue(result.isSuccess)
        assertTrue(fakeBangumiDao.items[100]?.isCached == true)
        assertEquals(1, fakePointDao.items.size)
    }

    @Test
    fun `clearCache removes bangumi by ID`() = runTest {
        fakeBangumiDao.items[100] = BangumiEntity(id = 100, title = "Camp", coverUrl = "", region = "", zoom = 1f, cachedAt = 1000L, isCached = true)

        repository.clearCache(100)

        assertTrue(fakeBangumiDao.items.isEmpty())
    }

    @Test
    fun `clearAllCache removes all bangumis`() = runTest {
        fakeBangumiDao.items[100] = BangumiEntity(id = 100, title = "Camp 1", coverUrl = "", region = "", zoom = 1f, cachedAt = 1000L, isCached = true)
        fakeBangumiDao.items[200] = BangumiEntity(id = 200, title = "Camp 2", coverUrl = "", region = "", zoom = 1f, cachedAt = 1000L, isCached = true)

        repository.clearAllCache()

        assertTrue(fakeBangumiDao.items.isEmpty())
    }

    // --- Fakes ---

    private class FakeAnitabiApi : AnitabiApi {
        var liteResponseToReturn: BangumiResponse? = null
        var pointsResponseToReturn: List<com.tnt.seichicamera.data.remote.dto.PointDetailItem>? = null
        var shouldThrow = false
        var callCount = 0

        override suspend fun getBangumiLite(subjectId: Int): BangumiResponse {
            callCount++
            if (shouldThrow) throw RuntimeException("Network timeout")
            return liteResponseToReturn ?: throw IllegalStateException("No lite response set")
        }

        override suspend fun getBangumiPoints(subjectId: Int): List<com.tnt.seichicamera.data.remote.dto.PointDetailItem> {
            if (shouldThrow) throw RuntimeException("Network timeout")
            return pointsResponseToReturn ?: emptyList()
        }
    }

    private class FakeBangumiDao : BangumiDao {
        val items = mutableMapOf<Int, BangumiEntity>()

        override suspend fun getById(id: Int): BangumiEntity? = items[id]
        override suspend fun insert(bangumi: BangumiEntity) { items[bangumi.id] = bangumi }
        override suspend fun deleteById(id: Int) { items.remove(id) }
        override suspend fun getAllCached(): List<BangumiEntity> = items.values.filter { it.isCached }
        override suspend fun updateCachedStatus(id: Int, isCached: Boolean) {
            items[id]?.let { items[id] = it.copy(isCached = isCached) }
        }
        override suspend fun deleteAll() { items.clear() }
    }

    private class FakePointDao : SacredPointDao {
        val items = mutableMapOf<String, SacredPointEntity>()

        override suspend fun getByBangumiId(bangumiId: Int): List<SacredPointEntity> =
            items.values.filter { it.bangumiId == bangumiId }
        override suspend fun insertAll(points: List<SacredPointEntity>) {
            points.forEach { items[it.id] = it }
        }
        override suspend fun deleteByBangumiId(bangumiId: Int) {
            items.entries.removeIf { it.value.bangumiId == bangumiId }
        }
        override suspend fun getById(id: String): SacredPointEntity? = items[id]
    }
}
