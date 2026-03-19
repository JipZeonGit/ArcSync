package com.jipzeongit.arcsync.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class IntelArcRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheDirectory = File(appContext.cacheDir, "intel-arc-cache").apply { mkdirs() }
    private val detailCache = mutableMapOf<String, DriverDetail>()
    private val listCache = mutableMapOf<AppLang, List<DriverSummary>>()

    private val cookieJar = InMemoryCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAllDrivers(lang: AppLang = AppLang.ZH_CN): List<DriverSummary> = withContext(Dispatchers.IO) {
        listCache[lang]?.let { return@withContext it }

        val cachedList = readListCache(lang)
        val landingUrl = lang.baseUrl

        try {
            val landingDoc = fetchDocument(landingUrl)
            val entries = extractDetailEntries(landingDoc).take(MAX_VERSIONS)
            val previousByUrl = cachedList.associateBy { it.detailUrl }

            val summaries = if (entries.isEmpty()) {
                val detail = parseDetail(landingDoc, landingUrl)
                detailCache[detail.summary.detailUrl] = detail
                writeDetailCache(detail.summary.detailUrl, detail)
                listOf(detail.summary)
            } else {
                entries.mapIndexed { index, entry ->
                    if (index > 0) {
                        Thread.sleep(REQUEST_SPACING_MS)
                    }

                    runCatching {
                        val detailDoc = if (urlsEquivalent(entry.url, landingUrl)) {
                            landingDoc
                        } else {
                            fetchDocument(entry.url)
                        }
                        val detail = parseDetail(
                            doc = detailDoc,
                            detailUrl = entry.url,
                            whqlOverride = entry.whqlCertified,
                            versionOverride = entry.version
                        )
                        detailCache[detail.summary.detailUrl] = detail
                        writeDetailCache(detail.summary.detailUrl, detail)
                        detail.summary
                    }.getOrElse {
                        previousByUrl[entry.url] ?: DriverSummary(
                            version = entry.version,
                            date = "Unknown",
                            size = "Unknown",
                            sha256 = "Unknown",
                            whqlCertified = entry.whqlCertified,
                            detailUrl = entry.url,
                            downloadUrl = "",
                            isCached = false
                        )
                    }
                }.distinctBy { it.version }
            }

            listCache[lang] = summaries
            writeListCache(lang, summaries)
            summaries
        } catch (t: Throwable) {
            if (cachedList.isNotEmpty()) {
                listCache[lang] = cachedList
                cachedList
            } else {
                throw t
            }
        }
    }

    suspend fun fetchDriverDetail(detailUrl: String): DriverDetail = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeIntelUrl(detailUrl)
        detailCache[normalizedUrl]?.let { return@withContext it }

        val cachedDetail = readDetailCache(normalizedUrl)
        if (cachedDetail != null) {
            detailCache[normalizedUrl] = cachedDetail
        }

        try {
            val doc = fetchDocument(normalizedUrl)
            val detail = parseDetail(doc, normalizedUrl)
            detailCache[detail.summary.detailUrl] = detail
            writeDetailCache(detail.summary.detailUrl, detail)
            detail
        } catch (t: Throwable) {
            cachedDetail ?: throw t
        }
    }

    fun clearCache() {
        detailCache.clear()
        listCache.clear()
    }

    private fun fetchDocument(url: String): Document {
        val candidates = buildList {
            add(normalizeIntelUrl(url))
            add(normalizeIntelUrl(toIntelComUrl(url)))
        }.filter { it.isNotBlank() }.distinct()

        var lastError: IOException? = null
        for (candidate in candidates) {
            repeat(MAX_FETCH_ATTEMPTS) { attempt ->
                try {
                    val response = client.newCall(buildRequest(candidate)).execute()
                    if (!response.isSuccessful) {
                        val code = response.code
                        val message = "HTTP error $code for $candidate"
                        response.close()
                        if (code in RETRYABLE_HTTP_CODES && attempt < MAX_FETCH_ATTEMPTS - 1) {
                            Thread.sleep(RETRY_DELAY_MS)
                            return@repeat
                        }
                        throw IOException(message)
                    }
                    val body = response.body?.string().orEmpty()
                    response.close()
                    return Jsoup.parse(body, candidate)
                } catch (io: IOException) {
                    lastError = io
                    if (attempt < MAX_FETCH_ATTEMPTS - 1) {
                        Thread.sleep(RETRY_DELAY_MS)
                    }
                }
            }
        }

        throw lastError ?: IOException("Failed to load $url")
    }

    private fun buildRequest(url: String): Request {
        val locale = localeForUrl(url)
        return Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", ACCEPT)
            .header("Accept-Language", locale.acceptLanguage)
            .header("Referer", locale.referer)
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .build()
    }

    private fun extractDetailEntries(doc: Document): List<DetailEntry> {
        return doc.select("select#version-driver-select option[value]")
            .mapNotNull { option ->
                val relativeUrl = option.attr("value").trim()
                if (relativeUrl.isBlank()) return@mapNotNull null

                val version = VERSION_REGEX.find(option.text())?.value ?: return@mapNotNull null
                DetailEntry(
                    url = normalizeIntelUrl(toAbsoluteUrl(doc, relativeUrl)),
                    version = version,
                    whqlCertified = isWhqlText(option.text())
                )
            }
            .distinctBy { it.url }
    }

    private fun parseDetail(
        doc: Document,
        detailUrl: String,
        whqlOverride: Boolean? = null,
        versionOverride: String? = null
    ): DriverDetail {
        val normalizedDetailUrl = normalizeIntelUrl(detailUrl)
        val optionText = extractCurrentOptionText(doc, normalizedDetailUrl)

        val version = doc.selectFirst("meta[name=DownloadVersion]")
            ?.attr("content")
            ?.trim()
            .orEmpty()
            .let { VERSION_REGEX.find(it)?.value.orEmpty() }
            .ifBlank { versionOverride.orEmpty() }
            .ifBlank { VERSION_REGEX.find(optionText)?.value.orEmpty() }
            .ifBlank { VERSION_REGEX.find(doc.text())?.value.orEmpty() }

        val date = doc.selectFirst(".dc-page-banner-actions-action-updated .dc-page-banner-actions-action__value")
            ?.text()
            ?.trim()
            .orEmpty()
            .ifBlank {
                doc.selectFirst("meta[name=LastUpdate]")
                    ?.attr("content")
                    ?.substringBefore(' ')
                    ?.trim()
                    .orEmpty()
            }

        val detailTexts = doc.select(".dc-page-available-downloads-hero-details-list__detail").eachText()
        val size = detailTexts.firstNotNullOfOrNull { SIZE_REGEX.find(it)?.value }.orEmpty()
        val sha256 = detailTexts.firstNotNullOfOrNull { SHA256_REGEX.find(it)?.value }.orEmpty()

        val downloadUrl = doc.selectFirst(
            ".dc-page-available-downloads-hero-button__cta[data-href], .available-download-button__cta[data-href], button[data-href]"
        )
            ?.attr("data-href")
            ?.trim()
            .orEmpty()
            .ifBlank {
                doc.select("a[href]")
                    .mapNotNull { anchor -> anchor.absUrl("href").takeIf { it.isNotBlank() } }
                    .firstOrNull { href ->
                        href.contains("download", ignoreCase = true) && href.endsWith(".exe", ignoreCase = true)
                    }
                    .orEmpty()
            }
            .let { raw -> if (raw.isBlank()) raw else normalizeIntelUrl(toAbsoluteUrl(doc, raw)) }

        val whqlCertified = whqlOverride ?: when {
            optionText.contains("Non-WHQL", ignoreCase = true) -> false
            isWhqlText(optionText) -> true
            doc.text().contains("Non-WHQL", ignoreCase = true) -> false
            else -> isWhqlText(doc.text())
        }

        val summary = DriverSummary(
            version = version.ifBlank { "Unknown" },
            date = date.ifBlank { "Unknown" },
            size = size.ifBlank { "Unknown" },
            sha256 = sha256.ifBlank { "Unknown" },
            whqlCertified = whqlCertified,
            detailUrl = normalizedDetailUrl,
            downloadUrl = downloadUrl,
            isCached = false
        )

        return DriverDetail(
            summary = summary,
            introductionHtml = extractSectionBody(doc.selectFirst(".dc-page-short-description")),
            availableDownloadsHtml = extractSectionBody(doc.selectFirst(".dc-page-available-downloads")),
            detailedDescriptionHtml = extractSectionBody(doc.selectFirst(".dc-page-detailed-description")),
            validProductsHtml = doc.selectFirst(".dc-page-detailed-other-valid-products-panel")?.outerHtml().orEmpty()
        )
    }

    private fun extractCurrentOptionText(doc: Document, detailUrl: String): String {
        val options = doc.select("select#version-driver-select option[value]")
        val matched = options.firstOrNull { option ->
            urlsEquivalent(normalizeIntelUrl(toAbsoluteUrl(doc, option.attr("value"))), detailUrl)
        }
        return matched?.text().orEmpty().ifBlank {
            doc.selectFirst("select#version-driver-select option[selected]")?.text().orEmpty()
        }
    }

    private fun extractSectionBody(container: Element?): String {
        if (container == null) return ""

        val builder = StringBuilder()
        container.children().forEach { child ->
            if (!child.tagName().matches(HEADING_TAG_REGEX)) {
                builder.append(child.outerHtml())
            }
        }
        return builder.toString()
    }

    private fun readListCache(lang: AppLang): List<DriverSummary> {
        val file = listCacheFile(lang)
        if (!file.exists()) return emptyList()

        return runCatching {
            val root = JSONObject(file.readText())
            if (isCacheExpired(root.optLong("timestamp", 0L))) return@runCatching emptyList()
            root.getJSONArray("drivers").toDriverSummaryList(fromCache = true)
        }.getOrDefault(emptyList())
    }

    private fun writeListCache(lang: AppLang, drivers: List<DriverSummary>) {
        if (drivers.isEmpty()) return

        runCatching {
            val root = JSONObject()
                .put("lang", lang.code)
                .put("timestamp", System.currentTimeMillis())
                .put("drivers", JSONArray().apply {
                    drivers.forEach { put(it.copy(isCached = false).toJson()) }
                })
            listCacheFile(lang).writeText(root.toString())
        }
    }

    private fun readDetailCache(detailUrl: String): DriverDetail? {
        val file = detailCacheFile(detailUrl)
        if (!file.exists()) return null

        return runCatching {
            val root = JSONObject(file.readText())
            if (isCacheExpired(root.optLong("timestamp", 0L))) return@runCatching null
            val payload = root.optJSONObject("detail") ?: return@runCatching null
            payload.toDriverDetail(fromCache = true)
        }.getOrNull()
    }

    private fun writeDetailCache(detailUrl: String, detail: DriverDetail) {
        runCatching {
            val root = JSONObject()
                .put("timestamp", System.currentTimeMillis())
                .put("detail", detail.copy(summary = detail.summary.copy(isCached = false)).toJson())
            detailCacheFile(detailUrl).writeText(root.toString())
        }
    }

    private fun isCacheExpired(timestamp: Long): Boolean {
        if (timestamp <= 0L) return true
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }

    private fun listCacheFile(lang: AppLang): File = File(cacheDirectory, "list_${lang.code}.json")

    private fun detailCacheFile(detailUrl: String): File = File(cacheDirectory, "detail_${hashKey(detailUrl)}.json")

    private fun hashKey(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun toAbsoluteUrl(doc: Document, value: String): String {
        if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            return value
        }
        return runCatching { URI(doc.baseUri()).resolve(value).toString() }.getOrDefault(value)
    }

    private fun urlsEquivalent(left: String, right: String): Boolean {
        return normalizeUrl(left) == normalizeUrl(right)
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().removeSuffix("/")
    }

    private fun normalizeIntelUrl(url: String): String {
        if (url.isBlank()) return url
        return normalizeUrl(toIntelComUrl(url))
    }

    private fun toIntelComUrl(url: String): String {
        if (url.isBlank()) return url
        return url
            .replace("https://www.intel.cn/", HOME_URL)
            .replace("https://www.intel.com.tw/", HOME_URL)
            .replace("https://intel.cn/", HOME_URL)
            .replace("https://intel.com.tw/", HOME_URL)
    }

    private fun localeForUrl(url: String): LocaleHeaders = when {
        url.contains("/content/www/cn/zh/") -> LocaleHeaders(
            acceptLanguage = "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
            referer = "https://www.intel.com/content/www/cn/zh/"
        )
        url.contains("/content/www/tw/zh/") -> LocaleHeaders(
            acceptLanguage = "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7",
            referer = "https://www.intel.com/content/www/tw/zh/"
        )
        else -> LocaleHeaders(
            acceptLanguage = "en-US,en;q=0.9",
            referer = "https://www.intel.com/content/www/us/en/"
        )
    }

    private fun isWhqlText(text: String): Boolean {
        return text.contains("WHQL", ignoreCase = true) && !text.contains("Non-WHQL", ignoreCase = true)
    }

    private fun DriverSummary.toJson(): JSONObject = JSONObject()
        .put("version", version)
        .put("date", date)
        .put("size", size)
        .put("sha256", sha256)
        .put("whqlCertified", whqlCertified)
        .put("detailUrl", detailUrl)
        .put("downloadUrl", downloadUrl)

    private fun JSONObject.toDriverSummary(fromCache: Boolean): DriverSummary = DriverSummary(
        version = optString("version", "Unknown"),
        date = optString("date", "Unknown"),
        size = optString("size", "Unknown"),
        sha256 = optString("sha256", "Unknown"),
        whqlCertified = optBoolean("whqlCertified", false),
        detailUrl = optString("detailUrl", ""),
        downloadUrl = optString("downloadUrl", ""),
        isCached = fromCache
    )

    private fun JSONArray.toDriverSummaryList(fromCache: Boolean): List<DriverSummary> = buildList {
        for (index in 0 until length()) {
            val obj = optJSONObject(index) ?: continue
            add(obj.toDriverSummary(fromCache = fromCache))
        }
    }

    private fun DriverDetail.toJson(): JSONObject = JSONObject()
        .put("summary", summary.toJson())
        .put("introductionHtml", introductionHtml)
        .put("availableDownloadsHtml", availableDownloadsHtml)
        .put("detailedDescriptionHtml", detailedDescriptionHtml)
        .put("validProductsHtml", validProductsHtml)

    private fun JSONObject.toDriverDetail(fromCache: Boolean): DriverDetail {
        val summaryObject = optJSONObject("summary") ?: JSONObject()
        return DriverDetail(
            summary = summaryObject.toDriverSummary(fromCache = fromCache),
            introductionHtml = optString("introductionHtml", ""),
            availableDownloadsHtml = optString("availableDownloadsHtml", ""),
            detailedDescriptionHtml = optString("detailedDescriptionHtml", ""),
            validProductsHtml = optString("validProductsHtml", "")
        )
    }

    companion object {
        private const val HOME_URL = "https://www.intel.com/"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
        private const val MAX_VERSIONS = 8
        private const val MAX_FETCH_ATTEMPTS = 2
        private const val RETRY_DELAY_MS = 1200L
        private const val REQUEST_SPACING_MS = 350L
        private const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L

        private val RETRYABLE_HTTP_CODES = setOf(403, 429)
        private val HEADING_TAG_REGEX = Regex("h[1-6]", RegexOption.IGNORE_CASE)
        private val VERSION_REGEX = Regex("\\b\\d+\\.\\d+\\.\\d+\\.\\d+\\b")
        private val SIZE_REGEX = Regex("\\b\\d+(?:,\\d{3})*(?:\\.\\d+)?\\s*(?:MB|GB)\\b", RegexOption.IGNORE_CASE)
        private val SHA256_REGEX = Regex("\\b[A-Fa-f0-9]{64}\\b")
    }
}

enum class AppLang(val code: String, val displayName: String, val baseUrl: String) {
    ZH_CN("zh_CN", "\u7b80\u4f53\u4e2d\u6587", "https://www.intel.com/content/www/cn/zh/download/785597/intel-arc-graphics-windows.html"),
    ZH_TW("zh_TW", "\u7e41\u9ad4\u4e2d\u6587", "https://www.intel.com/content/www/tw/zh/download/785597/intel-arc-graphics-windows.html"),
    EN("en", "English", "https://www.intel.com/content/www/us/en/download/785597/intel-arc-graphics-windows.html")
}

private data class DetailEntry(
    val url: String,
    val version: String,
    val whqlCertified: Boolean
)

private data class LocaleHeaders(
    val acceptLanguage: String,
    val referer: String
)

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
