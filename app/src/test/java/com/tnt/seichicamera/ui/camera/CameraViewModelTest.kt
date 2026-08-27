package com.tnt.seichicamera.ui.camera

import android.content.Context
import android.net.Uri
import android.net.createTestUri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import com.tnt.seichicamera.data.repository.CheckInRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCheckInRepository: FakeCheckInRepository
    private lateinit var viewModel: CameraViewModel
    private val dummyUri: Uri = createTestUri()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeCheckInRepository = FakeCheckInRepository()
        viewModel = CameraViewModel(fakeCheckInRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial overlay state has default values`() {
        val state = viewModel.overlayState.value
        assertNull(state.imageUri)
        assertEquals(0.5f, state.alpha, 0.001f)
        assertEquals(0f, state.translationX, 0.001f)
        assertEquals(0f, state.translationY, 0.001f)
        assertEquals(1f, state.scale, 0.001f)
        assertEquals(0f, state.rotation, 0.001f)
        assertFalse(state.isMirrored)
        assertFalse(state.isEditing)
        assertTrue(state.imageUrls.isEmpty())
        assertEquals(0, state.currentImageIndex)
        assertNull(state.currentImageUrl)
    }

    @Test
    fun `setOverlayImage updates imageUri and clears imageUrls and currentImageIndex`() {
        viewModel.setOverlayImageUrls(listOf("https://img.com/1.jpg", "https://img.com/2.jpg"), startIndex = 1)
        viewModel.toggleEditing()
        assertTrue(viewModel.overlayState.value.isEditing)

        viewModel.setOverlayImage(dummyUri)

        val state = viewModel.overlayState.value
        assertEquals(dummyUri, state.imageUri)
        assertTrue(state.imageUrls.isEmpty())
        assertEquals(0, state.currentImageIndex)
        assertNull(state.currentImageUrl)
        assertFalse(state.isEditing)
    }

    @Test
    fun `setOverlayImageUrls updates imageUrls and startIndex and clears imageUri`() {
        viewModel.setOverlayImage(dummyUri)
        assertEquals(dummyUri, viewModel.overlayState.value.imageUri)

        val urls = listOf("https://img.com/1.jpg", "https://img.com/2.jpg", "https://img.com/3.jpg")
        viewModel.setOverlayImageUrls(urls, startIndex = 2)

        val state = viewModel.overlayState.value
        assertNull(state.imageUri)
        assertEquals(urls, state.imageUrls)
        assertEquals(2, state.currentImageIndex)
        assertEquals("https://img.com/3.jpg", state.currentImageUrl)
    }

    @Test
    fun `nextImage advances currentImageIndex within boundaries`() {
        val urls = listOf("https://img.com/1.jpg", "https://img.com/2.jpg", "https://img.com/3.jpg")
        viewModel.setOverlayImageUrls(urls, startIndex = 0)

        viewModel.nextImage()
        assertEquals(1, viewModel.overlayState.value.currentImageIndex)
        assertEquals("https://img.com/2.jpg", viewModel.overlayState.value.currentImageUrl)

        viewModel.nextImage()
        assertEquals(2, viewModel.overlayState.value.currentImageIndex)
        assertEquals("https://img.com/3.jpg", viewModel.overlayState.value.currentImageUrl)

        // Beyond last index - must stay clamped
        viewModel.nextImage()
        assertEquals(2, viewModel.overlayState.value.currentImageIndex)
    }

    @Test
    fun `prevImage decrements currentImageIndex within boundaries`() {
        val urls = listOf("https://img.com/1.jpg", "https://img.com/2.jpg", "https://img.com/3.jpg")
        viewModel.setOverlayImageUrls(urls, startIndex = 2)

        viewModel.prevImage()
        assertEquals(1, viewModel.overlayState.value.currentImageIndex)
        assertEquals("https://img.com/2.jpg", viewModel.overlayState.value.currentImageUrl)

        viewModel.prevImage()
        assertEquals(0, viewModel.overlayState.value.currentImageIndex)
        assertEquals("https://img.com/1.jpg", viewModel.overlayState.value.currentImageUrl)

        // Below index 0 - must stay clamped
        viewModel.prevImage()
        assertEquals(0, viewModel.overlayState.value.currentImageIndex)
    }

    @Test
    fun `toggleMirror toggles mirrored state`() {
        assertFalse(viewModel.overlayState.value.isMirrored)

        viewModel.toggleMirror()
        assertTrue(viewModel.overlayState.value.isMirrored)

        viewModel.toggleMirror()
        assertFalse(viewModel.overlayState.value.isMirrored)
    }

    @Test
    fun `toggleEditing toggles editing state`() {
        assertFalse(viewModel.overlayState.value.isEditing)

        viewModel.toggleEditing()
        assertTrue(viewModel.overlayState.value.isEditing)

        viewModel.toggleEditing()
        assertFalse(viewModel.overlayState.value.isEditing)
    }

    @Test
    fun `setOverlayAlpha updates alpha`() {
        viewModel.setOverlayAlpha(0.85f)
        assertEquals(0.85f, viewModel.overlayState.value.alpha, 0.001f)
    }

    @Test
    fun `updateOverlayTransform updates transform parameters`() {
        viewModel.updateOverlayTransform(translationX = 12f, translationY = -34f, scale = 2.5f, rotation = 45f)
        val state = viewModel.overlayState.value
        assertEquals(12f, state.translationX, 0.001f)
        assertEquals(-34f, state.translationY, 0.001f)
        assertEquals(2.5f, state.scale, 0.001f)
        assertEquals(45f, state.rotation, 0.001f)
    }

    @Test
    fun `resetOverlay resets transform, alpha, and mirror to defaults`() {
        viewModel.updateOverlayTransform(translationX = 100f, translationY = 50f, scale = 3f, rotation = 90f)
        viewModel.setOverlayAlpha(0.9f)
        viewModel.toggleMirror()

        viewModel.resetOverlay()

        val state = viewModel.overlayState.value
        assertEquals(0.5f, state.alpha, 0.001f)
        assertEquals(0f, state.translationX, 0.001f)
        assertEquals(0f, state.translationY, 0.001f)
        assertEquals(1f, state.scale, 0.001f)
        assertEquals(0f, state.rotation, 0.001f)
        assertFalse(state.isMirrored)
    }

    @Test
    fun `toggleFlash toggles between OFF and ON`() {
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.uiState.value.flashMode)

        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_ON, viewModel.uiState.value.flashMode)

        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.uiState.value.flashMode)
    }

    @Test
    fun `flipCamera toggles between BACK and FRONT`() {
        assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.uiState.value.lensFacing)

        viewModel.flipCamera()
        assertEquals(CameraSelector.LENS_FACING_FRONT, viewModel.uiState.value.lensFacing)

        viewModel.flipCamera()
        assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.uiState.value.lensFacing)
    }

    @Test
    fun `setAspectRatio updates aspect ratio`() {
        viewModel.setAspectRatio(AspectRatioOption.RATIO_4_3)
        assertEquals(AspectRatioOption.RATIO_4_3, viewModel.uiState.value.aspectRatio)

        viewModel.setAspectRatio(AspectRatioOption.FREE)
        assertEquals(AspectRatioOption.FREE, viewModel.uiState.value.aspectRatio)
    }

    @Test
    fun `toggleGrid toggles showGrid`() {
        assertFalse(viewModel.uiState.value.showGrid)

        viewModel.toggleGrid()
        assertTrue(viewModel.uiState.value.showGrid)

        viewModel.toggleGrid()
        assertFalse(viewModel.uiState.value.showGrid)
    }

    @Test
    fun `onPhotoCaptured and clearCapturedPhoto update capturedPhotoUri`() {
        assertNull(viewModel.uiState.value.capturedPhotoUri)

        viewModel.onPhotoCaptured(dummyUri)
        assertEquals(dummyUri, viewModel.uiState.value.capturedPhotoUri)

        viewModel.clearCapturedPhoto()
        assertNull(viewModel.uiState.value.capturedPhotoUri)
    }

    @Test
    fun `setHasFlash updates hasFlash`() {
        assertTrue(viewModel.uiState.value.hasFlash)

        viewModel.setHasFlash(false)
        assertFalse(viewModel.uiState.value.hasFlash)

        viewModel.setHasFlash(true)
        assertTrue(viewModel.uiState.value.hasFlash)
    }

    @Test
    fun `setPointId updates pointId`() {
        assertEquals("", viewModel.pointId)
        viewModel.setPointId("point_123")
        assertEquals("point_123", viewModel.pointId)
    }

    @Test
    fun `initial comparisonUri is null`() {
        assertNull(viewModel.comparisonUri.value)
    }

    @Test
    fun `checkIn saves check-in data to repository`() = runTest(testDispatcher) {
        viewModel.setPointId("point_456")
        viewModel.onPhotoCaptured(dummyUri)

        // Pass a dummy Context or null-safe context if not used directly
        val dummyContext = android.content.ContextWrapper(null)
        viewModel.checkIn(dummyContext)
        advanceUntilIdle()

        assertEquals("point_456", fakeCheckInRepository.lastCheckedInPointId)
        assertEquals(dummyUri.toString(), fakeCheckInRepository.lastPhotoUri)
        assertNull(fakeCheckInRepository.lastComparisonUri)
    }

    @Test
    fun `checkIn with blank pointId does nothing`() = runTest(testDispatcher) {
        viewModel.setPointId("")
        viewModel.onPhotoCaptured(dummyUri)

        val dummyContext = android.content.ContextWrapper(null)
        viewModel.checkIn(dummyContext)
        advanceUntilIdle()

        assertNull(fakeCheckInRepository.lastCheckedInPointId)
    }

    @Test
    fun `checkIn with null capturedPhotoUri does nothing`() = runTest(testDispatcher) {
        viewModel.setPointId("point_789")
        viewModel.clearCapturedPhoto()

        val dummyContext = android.content.ContextWrapper(null)
        viewModel.checkIn(dummyContext)
        advanceUntilIdle()

        assertNull(fakeCheckInRepository.lastCheckedInPointId)
    }

    private class FakeCheckInRepository : CheckInRepository(
        checkInDao = object : CheckInDao {
            override suspend fun insert(checkIn: CheckInEntity): Long = 1L
            override suspend fun getByPointId(pointId: String): CheckInEntity? = null
            override fun getAllCheckedInPointIds(): Flow<List<String>> = flowOf(emptyList())
            override fun getAllCheckIns(): Flow<List<CheckInEntity>> = flowOf(emptyList())
        }
    ) {
        var lastCheckedInPointId: String? = null
        var lastPhotoUri: String? = null
        var lastComparisonUri: String? = null

        override suspend fun checkIn(pointId: String, photoUri: String, comparisonUri: String?): Long {
            lastCheckedInPointId = pointId
            lastPhotoUri = photoUri
            lastComparisonUri = comparisonUri
            return 1L
        }
    }
}
