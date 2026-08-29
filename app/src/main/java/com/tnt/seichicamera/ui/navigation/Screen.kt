package com.tnt.seichicamera.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.tnt.seichicamera.R
import java.net.URLEncoder

sealed class Screen(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Map : Screen("map", R.string.nav_map, Icons.Default.Map)
    data object Camera : Screen("camera?imageUrls={imageUrls}&pointId={pointId}", R.string.nav_camera, Icons.Default.CameraAlt) {
        fun createRoute(imageUrls: String? = null, pointId: String? = null): String {
            return "camera?imageUrls=${encodeRouteArgument(imageUrls.orEmpty())}" +
                    "&pointId=${encodeRouteArgument(pointId.orEmpty())}"
        }
        const val BASE_ROUTE = "camera?imageUrls={imageUrls}&pointId={pointId}"
    }
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Map, Camera, Settings)
    }
}

private fun encodeRouteArgument(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
