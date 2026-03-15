package com.jipzeongit.arcsync.data

import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class IntelArcRepository {
    private val detailCache = mutableMapOf<String, DriverDetail>()
    private val listCache = mutableListOf<DriverSummary>()

    private val cookieJar = InMemoryCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun fetchAllDrivers(): List<DriverSummary> = withContext(Dispatchers.IO) {
        if (listCache.isNotEmpty()) return@withContext listCache

        val url = if (isChinese()) CN_URL else EN_URL
        seedCookies(url)
        val doc = fetchDocument(url)

        val detailUrls = extractDetailUrls(doc)
        val summaries = mutableListOf<DriverSummary>()

        if (detailUrls.isEmpty()) {
            val detail = parseDetail(doc, url)
            detailCache[url] = detail
            summaries += detail.summary
        } else {
            for (detailUrl in detailUrls) {
                seedCookies(detailUrl)
                val detailDoc = fetchDocument(detailUrl)
                val detail = parseDetail(detailDoc, detailUrl)
                detailCache[detailUrl] = detail
                summaries += detail.summary
            }
        }

        val sorted = summaries.distinctBy { it.version }.sortedByDescending { it.date }
        listCache += sorted
        return@withContext listCache
    }

    suspend fun fetchDriverDetail(detailUrl: String): DriverDetail = withContext(Dispatchers.IO) {
        detailCache[detailUrl]?.let { return@withContext it }

        seedCookies(detailUrl)
        val doc = fetchDocument(detailUrl)
        val detail = parseDetail(doc, detailUrl)
        detailCache[detailUrl] = detail
        return@withContext detail
    }

    private fun fetchDocument(url: String): Document {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", ACCEPT)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .header("Referer", refererFor(url))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IOException("HTTP error ${response.code} for $url")
        }
        val body = response.body?.string().orEmpty()
        response.close()
        return Jsoup.parse(body, url)
    }

    private fun seedCookies(url: String) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", ACCEPT)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .build()
        client.newCall(request).execute().close()
    }

    private fun refererFor(url: String): String {
        return if (url.contains("intel.cn")) CN_URL else EN_URL
    }

    private fun parseDetail(doc: Document, detailUrl: String): DriverDetail {
        val version = extractValueByLabels(doc, listOf("Version", "版本"))
        val date = extractValueByLabels(doc, listOf("Date", "日期"))
        val size = extractValueByLabels(doc, listOf("Size", "大小"))
        val sha256 = extractValueByLabels(doc, listOf("SHA256", "SHA-256", "SHA 256"))

        val downloadLink = doc.select("a[href]")
            .firstOrNull { el ->
                val text = el.text().lowercase(Locale.getDefault())
                text.contains("download") || text.contains("下载")
            }
            ?.absUrl("href")
            .orEmpty()

        val summary = DriverSummary(
            version = if (version.isNotBlank()) version else "Unknown",
            date = if (date.isNotBlank()) date else "Unknown",
            size = if (size.isNotBlank()) size else "Unknown",
            sha256 = if (sha256.isNotBlank()) sha256 else "Unknown",
            detailUrl = detailUrl,
            downloadUrl = downloadLink
        )

        val introductionHtml = extractSectionHtml(doc, INTRO_LABELS)
        val downloadsHtml = extractSectionHtml(doc, DOWNLOADS_LABELS)
        val detailHtml = extractSectionHtml(doc, DETAIL_LABELS)
        val validProductsHtml = extractSectionHtml(doc, VALID_LABELS)

        return DriverDetail(
            summary = summary,
            introductionHtml = introductionHtml,
            availableDownloadsHtml = downloadsHtml,
            detailedDescriptionHtml = detailHtml,
            validProductsHtml = validProductsHtml
        )
    }

    private fun extractDetailUrls(doc: Document): List<String> {
        val urls = doc.select("a[href]")
            .mapNotNull { it.absUrl("href") }
            .filter { href ->
                href.contains("intel-arc-graphics-windows") && href.contains("/download/")
            }
            .distinct()

        if (urls.isNotEmpty()) return urls

        return doc.select("a[href]")
            .mapNotNull { it.absUrl("href") }
            .filter { href -> href.contains("/download/") && href.contains("intel") }
            .distinct()
    }

    private fun extractValueByLabels(doc: Document, labels: List<String>): String {
        val label = doc.select("*" ).firstOrNull { el ->
            labels.any { el.text().trim().equals(it, ignoreCase = true) }
        }
        if (label != null) {
            val next = label.nextElementSibling()
            if (next != null && next.text().isNotBlank()) return next.text().trim()
            val parent = label.parent()
            if (parent != null) {
                val siblings = parent.children()
                val index = siblings.indexOf(label)
                if (index >= 0 && index + 1 < siblings.size) {
                    val candidate = siblings[index + 1].text().trim()
                    if (candidate.isNotBlank()) return candidate
                }
            }
        }
        return ""
    }

    private fun extractSectionHtml(doc: Document, labels: List<String>): String {
        val heading = doc.select("h1,h2,h3,h4,h5,p,strong").firstOrNull { el ->
            labels.any { label -> el.text().contains(label, ignoreCase = true) }
        } ?: return ""

        val sb = StringBuilder()
        var el: Element? = heading.nextElementSibling()
        while (el != null && !isSectionHeading(el)) {
            sb.append(el.outerHtml())
            el = el.nextElementSibling()
        }
        return sb.toString()
    }

    private fun isSectionHeading(el: Element): Boolean {
        if (el.tagName().matches(Regex("h[1-6]", RegexOption.IGNORE_CASE))) return true
        val text = el.text()
        val allLabels = INTRO_LABELS + DOWNLOADS_LABELS + DETAIL_LABELS + VALID_LABELS
        return allLabels.any { label -> text.contains(label, ignoreCase = true) }
    }

    private fun isChinese(): Boolean {
        val lang = Locale.getDefault().language
        return lang.startsWith("zh")
    }

    companion object {
        private const val EN_URL = "https://www.intel.com/content/www/us/en/download/785597/intel-arc-graphics-windows.html"
        private const val CN_URL = "https://www.intel.cn/content/www/cn/zh/download/785597/intel-arc-graphics-windows.html"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9"
        private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"

        private val INTRO_LABELS = listOf("Introduction", "介绍")
        private val DOWNLOADS_LABELS = listOf("Available Downloads", "可供下载")
        private val DETAIL_LABELS = listOf("Detailed Description", "详细说明")
        private val VALID_LABELS = listOf("This download is valid for the product(s) listed below", "此下载对下面列出的产品有效")
    }
}

private class InMemoryCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        val existing = store[key] ?: mutableListOf()
        existing.removeAll { cookie -> cookies.any { it.name == cookie.name } }
        existing.addAll(cookies)
        store[key] = existing
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host] ?: emptyList()
    }
}
