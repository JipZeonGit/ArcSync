package com.jipzeongit.arcsync.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

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

    var topBarElevated by remember(currentRoute) { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.DRIVERS,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.DRIVERS) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = mainContentTopPadding)
                    ) {
                        DriversScreen(
                            viewModel = viewModel,
                            appLang = appLang,
                            onOpenDetail = { url ->
                                val encoded = Uri.encode(url)
                                navController.navigate("${Routes.DETAIL}?url=$encoded")
                            },
                            onOpenDownload = { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            onScrolledChange = { topBarElevated = it }
                        )
                    }
                }
                composable(Routes.SETTINGS) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = mainContentTopPadding)
                    ) {
                        SettingsScreen(
                            settingsRepository = settingsRepository,
                            onLangChanged = { lang ->
                                viewModel.clearAndReload(lang)
                            },
                            onScrolledChange = { topBarElevated = it }
                        )
                    }
                }
                composable(
                    route = "${Routes.DETAIL}?url={url}",
                    arguments = listOf(navArgument("url") { type = NavType.StringType })
                ) { backStackEntry ->
                    val url = Uri.decode(backStackEntry.arguments?.getString("url").orEmpty())
                    DriverDetailScreen(
                        viewModel = viewModel,
                        appLang = appLang,
                        detailUrl = url,
                        onBack = { navController.popBackStack() },
                        onOpenUrl = { link -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                    )
                }
            }

            if (showMainChrome) {
                MainTopBar(elevated = topBarElevated)
            }

            if (showMainChrome) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassBottomBar(
                        currentRoute = currentRoute,
                        appLang = appLang,
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Routes.DRIVERS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun MainTopBar(elevated: Boolean) {
    val containerColor by animateColorAsState(
        targetValue = if (elevated) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(durationMillis = 220),
        label = "topBarContainer"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(MainTopBarHeight)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Arc Sync",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private data class NavItem(
    val route: String,
    val labelZh: String,
    val labelTw: String,
    val labelEn: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
)

private val navItems = listOf(
    NavItem(Routes.DRIVERS, "\u9a71\u52a8", "\u9a45\u52d5", "Drivers", Icons.Filled.Build, Icons.Outlined.Build),
    NavItem(Routes.SETTINGS, "\u8bbe\u7f6e", "\u8a2d\u5b9a", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

private fun NavItem.label(lang: AppLang): String = when (lang) {
    AppLang.ZH_CN -> labelZh
    AppLang.ZH_TW -> labelTw
    AppLang.EN -> labelEn
}

@Composable
private fun GlassBottomBar(
    currentRoute: String?,
    appLang: AppLang,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val isDarkTheme = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

    val itemPositions = remember { mutableMapOf<Int, Pair<Float, Float>>() }
    var selectedItemPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    val rowHorizontalPadding = 6.dp
    val rowHorizontalPaddingPx = with(density) { rowHorizontalPadding.toPx() }
    val indicatorInsetPx = with(density) { 4.dp.toPx() }

    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }

    LaunchedEffect(selectedIndex, selectedItemPos) {
        val raw = selectedItemPos ?: itemPositions[selectedIndex] ?: return@LaunchedEffect
        val targetX = raw.first + rowHorizontalPaddingPx - indicatorInsetPx
        val targetWidth = raw.second + indicatorInsetPx * 2f
        launch {
            indicatorX.animateTo(
                targetValue = targetX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            indicatorWidth.animateTo(
                targetValue = targetWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val glassHighColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.30f)
    val glassLowColor = if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.10f)
    val specularColor = if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.45f)
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Transparent

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                        alpha = if (isDarkTheme) 0.65f else 0.80f
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        if (indicatorWidth.value > 0f) {
                            if (isDarkTheme) {
                                drawRoundRect(
                                    color = borderColor,
                                    topLeft = Offset(indicatorX.value - 0.5.dp.toPx(), 1.5.dp.toPx()),
                                    size = Size(indicatorWidth.value + 1.dp.toPx(), size.height - 3.dp.toPx()),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(glassHighColor, glassLowColor)),
                                topLeft = Offset(indicatorX.value, 2.dp.toPx()),
                                size = Size(indicatorWidth.value, size.height - 4.dp.toPx()),
                                cornerRadius = CornerRadius(size.height / 2f)
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, specularColor, Color.Transparent),
                                    startX = indicatorX.value + indicatorWidth.value * 0.15f,
                                    endX = indicatorX.value + indicatorWidth.value * 0.85f
                                ),
                                topLeft = Offset(
                                    indicatorX.value + indicatorWidth.value * 0.15f,
                                    3.dp.toPx()
                                ),
                                size = Size(indicatorWidth.value * 0.7f, 1.5.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx())
                            )
                        }
                    }
            )

            Row(
                modifier = Modifier.padding(horizontal = rowHorizontalPadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    GlassTabItem(
                        item = item,
                        appLang = appLang,
                        isSelected = index == selectedIndex,
                        onSelect = { onNavigate(item.route) },
                        onPositioned = { x, width ->
                            itemPositions[index] = x to width
                            if (index == selectedIndex) {
                                selectedItemPos = x to width
                            }
                            if (index == selectedIndex && indicatorWidth.value == 0f) {
                                scope.launch {
                                    val snapX = x + rowHorizontalPaddingPx - indicatorInsetPx
                                    val snapWidth = width + indicatorInsetPx * 2f
                                    indicatorX.snapTo(snapX)
                                    indicatorWidth.snapTo(snapWidth)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTabItem(
    item: NavItem,
    appLang: AppLang,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPositioned: suspend (x: Float, width: Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    val iconOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-1).dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconOffsetY"
    )

    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = if (isSelected) 250 else 150),
        label = "labelAlpha"
    )

    val labelScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "labelScale"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 14.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "horizontalPadding"
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onSelect() }
            .onGloballyPositioned { coordinates ->
                val x = coordinates.positionInParent().x
                val width = coordinates.size.width.toFloat()
                scope.launch { onPositioned(x, width) }
            }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .padding(horizontal = horizontalPadding, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                contentDescription = item.label(appLang),
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        translationY = with(density) { iconOffsetY.toPx() }
                    },
                tint = iconTint
            )

            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = labelAlpha
                    scaleX = labelScale
                    scaleY = labelScale
                },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = item.label(appLang),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
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
