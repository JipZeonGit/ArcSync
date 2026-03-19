package com.jipzeongit.arcsync.data

data class DriverSummary(
    val version: String,
    val date: String,
    val size: String,
    val sha256: String,
    val whqlCertified: Boolean,
    val detailUrl: String,
    val downloadUrl: String,
    val isCached: Boolean = false
)

data class DriverDetail(
    val summary: DriverSummary,
    val introductionHtml: String,
    val availableDownloadsHtml: String,
    val detailedDescriptionHtml: String,
    val validProductsHtml: String
)
