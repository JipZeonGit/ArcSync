package com.jipzeongit.arcsync.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jipzeongit.arcsync.data.DriversViewModel
import com.jipzeongit.arcsync.data.SettingsRepository
import com.jipzeongit.arcsync.ui.screens.DriverDetailScreen
import com.jipzeongit.arcsync.ui.screens.DriversScreen
import com.jipzeongit.arcsync.ui.screens.SettingsScreen

@Composable
fun AppRoot(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: DriversViewModel = viewModel()

    val currentRoute = currentRoute(navController)
    val showBottomBar = currentRoute == Routes.DRIVERS || currentRoute == Routes.SETTINGS

    val items = listOf(
        NavItem(Routes.DRIVERS, "驱动", Icons.Filled.Build),
        NavItem(Routes.SETTINGS, "设置", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DRIVERS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DRIVERS) {
                DriversScreen(
                    viewModel = viewModel,
                    onOpenDetail = { url ->
                        val encoded = Uri.encode(url)
                        navController.navigate("${Routes.DETAIL}?url=$encoded")
                    },
                    onOpenDownload = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(settingsRepository)
            }
            composable(
                route = "${Routes.DETAIL}?url={url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = Uri.decode(backStackEntry.arguments?.getString("url").orEmpty())
                DriverDetailScreen(
                    viewModel = viewModel,
                    detailUrl = url,
                    onBack = { navController.popBackStack() },
                    onOpenUrl = { link -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                )
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private object Routes {
    const val DRIVERS = "drivers"
    const val SETTINGS = "settings"
    const val DETAIL = "detail"
}

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route?.substringBefore("?")
}
