package com.tnt.seichicamera.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.tnt.seichicamera.R
import com.tnt.seichicamera.domain.model.BangumiSearchResult
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
    val focusManager = LocalFocusManager.current
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
        // Layer 1: Full-screen map (bottom layer)
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

                            val iconDrawable = androidx.core.content.ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_map_pin
                            )?.mutate()
                            
                            if (point.id in checkedInIds) {
                                iconDrawable?.setTint(android.graphics.Color.GREEN)
                            }
                            if (iconDrawable != null) {
                                setIcon(iconDrawable)
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

        // Layer 2: Floating search bar + suggestions (top layer, over the map)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            // Floating search card
            Surface(
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 6.dp,
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text(stringResource(R.string.search_bangumi)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    },
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                viewModel.searchBangumi()
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.searchBangumi()
                    }),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Search suggestions dropdown
            if (uiState.showSearchResults && uiState.searchResults.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 300.dp)
                ) {
                    LazyColumn {
                        items(uiState.searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.selectSearchResult(result)
                                }
                            )
                            if (result != uiState.searchResults.last()) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            // Bangumi title bar (shows current loaded anime)
            uiState.bangumi?.let { bangumi ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp,
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bangumi.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.points.isNotEmpty()) {
                            Text(
                                text = "${uiState.points.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.downloadOfflineCache() }) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = stringResource(R.string.download_offline),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
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

@Composable
private fun SearchResultItem(
    result: BangumiSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        result.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.nameCn ?: result.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (result.nameCn != null && result.nameCn != result.name) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        result.airDate?.takeIf { it.isNotBlank() }?.let { date ->
            Text(
                text = date.take(4), // Show just the year
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
