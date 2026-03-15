package com.jipzeongit.arcsync.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.jipzeongit.arcsync.ui.components.WaveLoadingIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.data.DriverDetailState
import com.jipzeongit.arcsync.data.DriversViewModel
import com.jipzeongit.arcsync.util.HtmlText
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDetailScreen(
    viewModel: DriversViewModel,
    detailUrl: String,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    LaunchedEffect(detailUrl) {
        viewModel.loadDetail(detailUrl)
    }

    val uiState = viewModel.detailState.collectAsStateWithLifecycle().value
    val isZh = Locale.getDefault().language.startsWith("zh")

    val titleFallback = if (isZh) "驱动详情" else "Driver Detail"
    val sectionIntro = if (isZh) "介绍" else "Introduction"
    val sectionDownloads = if (isZh) "可供下载" else "Available Downloads"
    val sectionDetail = if (isZh) "详细说明" else "Detailed Description"
    val sectionValid = if (isZh) "此下载对下面列出的产品有效" else "Valid Products"
    val dateLabel = if (isZh) "日期" else "Date"
    val sizeLabel = if (isZh) "大小" else "Size"
    val shaLabel = "SHA256"

    val title = when (uiState) {
        is DriverDetailState.Data -> uiState.detail.summary.version
        else -> titleFallback
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isZh) "返回" else "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is DriverDetailState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    WaveLoadingIndicator()
                }
            }
            is DriverDetailState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(if (isZh) "加载失败：${uiState.message}" else "Load failed: ${uiState.message}")
                }
            }
            is DriverDetailState.Data -> {
                val detail = uiState.detail
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Text(detail.summary.version, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text("$dateLabel: ${detail.summary.date}")
                        Text("$sizeLabel: ${detail.summary.size}")
                        Text("$shaLabel: ${detail.summary.sha256}")
                    }
                    if (detail.introductionHtml.isNotBlank()) {
                        item {
                            Section(sectionIntro) { HtmlText(detail.introductionHtml, onOpenUrl, detailUrl) }
                        }
                    }
                    if (detail.availableDownloadsHtml.isNotBlank()) {
                        item {
                            Section(sectionDownloads) { HtmlText(detail.availableDownloadsHtml, onOpenUrl, detailUrl) }
                        }
                    }
                    if (detail.detailedDescriptionHtml.isNotBlank()) {
                        item {
                            Section(sectionDetail) { HtmlText(detail.detailedDescriptionHtml, onOpenUrl, detailUrl) }
                        }
                    }
                    if (detail.validProductsHtml.isNotBlank()) {
                        item {
                            Section(sectionValid) { HtmlText(detail.validProductsHtml, onOpenUrl, detailUrl) }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

