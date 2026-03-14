package com.jipzeongit.arcsync.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.BuildConfig
import com.jipzeongit.arcsync.data.SettingsRepository
import com.jipzeongit.arcsync.util.AppLogger
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(initialValue = SettingsRepository.ThemeMode.SYSTEM)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val themeLabel = when (themeMode) {
        SettingsRepository.ThemeMode.OFF -> "关闭"
        SettingsRepository.ThemeMode.ON -> "开启"
        SettingsRepository.ThemeMode.SYSTEM -> "跟随系统"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Group(title = "常规") {
            ListItem(
                headlineContent = { Text("深色模式") },
                supportingContent = { Text(themeLabel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
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
                headlineContent = { Text("关于") },
                supportingContent = { Text("版本与免责声明") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutDialog = true }
            )
            ListItem(
                headlineContent = { Text("获取更新") },
                supportingContent = { Text("模拟检查更新") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                    }
            )
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelected = { mode ->
                scope.launch { settingsRepository.setThemeMode(mode) }
                showThemeDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(version = BuildConfig.VERSION_NAME, onDismiss = { showAboutDialog = false })
    }

    LaunchedEffect(Unit) {
        AppLogger.log("Settings opened")
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ThemeDialog(
    current: SettingsRepository.ThemeMode,
    onDismiss: () -> Unit,
    onSelected: (SettingsRepository.ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("深色模式") },
        confirmButton = { },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption("关闭", current == SettingsRepository.ThemeMode.OFF) { onSelected(SettingsRepository.ThemeMode.OFF) }
                ThemeOption("开启", current == SettingsRepository.ThemeMode.ON) { onSelected(SettingsRepository.ThemeMode.ON) }
                ThemeOption("跟随系统", current == SettingsRepository.ThemeMode.SYSTEM) { onSelected(SettingsRepository.ThemeMode.SYSTEM) }
            }
        }
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    )
}

@Composable
private fun AboutDialog(version: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于") },
        text = { Text("版本 $version\n\n本项目为社区驱动的第三方非官方开源工具。本软件与英特尔公司 (Intel Corporation) 没有任何关联、授权或背书。") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        }
    )
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
