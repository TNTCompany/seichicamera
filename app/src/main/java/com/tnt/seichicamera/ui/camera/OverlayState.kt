package com.tnt.seichicamera.ui.camera

import android.net.Uri

data class OverlayState(
    val imageUri: Uri? = null,
    val alpha: Float = 0.5f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val isMirrored: Boolean = false,
    val isEditing: Boolean = false,
    // For multi-image switching from map
    val imageUrls: List<String> = emptyList(),
    val currentImageIndex: Int = 0
) {
    val currentImageUrl: String?
        get() = imageUrls.getOrNull(currentImageIndex)
}
