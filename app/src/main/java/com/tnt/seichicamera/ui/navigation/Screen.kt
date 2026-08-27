package com.tnt.seichicamera.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Map : Screen("map", "Map", Icons.Default.Map)
    data object Camera : Screen("camera?imageUrls={imageUrls}&pointId={pointId}", "Camera", Icons.Default.CameraAlt) {
        fun createRoute(imageUrls: String? = null, pointId: String? = null): String {
            return "camera?imageUrls=${imageUrls ?: ""}&pointId=${pointId ?: ""}"
        }
        const val BASE_ROUTE = "camera?imageUrls={imageUrls}&pointId={pointId}"
    }
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Map, Camera, Settings)
    }
}
