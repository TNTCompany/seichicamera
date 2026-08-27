package com.tnt.seichicamera.ui.camera

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "CameraScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    imageUrls: String,
    pointId: String,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Image picker launcher
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.setOverlayImage(it) }
    }

    // Initialize overlay from nav args
    LaunchedEffect(imageUrls) {
        if (imageUrls.isNotBlank()) {
            val urls = imageUrls.split(",").filter { it.isNotBlank() }
            if (urls.isNotEmpty()) {
                viewModel.setOverlayImageUrls(urls)
            }
        }
    }

    // Initialize pointId from nav args
    LaunchedEffect(pointId) {
        if (pointId.isNotBlank()) {
            viewModel.setPointId(pointId)
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Camera setup
    val previewView = remember { PreviewView(context) }

    DisposableEffect(uiState.lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val capture = ImageCapture.Builder()
                .setFlashMode(uiState.flashMode)
                .build()
            imageCapture = capture

            val selector = CameraSelector.Builder()
                .requireLensFacing(uiState.lensFacing)
                .build()

            try {
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                viewModel.setHasFlash(camera.cameraInfo.hasFlashUnit())
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose { cameraProvider?.unbindAll() }
    }

    // Update flash mode when state changes
    DisposableEffect(uiState.flashMode) {
        imageCapture?.flashMode = uiState.flashMode
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        val aspectModifier = uiState.aspectRatio.ratioFloat?.let {
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f / it) // portrait: height > width
        } ?: Modifier.fillMaxSize()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = aspectModifier
            )

            // Grid overlay
            if (uiState.showGrid) {
                GridOverlay(modifier = aspectModifier)
            }

            // Image overlay
            ImageOverlay(
                state = overlayState,
                onTransform = { tx, ty, s, r -> viewModel.updateOverlayTransform(tx, ty, s, r) },
                onAlphaChange = { viewModel.setOverlayAlpha(it) },
                onMirror = { viewModel.toggleMirror() },
                onReset = { viewModel.resetOverlay() },
                onPickImage = {
                    pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onNextImage = { viewModel.nextImage() },
                onPrevImage = { viewModel.prevImage() },
                onTapOverlay = { viewModel.toggleEditing() }
            )
        }

        // Top toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Aspect ratio selector
            AspectRatioSelector(
                current = uiState.aspectRatio,
                onSelect = { viewModel.setAspectRatio(it) }
            )

            Row {
                // Grid toggle
                IconButton(onClick = { viewModel.toggleGrid() }) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = "Grid",
                        tint = if (uiState.showGrid) Color.Yellow else Color.White
                    )
                }

                // Flash toggle
                if (uiState.hasFlash) {
                    IconButton(onClick = { viewModel.toggleFlash() }) {
                        Icon(
                            if (uiState.flashMode == ImageCapture.FLASH_MODE_ON)
                                Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = Color.White
                        )
                    }
                }

                // Flip camera
                IconButton(onClick = { viewModel.flipCamera() }) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip", tint = Color.White)
                }
            }
        }

        // Bottom capture button
        FloatingActionButton(
            onClick = {
                val capture = imageCapture ?: return@FloatingActionButton
                val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SeichiCamera")
                    }
                }
                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()

                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            viewModel.onPhotoCaptured(output.savedUri)
                            Toast.makeText(context, "Photo saved", Toast.LENGTH_SHORT).show()
                        }
                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Capture failed", exception)
                            Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(36.dp))
        }

        // Post-capture bottom sheet
        if (uiState.capturedPhotoUri != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.clearCapturedPhoto() }
            ) {
                PostCaptureSheet(
                    photoUri = uiState.capturedPhotoUri!!,
                    pointId = viewModel.pointId.ifBlank { null },
                    onCheckIn = {
                        viewModel.checkIn(context)
                        Toast.makeText(context, "Checked in! ✅", Toast.LENGTH_SHORT).show()
                        viewModel.clearCapturedPhoto()
                    },
                    onGenerateComparison = {
                        viewModel.generateComparison(context)
                        viewModel.clearCapturedPhoto()
                    },
                    onDismiss = { viewModel.clearCapturedPhoto() }
                )
            }
        }
    }
}

@Composable
private fun GridOverlay(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeColor = Color.White.copy(alpha = 0.5f)
        val strokeWidth = 1.dp.toPx()

        // Vertical lines (rule of thirds)
        drawLine(strokeColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth)
        drawLine(strokeColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth)
        // Horizontal lines
        drawLine(strokeColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth)
        drawLine(strokeColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth)
    }
}

@Composable
private fun AspectRatioSelector(
    current: AspectRatioOption,
    onSelect: (AspectRatioOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(current.label, color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AspectRatioOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
