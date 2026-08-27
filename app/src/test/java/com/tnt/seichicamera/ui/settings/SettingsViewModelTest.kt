package com.tnt.seichicamera.ui.settings

import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import com.tnt.seichicamera.data.remote.AnitabiApi
import com.tnt.seichicamera.data.remote.dto.BangumiResponse
import com.tnt.seichicamera.data.repository.BangumiRepository
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeBangumiRepository: FakeBangumiRepository
    private lateinit var viewModel: SettingsViewModel

    private val testBangumi1 = Bangumi(
        id = 253,
        title = "Your Name",
        coverUrl = "https://example.com/cover1.jpg",
        region = "Tokyo",
        zoom = 14f
    )
    private val testBangumi2 = Bangumi(
        id = 100,
        title = "Weathering with You",
        coverUrl = "https://example.com/cover2.jpg",
        region = "Tokyo",
        zoom = 14f
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeBangumiRepository = FakeBangumiRepository()
        fakeBangumiRepository.cachedList = mutableListOf(testBangumi1, testBangumi2)
        viewModel = SettingsViewModel(fakeBangumiRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads cached bangumis`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoadingCache)
        assertEquals(2, state.cachedBangumis.size)
        assertEquals(testBangumi1, state.cachedBangumis[0])
        assertEquals(testBangumi2, state.cachedBangumis[1])
    }

    @Test
    fun `clearCache removes specific item and reloads list`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.cachedBangumis.size)

        viewModel.clearCache(253)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.cachedBangumis.size)
        assertEquals(100, state.cachedBangumis[0].id)
    }

    @Test
    fun `clearAllCache removes all items and reloads empty list`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.cachedBangumis.size)

        viewModel.clearAllCache()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.cachedBangumis.isEmpty())
    }

    @Test
    fun `setLanguage updates currentLocaleTag`() {
        viewModel.setLanguage("ja")
        assertEquals("ja", viewModel.uiState.value.currentLocaleTag)

        viewModel.setLanguage("zh-CN")
        assertEquals("zh-CN", viewModel.uiState.value.currentLocaleTag)

        viewModel.setLanguage("")
        assertEquals("", viewModel.uiState.value.currentLocaleTag)
    }

    @Test
    fun `localeHelper has expected language options`() {
        val languages = LocaleHelper.languages
        assertTrue(languages.any { it.tag == "" && it.displayName == "System Default" })
        assertTrue(languages.any { it.tag == "en" && it.displayName == "English" })
        assertTrue(languages.any { it.tag == "zh-CN" && it.displayName == "简体中文" })
        assertTrue(languages.any { it.tag == "zh-HK" && it.displayName == "繁體中文（香港）" })
        assertTrue(languages.any { it.tag == "zh-TW" && it.displayName == "繁體中文（台灣）" })
        assertTrue(languages.any { it.tag == "ja" && it.displayName == "日本語" })
    }

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
        var cachedList = mutableListOf<Bangumi>()

        override suspend fun getCachedBangumis(): List<Bangumi> {
            return cachedList.toList()
        }

        override suspend fun clearCache(subjectId: Int) {
            cachedList.removeAll { it.id == subjectId }
        }

        override suspend fun clearAllCache() {
            cachedList.clear()
        }
    }
}
