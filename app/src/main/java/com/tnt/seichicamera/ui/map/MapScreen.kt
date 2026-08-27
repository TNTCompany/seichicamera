package com.tnt.seichicamera.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
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
                placeholder = { Text("Enter Bangumi Subject ID") },
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchBangumi() }) {
                        Icon(Icons.Default.Search, "Search")
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

            // Title
            uiState.bangumi?.let {
                Text(
                    text = it.title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
            }

            // Map
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(5.0)
                        controller.setCenter(GeoPoint(36.0, 138.0)) // Japan center
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    uiState.points.forEach { point ->
                        val marker = Marker(mapView).apply {
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
                        mapView.overlays.add(marker)
                    }

                    // Zoom to fit points
                    if (uiState.points.isNotEmpty()) {
                        val avgLat = uiState.points.map { it.latitude }.average()
                        val avgLng = uiState.points.map { it.longitude }.average()
                        val zoom = uiState.bangumi?.zoom?.toDouble() ?: 12.0
                        mapView.controller.setCenter(GeoPoint(avgLat, avgLng))
                        mapView.controller.setZoom(zoom)
                    }

                    mapView.invalidate()
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
