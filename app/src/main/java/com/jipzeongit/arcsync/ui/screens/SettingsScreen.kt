package com.jipzeongit.arcsync.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.BuildConfig
import com.jipzeongit.arcsync.data.SettingsRepository
import com.jipzeongit.arcsync.util.AppLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(initialValue = SettingsRepository.ThemeMode.SYSTEM)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Group(title = "常规") {
            ListItem(
                headlineContent = { Text("深色模式") },
                supportingContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ThemeSelectionCard(
                            label = "浅色",
                            icon = Icons.Filled.LightMode,
                            selected = themeMode == SettingsRepository.ThemeMode.OFF,
                            onClick = { scope.launch { settingsRepository.setThemeMode(SettingsRepository.ThemeMode.OFF) } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeSelectionCard(
                            label = "深色",
                            icon = Icons.Filled.DarkMode,
                            selected = themeMode == SettingsRepository.ThemeMode.ON,
                            onClick = { scope.launch { settingsRepository.setThemeMode(SettingsRepository.ThemeMode.ON) } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeSelectionCard(
                            label = "跟随系统",
                            icon = Icons.Filled.SettingsSuggest,
                            selected = themeMode == SettingsRepository.ThemeMode.SYSTEM,
                            onClick = { scope.launch { settingsRepository.setThemeMode(SettingsRepository.ThemeMode.SYSTEM) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )
            ListItem(
                headlineContent = { Text("导出应用日志") },
                supportingContent = { Text("分享运行日志") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { exportLogs(context) }
            )
        }
        Group(title = "其他") {
            ListItem(
                headlineContent = { Text("获取更新") },
                supportingContent = { Text("模拟检查更新") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                    }
            )
            ListItem(
                headlineContent = { Text("GitHub") },
                supportingContent = { Text(GITHUB_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openUrl(context, GITHUB_URL) }
            )
            ListItem(
                headlineContent = { Text("关于") },
                supportingContent = {
                    Text(
                        "版本 ${BuildConfig.VERSION_NAME}\n\n" +
                        "本项目为社区驱动的第三方非官方开源工具。本软件与英特尔公司 (Intel Corporation) 没有任何关联、授权或背书。"
                    )
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        AppLogger.log("Settings opened")
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                content()
            }
        }
    }
}
@Composable
private fun ThemeSelectionCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
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
    context.startActivity(Intent.createChooser(intent, "导出日志"))
}




