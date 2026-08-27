package com.tnt.seichicamera.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.tnt.seichicamera.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tnt.seichicamera.domain.model.SacredPoint
import com.tnt.seichicamera.ui.navigation.Screen
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkedInIds by viewModel.checkedInPointIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(36.0, 138.0)) // Japan center
        }
    }

    // Bind MapView lifecycle
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Track state to avoid resetting view/markers unnecessarily on recomposition
    var lastCenteredBangumiId by remember { mutableStateOf<Int?>(null) }
    var lastRenderedPoints by remember { mutableStateOf<List<SacredPoint>?>(null) }
    var lastRenderedCheckedInIds by remember { mutableStateOf<List<String>?>(null) }

    // Show errors
    LaunchedEffect(uiState.error, uiState.errorRes) {
        val message = when {
            uiState.errorRes != null -> {
                if (uiState.errorArg != null) {
                    context.getString(uiState.errorRes!!, uiState.errorArg)
                } else {
                    context.getString(uiState.errorRes!!)
                }
            }
            uiState.error != null -> uiState.error
            else -> null
        }
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.search_bangumi)) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchBangumi() }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchBangumi() }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Title & Offline Download
            uiState.bangumi?.let { bangumi ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bangumi.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.downloadOfflineCache() }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(R.string.download_offline)
                        )
                    }
                }
            }

            // Map
            AndroidView(
                factory = { mapView },
                update = { view ->
                    val pointsChanged = lastRenderedPoints != uiState.points ||
                            lastRenderedCheckedInIds != checkedInIds

                    if (pointsChanged) {
                        lastRenderedPoints = uiState.points
                        lastRenderedCheckedInIds = checkedInIds
                        view.overlays.clear()

                        uiState.points.forEach { point ->
                            val marker = Marker(view).apply {
                                position = GeoPoint(point.latitude, point.longitude)
                                title = point.name ?: "Point"
                                snippet = point.ep ?: ""
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                // Green if checked in, default otherwise
                                if (point.id in checkedInIds) {
                                    // Use default marker (tinted via icon in future)
                                }

                                setOnMarkerClickListener { _, _ ->
                                    viewModel.selectPoint(point)
                                    true
                                }
                            }
                            view.overlays.add(marker)
                        }
                        view.invalidate()
                    }

                    // Zoom to fit points ONLY when loaded bangumi changes
                    val currentBangumiId = uiState.bangumi?.id
                    if (currentBangumiId != null && currentBangumiId != lastCenteredBangumiId && uiState.points.isNotEmpty()) {
                        lastCenteredBangumiId = currentBangumiId
                        val avgLat = uiState.points.map { it.latitude }.average()
                        val avgLng = uiState.points.map { it.longitude }.average()
                        val zoom = uiState.bangumi?.zoom?.toDouble() ?: 12.0
                        view.controller.setCenter(GeoPoint(avgLat, avgLng))
                        view.controller.setZoom(zoom)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Point detail bottom sheet
        val sheetState = rememberModalBottomSheetState()
        uiState.selectedPoint?.let { point ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectPoint(null) },
                sheetState = sheetState
            ) {
                PointDetailSheet(
                    point = point,
                    isCheckedIn = point.id in checkedInIds,
                    onNavigate = {
                        val geoUri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(${Uri.encode(point.name ?: "Point")})")
                        val intent = Intent(Intent.ACTION_VIEW, geoUri)
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Fallback to browser
                            val webUri = Uri.parse("https://www.google.com/maps?q=${point.latitude},${point.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    onShootWithImage = { imageIndex ->
                        viewModel.selectPoint(null)
                        val urls = point.imageUrls.joinToString(",")
                        navController.navigate(Screen.Camera.createRoute(imageUrls = urls, pointId = point.id))
                    }
                )
            }
        }
    }
}
