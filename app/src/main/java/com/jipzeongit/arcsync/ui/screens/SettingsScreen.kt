package com.jipzeongit.arcsync.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.BuildConfig
import com.jipzeongit.arcsync.data.AppLang
import com.jipzeongit.arcsync.data.SettingsRepository
import com.jipzeongit.arcsync.ui.theme.primaryColor
import com.jipzeongit.arcsync.util.AppLogger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val GroupCardShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onLangChanged: (AppLang) -> Unit,
    onScrolledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val themeMode by settingsRepository.themeModeFlow
        .collectAsStateWithLifecycle(initialValue = SettingsRepository.ThemeMode.SYSTEM)
    val themeStyle by settingsRepository.themeStyleFlow
        .collectAsStateWithLifecycle(initialValue = SettingsRepository.ThemeStyle.DYNAMIC)
    val amoledEnabled by settingsRepository.amoledEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val appLang by settingsRepository.appLangFlow
        .collectAsStateWithLifecycle(initialValue = AppLang.ZH_CN)

    val isDark = when (themeMode) {
        SettingsRepository.ThemeMode.ON -> true
        SettingsRepository.ThemeMode.OFF -> false
        SettingsRepository.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val strings = Strings.of(appLang)

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
            .distinctUntilChanged()
            .collect(onScrolledChange)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item { SectionHeader(strings.appearance) }

        item {
            GroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeModeOption(
                        icon = Icons.Filled.LightMode,
                        label = strings.light,
                        isSelected = themeMode == SettingsRepository.ThemeMode.OFF,
                        onClick = { scope.launch { settingsRepository.setThemeMode(SettingsRepository.ThemeMode.OFF) } },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeOption(
                        icon = Icons.Filled.DarkMode,
                        label = strings.dark,
                        isSelected = themeMode == SettingsRepository.ThemeMode.ON,
                        onClick = { scope.launch { settingsRepository.setThemeMode(SettingsRepository.ThemeMode.ON) } },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeOption(
                        icon = Icons.Filled.SettingsSuggest,
                        label = strings.system,
                        isSelected = themeMode == SettingsRepository.ThemeMode.SYSTEM,
                        onClick = { scope.launch { settingsRepository.setThemeMode(SettingsRepository.ThemeMode.SYSTEM) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            GroupCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.themeStyle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.themeStyleDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingsRepository.ThemeStyle.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { style ->
                                ThemeStyleOption(
                                    label = strings.labelFor(style),
                                    color = style.primaryColor(),
                                    isSelected = themeStyle == style,
                                    onClick = { scope.launch { settingsRepository.setThemeStyle(style) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(2 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (isDark) {
            item {
                ToggleSettingCard(
                    title = strings.amoledTitle,
                    description = strings.amoledDesc,
                    checked = amoledEnabled,
                    onCheckedChange = { scope.launch { settingsRepository.setAmoledEnabled(it) } }
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(strings.language)
        }

        item {
            GroupCard {
                Column(modifier = Modifier.padding(4.dp)) {
                    AppLang.entries.forEach { lang ->
                        val isSelected = lang == appLang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        settingsRepository.setAppLang(lang)
                                        onLangChanged(lang)
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(strings.general)
        }

        item {
            GroupCard {
                ClickableItem(
                    title = strings.exportLogs,
                    subtitle = strings.exportLogsDesc,
                    onClick = { exportLogs(context) }
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(strings.other)
        }

        item {
            GroupCard {
                Column {
                    ClickableItem(
                        title = strings.checkUpdate,
                        subtitle = strings.checkUpdateDesc,
                        onClick = { Toast.makeText(context, strings.latestVersion, Toast.LENGTH_SHORT).show() }
                    )
                    ClickableItem(
                        title = "GitHub",
                        subtitle = GITHUB_URL,
                        onClick = { openUrl(context, GITHUB_URL) }
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = strings.about,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${strings.version} ${BuildConfig.VERSION_NAME}\n\n${strings.disclaimer}\n\n${strings.thanks}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }

    LaunchedEffect(Unit) { AppLogger.log("Settings opened") }
}

private data class Strings(
    val appearance: String,
    val light: String,
    val dark: String,
    val system: String,
    val themeStyle: String,
    val themeStyleDesc: String,
    val themeDynamic: String,
    val themeOcean: String,
    val themePurple: String,
    val themeForest: String,
    val themeSlate: String,
    val themeAmber: String,
    val amoledTitle: String,
    val amoledDesc: String,
    val language: String,
    val general: String,
    val exportLogs: String,
    val exportLogsDesc: String,
    val other: String,
    val checkUpdate: String,
    val checkUpdateDesc: String,
    val latestVersion: String,
    val about: String,
    val version: String,
    val disclaimer: String,
    val thanks: String
) {
    fun labelFor(style: SettingsRepository.ThemeStyle): String = when (style) {
        SettingsRepository.ThemeStyle.DYNAMIC -> themeDynamic
        SettingsRepository.ThemeStyle.OCEAN -> themeOcean
        SettingsRepository.ThemeStyle.PURPLE -> themePurple
        SettingsRepository.ThemeStyle.FOREST -> themeForest
        SettingsRepository.ThemeStyle.SLATE -> themeSlate
        SettingsRepository.ThemeStyle.AMBER -> themeAmber
    }

    companion object {
        fun of(lang: AppLang): Strings = when (lang) {
            AppLang.ZH_CN -> Strings(
                appearance = "\u5916\u89c2",
                light = "\u6d45\u8272",
                dark = "\u6df1\u8272",
                system = "\u8ddf\u968f\u7cfb\u7edf",
                themeStyle = "\u4e3b\u9898\u98ce\u683c",
                themeStyleDesc = "\u9009\u62e9 Arc Sync \u7684\u914d\u8272\u65b9\u6848\uff0c\u52a8\u6001\u4e3b\u9898\u4f1a\u8ddf\u968f\u7cfb\u7edf\u58c1\u7eb8\u53d8\u5316\u3002",
                themeDynamic = "\u52a8\u6001",
                themeOcean = "\u6d77\u6d0b",
                themePurple = "\u7d2b\u8272",
                themeForest = "\u68ee\u6797",
                themeSlate = "\u77f3\u677f",
                themeAmber = "\u7425\u73c0",
                amoledTitle = "AMOLED \u9ed1\u8272\u4e3b\u9898",
                amoledDesc = "\u4f7f\u7528\u7eaf\u9ed1\u80cc\u666f\u4ee5\u8282\u7701 OLED \u5c4f\u5e55\u7535\u91cf",
                language = "\u8bed\u8a00",
                general = "\u5e38\u89c4",
                exportLogs = "\u5bfc\u51fa\u5e94\u7528\u65e5\u5fd7",
                exportLogsDesc = "\u5206\u4eab\u8fd0\u884c\u65e5\u5fd7",
                other = "\u5176\u4ed6",
                checkUpdate = "\u83b7\u53d6\u66f4\u65b0",
                checkUpdateDesc = "\u68c0\u67e5\u66f4\u65b0",
                latestVersion = "\u5df2\u662f\u6700\u65b0\u7248\u672c",
                about = "\u5173\u4e8e",
                version = "\u7248\u672c",
                disclaimer = "\u672c\u9879\u76ee\u4e3a\u793e\u533a\u9a71\u52a8\u7684\u7b2c\u4e09\u65b9\u975e\u5b98\u65b9\u5f00\u6e90\u5de5\u5177\u3002\u672c\u8f6f\u4ef6\u4e0e\u82f1\u7279\u5c14\u516c\u53f8 (Intel Corporation) \u6ca1\u6709\u4efb\u4f55\u5173\u8054\u3001\u6388\u6743\u6216\u80cc\u4e66\u3002",
                thanks = "\u7279\u522b\u611f\u8c22 GitHub Store \u7684\u4f18\u79c0 UI \u8bbe\u8ba1\u4e3a\u672c\u9879\u76ee\u63d0\u4f9b\u7684\u53c2\u8003\u3002"
            )
            AppLang.ZH_TW -> Strings(
                appearance = "\u5916\u89c0",
                light = "\u6dfa\u8272",
                dark = "\u6df1\u8272",
                system = "\u8ddf\u96a8\u7cfb\u7d71",
                themeStyle = "\u4e3b\u984c\u98a8\u683c",
                themeStyleDesc = "\u9078\u64c7 Arc Sync \u7684\u914d\u8272\u65b9\u6848\uff0c\u52d5\u614b\u4e3b\u984c\u6703\u8ddf\u96a8\u7cfb\u7d71\u684c\u5e03\u8b8a\u5316\u3002",
                themeDynamic = "\u52d5\u614b",
                themeOcean = "\u6d77\u6d0b",
                themePurple = "\u7d2b\u8272",
                themeForest = "\u68ee\u6797",
                themeSlate = "\u77f3\u677f",
                themeAmber = "\u7425\u73c0",
                amoledTitle = "AMOLED \u9ed1\u8272\u4e3b\u984c",
                amoledDesc = "\u4f7f\u7528\u7d14\u9ed1\u80cc\u666f\u4ee5\u7bc0\u7701 OLED \u87a2\u5e55\u96fb\u91cf",
                language = "\u8a9e\u8a00",
                general = "\u4e00\u822c",
                exportLogs = "\u532f\u51fa\u61c9\u7528\u65e5\u8a8c",
                exportLogsDesc = "\u5206\u4eab\u57f7\u884c\u65e5\u8a8c",
                other = "\u5176\u4ed6",
                checkUpdate = "\u53d6\u5f97\u66f4\u65b0",
                checkUpdateDesc = "\u6aa2\u67e5\u66f4\u65b0",
                latestVersion = "\u5df2\u662f\u6700\u65b0\u7248\u672c",
                about = "\u95dc\u65bc",
                version = "\u7248\u672c",
                disclaimer = "\u672c\u5c08\u6848\u70ba\u793e\u7fa4\u9a45\u52d5\u7684\u7b2c\u4e09\u65b9\u975e\u5b98\u65b9\u958b\u6e90\u5de5\u5177\u3002\u672c\u8edf\u9ad4\u8207\u82f1\u7279\u723e\u516c\u53f8 (Intel Corporation) \u6c92\u6709\u4efb\u4f55\u95dc\u806f\u3001\u6388\u6b0a\u6216\u80cc\u66f8\u3002",
                thanks = "\u7279\u5225\u611f\u8b1d GitHub Store \u7684\u512a\u79c0 UI \u8a2d\u8a08\u70ba\u672c\u5c08\u6848\u63d0\u4f9b\u53c3\u8003\u3002"
            )
            AppLang.EN -> Strings(
                appearance = "Appearance",
                light = "Light",
                dark = "Dark",
                system = "System",
                themeStyle = "Theme Style",
                themeStyleDesc = "Choose the color preset for Arc Sync. Dynamic follows the system wallpaper.",
                themeDynamic = "Dynamic",
                themeOcean = "Ocean",
                themePurple = "Purple",
                themeForest = "Forest",
                themeSlate = "Slate",
                themeAmber = "Amber",
                amoledTitle = "AMOLED Black Theme",
                amoledDesc = "Pure black background for OLED power saving",
                language = "Language",
                general = "General",
                exportLogs = "Export App Logs",
                exportLogsDesc = "Share runtime logs",
                other = "Other",
                checkUpdate = "Check for Updates",
                checkUpdateDesc = "Check for updates",
                latestVersion = "Already up to date",
                about = "About",
                version = "Version",
                disclaimer = "This is a community-driven, unofficial open-source tool. This software is not affiliated with, endorsed by, or authorized by Intel Corporation.",
                thanks = "Special thanks to GitHub Store for its excellent UI design, which served as a reference for this project."
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    ElevatedCard(
        shape = GroupCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun ThemeModeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "themeScale"
    )

    Column(
        modifier = modifier
            .scale(animScale)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ThemeStyleOption(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animScale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "themeStyleScale"
    )

    Row(
        modifier = modifier
            .scale(animScale)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ToggleSettingCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Switch
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun ClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val GITHUB_URL = "https://github.com/JipZeonGit/ArcSync"

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun exportLogs(context: Context) {
    val file = AppLogger.exportToFile() ?: return
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Logs"))
}
