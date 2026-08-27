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
            // Placeholder — will be replaced in Task 8
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🗺️ Map Screen")
            }
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
            // Placeholder — will be replaced in Task 6
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("📷 Camera Screen\nimageUrls=$imageUrls\npointId=$pointId")
            }
        }

        composable(Screen.Settings.route) {
            // Placeholder — will be replaced in Task 10
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("⚙️ Settings Screen")
            }
        }
    }
}
