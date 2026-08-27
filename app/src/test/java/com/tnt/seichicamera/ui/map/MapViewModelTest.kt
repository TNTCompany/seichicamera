package com.tnt.seichicamera.ui.map

import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import com.tnt.seichicamera.data.remote.AnitabiApi
import com.tnt.seichicamera.data.remote.dto.BangumiResponse
import com.tnt.seichicamera.data.repository.BangumiRepository
import com.tnt.seichicamera.data.repository.CheckInRepository
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.domain.model.SacredPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeBangumiRepository: FakeBangumiRepository
    private lateinit var fakeCheckInRepository: FakeCheckInRepository
    private lateinit var viewModel: MapViewModel

    private val testBangumi = Bangumi(
        id = 253,
        title = "Your Name",
        coverUrl = "https://example.com/cover.jpg",
        region = "Tokyo",
        zoom = 14f
    )

    private val testPoints = listOf(
        SacredPoint(
            id = "p1",
            bangumiId = 253,
            name = "Suga Shrine Steps",
            latitude = 35.6852,
            longitude = 139.7238,
            imageUrls = listOf("https://example.com/p1.jpg"),
            originUrl = "https://anitabi.cn/map?point=p1",
            ep = "ED"
        ),
        SacredPoint(
            id = "p2",
            bangumiId = 253,
            name = "Yotsuya Station",
            latitude = 35.6860,
            longitude = 139.7300,
            imageUrls = listOf("https://example.com/p2.jpg"),
            originUrl = "https://anitabi.cn/map?point=p2",
            ep = "OP"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeBangumiRepository = FakeBangumiRepository()
        fakeCheckInRepository = FakeCheckInRepository()
        viewModel = MapViewModel(fakeBangumiRepository, fakeCheckInRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.bangumi)
        assertTrue(state.points.isEmpty())
        assertNull(state.selectedPoint)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onSearchQueryChanged updates searchQuery`() {
        viewModel.onSearchQueryChanged("253")
        assertEquals("253", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `searchBangumi with blank query does not trigger load`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("   ")
        viewModel.searchBangumi()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.bangumi)
    }

    @Test
    fun `searchBangumi with non-numeric query sets error`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("your name")
        viewModel.searchBangumi()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Please enter a Bangumi Subject ID (number)", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.bangumi)
    }

    @Test
    fun `searchBangumi with valid ID succeeds and populates state`() = runTest(testDispatcher) {
        fakeBangumiRepository.resultToReturn = Result.success(testBangumi to testPoints)

        viewModel.onSearchQueryChanged("253")
        viewModel.searchBangumi()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(testBangumi, state.bangumi)
        assertEquals(testPoints, state.points)
        assertNull(state.selectedPoint)
    }

    @Test
    fun `searchBangumi with valid ID handles failure`() = runTest(testDispatcher) {
        fakeBangumiRepository.resultToReturn = Result.failure(RuntimeException("Bangumi not found"))

        viewModel.onSearchQueryChanged("99999")
        viewModel.searchBangumi()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Bangumi not found", state.error)
        assertNull(state.bangumi)
        assertTrue(state.points.isEmpty())
    }

    @Test
    fun `selectPoint updates selectedPoint in state`() {
        viewModel.selectPoint(testPoints[0])
        assertEquals(testPoints[0], viewModel.uiState.value.selectedPoint)

        viewModel.selectPoint(null)
        assertNull(viewModel.uiState.value.selectedPoint)
    }

    @Test
    fun `clearError clears error from state`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("invalid")
        viewModel.searchBangumi()
        advanceUntilIdle()
        assertEquals("Please enter a Bangumi Subject ID (number)", viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `checkedInPointIds emits repository updates`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.checkedInPointIds.collect()
        }

        fakeCheckInRepository.checkedInIdsFlow.value = listOf("p1", "p2")
        advanceUntilIdle()
        assertEquals(listOf("p1", "p2"), viewModel.checkedInPointIds.value)
    }

    @Test
    fun `downloadOfflineCache does nothing when no bangumi loaded`() = runTest(testDispatcher) {
        viewModel.downloadOfflineCache()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertNull(fakeBangumiRepository.lastCachedSubjectId)
    }

    @Test
    fun `downloadOfflineCache succeeds and resets loading`() = runTest(testDispatcher) {
        fakeBangumiRepository.resultToReturn = Result.success(testBangumi to testPoints)
        viewModel.onSearchQueryChanged("253")
        viewModel.searchBangumi()
        advanceUntilIdle()

        fakeBangumiRepository.cacheResultToReturn = Result.success(Unit)
        viewModel.downloadOfflineCache()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(253, fakeBangumiRepository.lastCachedSubjectId)
    }

    @Test
    fun `downloadOfflineCache fails and sets error`() = runTest(testDispatcher) {
        fakeBangumiRepository.resultToReturn = Result.success(testBangumi to testPoints)
        viewModel.onSearchQueryChanged("253")
        viewModel.searchBangumi()
        advanceUntilIdle()

        fakeBangumiRepository.cacheResultToReturn = Result.failure(RuntimeException("Network error"))
        viewModel.downloadOfflineCache()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Cache failed: Network error", state.error)
        assertEquals(253, fakeBangumiRepository.lastCachedSubjectId)
    }

    // --- Fake Test Implementations ---

    private class FakeBangumiRepository : BangumiRepository(
        api = object : AnitabiApi {
            override suspend fun getBangumiPoints(subjectId: Int): BangumiResponse =
                throw UnsupportedOperationException()
        },
        bangumiDao = object : BangumiDao {
            override suspend fun getById(id: Int): BangumiEntity? = null
            override suspend fun insert(bangumi: BangumiEntity) {}
            override suspend fun deleteById(id: Int) {}
            override suspend fun getAllCached(): List<BangumiEntity> = emptyList()
            override suspend fun updateCachedStatus(id: Int, isCached: Boolean) {}
            override suspend fun deleteAll() {}
        },
        pointDao = object : SacredPointDao {
            override suspend fun getByBangumiId(bangumiId: Int): List<SacredPointEntity> = emptyList()
            override suspend fun insertAll(points: List<SacredPointEntity>) {}
            override suspend fun deleteByBangumiId(bangumiId: Int) {}
            override suspend fun getById(id: String): SacredPointEntity? = null
        }
    ) {
        var resultToReturn: Result<Pair<Bangumi, List<SacredPoint>>> =
            Result.failure(IllegalStateException("Not configured"))
        var cacheResultToReturn: Result<Unit> = Result.success(Unit)
        var lastCachedSubjectId: Int? = null

        override suspend fun getBangumiPoints(subjectId: Int): Result<Pair<Bangumi, List<SacredPoint>>> {
            return resultToReturn
        }

        override suspend fun cacheOffline(subjectId: Int): Result<Unit> {
            lastCachedSubjectId = subjectId
            return cacheResultToReturn
        }
    }

    private class FakeCheckInRepository : CheckInRepository(
        checkInDao = object : CheckInDao {
            override suspend fun insert(checkIn: CheckInEntity): Long = 1L
            override suspend fun getByPointId(pointId: String): CheckInEntity? = null
            override fun getAllCheckedInPointIds(): Flow<List<String>> = flowOf(emptyList())
            override fun getAllCheckIns(): Flow<List<CheckInEntity>> = flowOf(emptyList())
        }
    ) {
        val checkedInIdsFlow = MutableStateFlow<List<String>>(emptyList())
        override fun getCheckedInPointIds(): Flow<List<String>> = checkedInIdsFlow
    }
}
