package com.tnt.seichicamera.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

import com.tnt.seichicamera.ui.camera.CameraScreen
import com.tnt.seichicamera.ui.map.MapScreen
import com.tnt.seichicamera.ui.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            MapScreen(navController = navController)
        }

        composable(
            route = Screen.Camera.BASE_ROUTE,
            arguments = listOf(
                navArgument("imageUrls") { type = NavType.StringType; defaultValue = "" },
                navArgument("pointId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val imageUrls = backStackEntry.arguments?.getString("imageUrls") ?: ""
            val pointId = backStackEntry.arguments?.getString("pointId") ?: ""
            CameraScreen(
                navController = navController,
                imageUrls = imageUrls,
                pointId = pointId
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
