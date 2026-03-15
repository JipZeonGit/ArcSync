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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.data.DriverSummary
import com.jipzeongit.arcsync.data.DriversUiState
import com.jipzeongit.arcsync.data.DriversViewModel
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DriversScreen(
    viewModel: DriversViewModel,
    onOpenDetail: (String) -> Unit,
    onOpenDownload: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshDrivers() }
    )

    LaunchedEffect(Unit) {
        viewModel.loadDrivers()
    }

    when (val ui = uiState) {
        is DriversUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DriversUiState.Error -> {
            ErrorState(message = ui.message, onRetry = { viewModel.loadDrivers() })
        }
        is DriversUiState.Data -> {
            Box(Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                DriversList(
                    drivers = ui.drivers,
                    listState = listState,
                    showRefreshing = isRefreshing,
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
    showRefreshing: Boolean,
    onOpenDetail: (String) -> Unit,
    onOpenDownload: (String) -> Unit
) {
    val isZh = Locale.getDefault().language.startsWith("zh")
    val dateLabel = if (isZh) "日期" else "Date"
    val sizeLabel = if (isZh) "大小" else "Size"
    val shaLabel = "SHA256"
    val downloadLabel = if (isZh) "下载" else "Download"

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
                if (showRefreshing) {
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        items(drivers) { driver ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDetail(driver.detailUrl) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(driver.version, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("$dateLabel: ${driver.date}", style = MaterialTheme.typography.bodyMedium)
                        Text("$sizeLabel: ${driver.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("$shaLabel: ${driver.sha256}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = downloadLabel,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable(enabled = driver.downloadUrl.isNotBlank()) {
                                onOpenDownload(driver.downloadUrl)
                            }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val isZh = Locale.getDefault().language.startsWith("zh")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isZh) "加载失败：$message" else "Load failed: $message")
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Text(if (isZh) "重试" else "Retry")
            }
        }
    }
}

