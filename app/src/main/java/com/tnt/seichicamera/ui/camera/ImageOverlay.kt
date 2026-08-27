package com.tnt.seichicamera.ui.camera

import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ImageOverlay(
    state: OverlayState,
    onTransform: (translationX: Float, translationY: Float, scale: Float, rotation: Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onMirror: () -> Unit,
    onReset: () -> Unit,
    onPickImage: () -> Unit,
    onNextImage: () -> Unit,
    onPrevImage: () -> Unit,
    onTapOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transformState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        onTransform(
            state.translationX + panChange.x,
            state.translationY + panChange.y,
            (state.scale * zoomChange).coerceIn(0.1f, 10f),
            state.rotation + rotationChange
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Overlay image
        val imageModel: Any? = state.imageUri ?: state.currentImageUrl
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Overlay reference image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = state.translationX
                        translationY = state.translationY
                        scaleX = state.scale * if (state.isMirrored) -1f else 1f
                        scaleY = state.scale
                        rotationZ = state.rotation
                    }
                    .alpha(state.alpha)
                    .transformable(transformState)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onTapOverlay() })
                    }
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            // Multi-image navigation (only if multiple images)
            if (state.imageUrls.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevImage) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = Color.White)
                    }
                    Text(
                        "${state.currentImageIndex + 1} / ${state.imageUrls.size}",
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onNextImage) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = Color.White)
                    }
                }
            }

            // Transparency slider
            Slider(
                value = state.alpha,
                onValueChange = onAlphaChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPickImage) {
                    Icon(Icons.Default.Image, "Load image", tint = Color.White)
                }
                IconButton(onClick = onMirror) {
                    Icon(Icons.Default.Flip, "Mirror", tint = Color.White)
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.RestartAlt, "Reset", tint = Color.White)
                }
            }
        }
    }
}
