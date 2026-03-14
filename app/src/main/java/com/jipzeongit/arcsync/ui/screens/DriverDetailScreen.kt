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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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

    val title = when (uiState) {
        is DriverDetailState.Data -> uiState.detail.summary.version
        else -> "Driver Detail"
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is DriverDetailState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DriverDetailState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Load failed: ${uiState.message}")
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
                        Text("Date: ${detail.summary.date}")
                        Text("Size: ${detail.summary.size}")
                        Text("SHA256: ${detail.summary.sha256}")
                    }
                    if (detail.introductionHtml.isNotBlank()) {
                        item {
                            Section("Introduction") { HtmlText(detail.introductionHtml, onOpenUrl) }
                        }
                    }
                    if (detail.availableDownloadsHtml.isNotBlank()) {
                        item {
                            Section("Available Downloads") { HtmlText(detail.availableDownloadsHtml, onOpenUrl) }
                        }
                    }
                    if (detail.detailedDescriptionHtml.isNotBlank()) {
                        item {
                            Section("Detailed Description") { HtmlText(detail.detailedDescriptionHtml, onOpenUrl) }
                        }
                    }
                    if (detail.validProductsHtml.isNotBlank()) {
                        item {
                            Section("Valid Products") { HtmlText(detail.validProductsHtml, onOpenUrl) }
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
