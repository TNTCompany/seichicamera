package com.tnt.seichicamera.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        Screen.bottomNavItems.forEach { screen ->
            val selected = when (screen) {
                is Screen.Camera -> currentRoute?.startsWith("camera") == true
                else -> currentRoute == screen.route
            }
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelRes)) },
                label = { Text(stringResource(screen.labelRes)) },
                selected = selected,
                onClick = {
                    val targetRoute = when (screen) {
                        is Screen.Camera -> Screen.Camera.createRoute()
                        else -> screen.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
