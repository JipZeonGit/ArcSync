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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jipzeongit.arcsync.data.AppLang
import com.jipzeongit.arcsync.data.DriverDetailState
import com.jipzeongit.arcsync.data.DriversViewModel
import com.jipzeongit.arcsync.ui.components.WaveLoadingIndicator
import com.jipzeongit.arcsync.util.HtmlText

private val SectionCardShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDetailScreen(
    viewModel: DriversViewModel,
    appLang: AppLang,
    detailUrl: String,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    LaunchedEffect(detailUrl, appLang) {
        viewModel.loadDetail(detailUrl)
    }

    val uiState = viewModel.detailState.collectAsStateWithLifecycle().value

    val titleFallback = when (appLang) {
        AppLang.ZH_CN -> "\u9a71\u52a8\u8be6\u60c5"
        AppLang.ZH_TW -> "\u9a45\u52d5\u8a73\u60c5"
        AppLang.EN -> "Driver Detail"
    }
    val sectionIntro = when (appLang) {
        AppLang.ZH_CN -> "\u4ecb\u7ecd"
        AppLang.ZH_TW -> "\u4ecb\u7d39"
        AppLang.EN -> "Introduction"
    }
    val sectionDetail = when (appLang) {
        AppLang.ZH_CN -> "\u8be6\u7ec6\u8bf4\u660e"
        AppLang.ZH_TW -> "\u8a73\u7d30\u8aaa\u660e"
        AppLang.EN -> "Detailed Description"
    }
    val sectionValid = when (appLang) {
        AppLang.ZH_CN -> "\u6b64\u4e0b\u8f7d\u5bf9\u4e0b\u9762\u5217\u51fa\u7684\u4ea7\u54c1\u6709\u6548"
        AppLang.ZH_TW -> "\u6b64\u4e0b\u8f09\u5c0d\u4e0b\u9762\u5217\u51fa\u7684\u7522\u54c1\u6709\u6548"
        AppLang.EN -> "Valid Products"
    }
    val dateLabel = when (appLang) {
        AppLang.ZH_CN -> "\u65e5\u671f"
        AppLang.ZH_TW -> "\u65e5\u671f"
        AppLang.EN -> "Date"
    }
    val sizeLabel = when (appLang) {
        AppLang.ZH_CN -> "\u5927\u5c0f"
        AppLang.ZH_TW -> "\u5927\u5c0f"
        AppLang.EN -> "Size"
    }
    val backLabel = when (appLang) {
        AppLang.ZH_CN -> "\u8fd4\u56de"
        AppLang.ZH_TW -> "\u8fd4\u56de"
        AppLang.EN -> "Back"
    }
    val failLabel = when (appLang) {
        AppLang.ZH_CN -> "\u52a0\u8f7d\u5931\u8d25"
        AppLang.ZH_TW -> "\u8f09\u5165\u5931\u6557"
        AppLang.EN -> "Load failed"
    }
    val shaLabel = "SHA256"

    val title = when (uiState) {
        is DriverDetailState.Data -> uiState.detail.summary.version
        else -> titleFallback
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backLabel
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is DriverDetailState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    WaveLoadingIndicator()
                }
            }

            is DriverDetailState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$failLabel: ${uiState.message}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is DriverDetailState.Data -> {
                val detail = uiState.detail
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ElevatedCard(
                            shape = SectionCardShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = detail.summary.version,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                InfoLine("$dateLabel: ${detail.summary.date}")
                                InfoLine("$sizeLabel: ${detail.summary.size}")
                                InfoLine("$shaLabel: ${detail.summary.sha256}")
                            }
                        }
                    }

                    if (detail.introductionHtml.isNotBlank()) {
                        item {
                            SectionCard(sectionIntro) {
                                HtmlText(detail.introductionHtml, onOpenUrl, detailUrl)
                            }
                        }
                    }

                    if (detail.detailedDescriptionHtml.isNotBlank()) {
                        item {
                            SectionCard(sectionDetail) {
                                HtmlText(detail.detailedDescriptionHtml, onOpenUrl, detailUrl)
                            }
                        }
                    }

                    if (detail.validProductsHtml.isNotBlank()) {
                        item {
                            SectionCard(sectionValid) {
                                HtmlText(detail.validProductsHtml, onOpenUrl, detailUrl)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        shape = SectionCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
