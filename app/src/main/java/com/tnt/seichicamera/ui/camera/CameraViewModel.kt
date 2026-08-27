package com.tnt.seichicamera.ui.camera

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class AspectRatioOption(val label: String, val ratioFloat: Float?) {
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_CINEMATIC("2.35:1", 2.35f),
    RATIO_1_1("1:1", 1f),
    FREE("Free", null)
}

data class CameraUiState(
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_16_9,
    val showGrid: Boolean = false,
    val capturedPhotoUri: Uri? = null,
    val hasFlash: Boolean = true
)

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun toggleFlash() {
        _uiState.update {
            val newMode = if (it.flashMode == ImageCapture.FLASH_MODE_OFF)
                ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            it.copy(flashMode = newMode)
        }
    }

    fun flipCamera() {
        _uiState.update {
            val newFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            it.copy(lensFacing = newFacing)
        }
    }

    fun setAspectRatio(ratio: AspectRatioOption) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun onPhotoCaptured(uri: Uri?) {
        _uiState.update { it.copy(capturedPhotoUri = uri) }
    }

    fun clearCapturedPhoto() {
        _uiState.update { it.copy(capturedPhotoUri = null) }
    }

    fun setHasFlash(hasFlash: Boolean) {
        _uiState.update { it.copy(hasFlash = hasFlash) }
    }

    private val _overlayState = MutableStateFlow(OverlayState())
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    fun setOverlayImage(uri: Uri) {
        _overlayState.update {
            it.copy(imageUri = uri, imageUrls = emptyList(), currentImageIndex = 0, isEditing = false)
        }
    }

    fun setOverlayImageUrls(urls: List<String>, startIndex: Int = 0) {
        _overlayState.update {
            it.copy(imageUrls = urls, currentImageIndex = startIndex, imageUri = null)
        }
    }

    fun updateOverlayTransform(translationX: Float, translationY: Float, scale: Float, rotation: Float) {
        _overlayState.update {
            it.copy(translationX = translationX, translationY = translationY, scale = scale, rotation = rotation)
        }
    }

    fun setOverlayAlpha(alpha: Float) {
        _overlayState.update { it.copy(alpha = alpha) }
    }

    fun toggleMirror() {
        _overlayState.update { it.copy(isMirrored = !it.isMirrored) }
    }

    fun resetOverlay() {
        _overlayState.update {
            it.copy(
                alpha = 0.5f, translationX = 0f, translationY = 0f,
                scale = 1f, rotation = 0f, isMirrored = false
            )
        }
    }

    fun nextImage() {
        _overlayState.update {
            val next = (it.currentImageIndex + 1).coerceAtMost(it.imageUrls.size - 1)
            it.copy(currentImageIndex = next)
        }
    }

    fun prevImage() {
        _overlayState.update {
            val prev = (it.currentImageIndex - 1).coerceAtLeast(0)
            it.copy(currentImageIndex = prev)
        }
    }

    fun toggleEditing() {
        _overlayState.update { it.copy(isEditing = !it.isEditing) }
    }
}

