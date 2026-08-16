package com.jipzeongit.arcsync.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jipzeongit.arcsync.data.AppLang
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
    val appLang by settingsRepository.appLangFlow
        .collectAsStateWithLifecycle(initialValue = AppLang.ZH_CN)

    val currentRoute = currentRoute(navController)
    val showMainChrome = currentRoute == Routes.DRIVERS || currentRoute == Routes.SETTINGS
    val density = LocalDensity.current
    val statusBarTopPadding = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val mainContentTopPadding = statusBarTopPadding + MainTopBarHeight + MainContentGap

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 页面内容
            NavHost(
                navController = navController,
                startDestination = Routes.DRIVERS,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (showMainChrome) mainContentTopPadding else 0.dp)
            ) {
                composable(Routes.DRIVERS) {
                    DriversScreen(
                        viewModel = viewModel,
                        appLang = appLang,
                        onOpenDetail = { url ->
                            navController.navigate("${Routes.DETAIL}?url=${Uri.encode(url)}")
                        },
                        onOpenDownload = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        onScrolledChange = { }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settingsRepository = settingsRepository,
                        onLangChanged = { },
                        onScrolledChange = { }
                    )
                }

                composable(
                    route = "${Routes.DETAIL}?url={url}",
                    arguments = listOf(navArgument("url") { type = NavType.StringType })
                ) { backStackEntry ->
                    val url = backStackEntry.arguments?.getString("url") ?: ""
                    DriverDetailScreen(
                        viewModel = viewModel,
                        appLang = appLang,
                        detailUrl = url,
                        onBack = { navController.popBackStack() },
                        onOpenUrl = { openUrl ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(openUrl)))
                        }
                    )
                }
            }

            // 顶部导航栏（保留原有效果）
            if (showMainChrome) {
                GlassTopBar(
                    currentRoute = currentRoute,
                    driversLabel = appLang.driversLabel,
                    settingsLabel = appLang.settingsLabel,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.DRIVERS) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 底部导航栏 - 简化版玻璃效果
            if (showMainChrome) {
                GlassBottomBar(
                    currentRoute = currentRoute,
                    appLang = appLang,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.DRIVERS) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassTopBar(
    currentRoute: String?,
    driversLabel: String,
    settingsLabel: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MainTopBarHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarItem(
            icon = if (currentRoute == Routes.DRIVERS) Icons.Filled.Build else Icons.Outlined.Build,
            label = driversLabel,
            isSelected = currentRoute == Routes.DRIVERS,
            onClick = { onNavigate(Routes.DRIVERS) }
        )
        TopBarItem(
            icon = if (currentRoute == Routes.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
            label = settingsLabel,
            isSelected = currentRoute == Routes.SETTINGS,
            onClick = { onNavigate(Routes.SETTINGS) }
        )
    }
}

@Composable
private fun TopBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )
    }
}

@Composable
private fun GlassBottomBar(
    currentRoute: String?,
    appLang: AppLang,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = remember { listOf(NavItem.Drivers, NavItem.Settings) }
    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val isLightTheme = !isSystemInDarkTheme()

    // 配色 - 模拟玻璃效果
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val bgColor = if (isLightTheme) {
        Color(0xFFF0F0F0).copy(alpha = 0.85f)
    } else {
        Color(0xFF1A1A1A).copy(alpha = 0.85f)
    }
    val indicatorColor = if (isLightTheme) {
        Color.White.copy(alpha = 0.7f)
    } else {
        Color(0xFF2A2A2A).copy(alpha = 0.7f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) indicatorColor else Color.Transparent)
                    .clickable { onNavigate(item.route) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) accentColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = item.label(appLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private enum class NavItem(
    val route: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
    val label: (AppLang) -> String
) {
    Drivers(
        route = Routes.DRIVERS,
        iconFilled = Icons.Filled.Build,
        iconOutlined = Icons.Outlined.Build,
        label = { lang -> lang.driversLabel }
    ),
    Settings(
        route = Routes.SETTINGS,
        iconFilled = Icons.Filled.Settings,
        iconOutlined = Icons.Outlined.Settings,
        label = { lang -> lang.settingsLabel }
    )
}

private val AppLang.driversLabel: String
    get() = when (this) {
        AppLang.ZH_CN -> "驱动"
        AppLang.ZH_TW -> "驅動"
        AppLang.EN -> "Drivers"
    }

private val AppLang.settingsLabel: String
    get() = when (this) {
        AppLang.ZH_CN -> "设置"
        AppLang.ZH_TW -> "設定"
        AppLang.EN -> "Settings"
    }

private object Routes {
    const val DRIVERS = "drivers"
    const val SETTINGS = "settings"
    const val DETAIL = "detail"
}

private val MainTopBarHeight = 64.dp
private val MainContentGap = 12.dp

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route?.substringBefore("?")
}
