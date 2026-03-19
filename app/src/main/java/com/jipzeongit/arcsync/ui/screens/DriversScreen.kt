package com.jipzeongit.arcsync.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.data.AppLang
import com.jipzeongit.arcsync.data.DriverSummary
import com.jipzeongit.arcsync.data.DriversUiState
import com.jipzeongit.arcsync.data.DriversViewModel
import com.jipzeongit.arcsync.ui.components.WaveLoadingIndicator
import kotlinx.coroutines.flow.distinctUntilChanged

private val CardShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriversScreen(
    viewModel: DriversViewModel,
    appLang: AppLang,
    onOpenDetail: (String) -> Unit,
    onOpenDownload: (String) -> Unit,
    onScrolledChange: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(appLang) {
        viewModel.loadDrivers(appLang)
    }

    LaunchedEffect(uiState) {
        if (uiState !is DriversUiState.Data) {
            onScrolledChange(false)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
            .distinctUntilChanged()
            .collect(onScrolledChange)
    }

    when (val ui = uiState) {
        is DriversUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WaveLoadingIndicator()
            }
        }

        is DriversUiState.Error -> {
            ErrorState(
                message = ui.message,
                appLang = appLang,
                onRetry = { viewModel.clearAndReload(appLang) }
            )
        }

        is DriversUiState.Data -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshDrivers(appLang) },
                modifier = Modifier.fillMaxSize()
            ) {
                DriversList(
                    drivers = ui.drivers,
                    listState = listState,
                    appLang = appLang,
                    onOpenDetail = onOpenDetail,
                    onOpenDownload = onOpenDownload
                )
            }
        }
    }
}

@Composable
private fun DriversList(
    drivers: List<DriverSummary>,
    listState: LazyListState,
    appLang: AppLang,
    onOpenDetail: (String) -> Unit,
    onOpenDownload: (String) -> Unit
) {
    val whqlLabel = when (appLang) {
        AppLang.ZH_CN -> "WHQL 认证"
        AppLang.ZH_TW -> "WHQL 認證"
        AppLang.EN -> "WHQL Certified"
    }
    val cachedLabel = when (appLang) {
        AppLang.ZH_CN -> "缓存数据"
        AppLang.ZH_TW -> "快取資料"
        AppLang.EN -> "Cached"
    }
    val downloadLabel = when (appLang) {
        AppLang.ZH_CN -> "下载"
        AppLang.ZH_TW -> "下載"
        AppLang.EN -> "Download"
    }
    val copyHashLabel = when (appLang) {
        AppLang.ZH_CN -> "复制 SHA256"
        AppLang.ZH_TW -> "複製 SHA256"
        AppLang.EN -> "Copy SHA256"
    }
    val cacheNotice = when (appLang) {
        AppLang.ZH_CN -> "当前显示的是缓存数据"
        AppLang.ZH_TW -> "目前顯示的是快取資料"
        AppLang.EN -> "Currently showing cached data"
    }
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        if (drivers.any { it.isCached }) {
            item {
                Text(
                    text = cacheNotice,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        items(drivers) { driver ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDetail(driver.detailUrl) },
                shape = CardShape,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = driver.version,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${driver.date}  •  ${driver.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (driver.whqlCertified || driver.isCached) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (driver.whqlCertified) {
                                    StatusChip(
                                        text = whqlLabel,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (driver.isCached) {
                                    StatusChip(
                                        text = cachedLabel,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        if (driver.sha256 != "Unknown") {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SHA256: ${driver.sha256.take(8)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(driver.sha256)) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = copyHashLabel,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    FilledTonalIconButton(
                        onClick = { onOpenDownload(driver.downloadUrl) },
                        enabled = driver.downloadUrl.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = downloadLabel
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun StatusChip(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorState(message: String, appLang: AppLang, onRetry: () -> Unit) {
    val failLabel = when (appLang) {
        AppLang.ZH_CN -> "加载失败"
        AppLang.ZH_TW -> "載入失敗"
        AppLang.EN -> "Load failed"
    }
    val retryLabel = when (appLang) {
        AppLang.ZH_CN -> "重试"
        AppLang.ZH_TW -> "重試"
        AppLang.EN -> "Retry"
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = failLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(retryLabel)
            }
        }
    }
}

