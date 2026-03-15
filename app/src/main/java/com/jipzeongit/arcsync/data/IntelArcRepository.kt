package com.jipzeongit.arcsync.data

import java.io.IOException
import java.net.URI
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

        val detailEntries = extractDetailEntries(doc)
        val summaries = mutableListOf<DriverSummary>()

        if (detailEntries.isEmpty()) {
            val detail = parseDetail(doc, url)
            detailCache[url] = detail
            summaries += detail.summary
        } else {
            for (entry in detailEntries) {
                seedCookies(entry.url)
                val detailDoc = fetchDocument(entry.url)
                val detail = parseDetail(detailDoc, entry.url, entry.whqlCertified)
                detailCache[entry.url] = detail
                summaries += detail.summary
            }
        }

        val ordered = summaries.distinctBy { it.version }
        listCache += ordered
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

    fun clearCache() {
        detailCache.clear()
        listCache.clear()
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

    private fun parseDetail(doc: Document, detailUrl: String, whqlOverride: Boolean? = null): DriverDetail {
        val jsonDetail = extractJsonDetail(doc)
        val metaVersion = extractMetaContent(doc, listOf("DownloadVersion"))
        val metaDate = extractMetaDate(doc)
        val metaDownloadUrl = extractMetaContent(doc, listOf("RecommendedDownloadUrl"))
        val version = sanitizeVersion(
            metaVersion
                .ifBlank { jsonDetail?.version.orEmpty() }
                ?: extractValueByLabels(
                    doc,
                    listOf("Version", "版本", "Driver Version", "驱动程序版本"),
                    maxLen = 40,
                    requireMatch = VERSION_REGEX
                ).ifBlank { extractVersionFromText(doc) }
        )
        val date = sanitizeValue(
            metaDate
                .ifBlank { jsonDetail?.date.orEmpty() }
                ?: extractValueByLabels(
                    doc,
                    listOf("Date", "日期", "Release Date", "发布日期"),
                    maxLen = 40,
                    requireMatch = DATE_VALUE_REGEX
                ).ifBlank { extractDateFromText(doc) }
        )
        val size = sanitizeValue(
            jsonDetail?.size
                ?: extractValueByLabels(
                    doc,
                    listOf("Size", "大小", "File Size", "文件大小"),
                    maxLen = 40,
                    requireMatch = SIZE_REGEX
                ).ifBlank { extractSizeFromText(doc) }
        )
        val sha256 = sanitizeValue(
            jsonDetail?.sha256
                ?: extractValueByLabels(
                    doc,
                    listOf("SHA256", "SHA-256", "SHA 256", "Checksum", "校验码"),
                    maxLen = 80,
                    requireMatch = SHA256_REGEX
                ).ifBlank { extractSha256FromText(doc) }
        )
        val whqlCertified = whqlOverride ?: detectWhql(doc)

        val downloadLink = jsonDetail?.downloadUrl ?: doc.select("a[href]")
            .firstOrNull { el ->
                val text = el.text().lowercase(Locale.getDefault())
                val href = el.absUrl("href").lowercase(Locale.getDefault())
                text.contains("download") || text.contains("下载") || href.contains("download")
            }
            ?.absUrl("href")
            .orEmpty()
            .ifBlank { metaDownloadUrl }
            .ifBlank { extractDownloadUrl(doc) }
        val summary = DriverSummary(
            version = if (version.isNotBlank()) version else "Unknown",
            date = if (date.isNotBlank()) date else "Unknown",
            size = if (size.isNotBlank()) size else "Unknown",
            sha256 = if (sha256.isNotBlank()) sha256 else "Unknown",
            whqlCertified = whqlCertified,
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

    private fun extractDetailEntries(doc: Document): List<DetailEntry> {
        val optionEntries = doc.select("select#version-driver-select option[value], select[name=version-driver-select] option[value], select option[value]")
            .mapNotNull { option ->
                val value = option.attr("value")?.trim().orEmpty()
                if (value.isBlank()) return@mapNotNull null
                if (!value.contains("/download/") || !value.contains("intel-arc-graphics-windows")) return@mapNotNull null
                val whql = option.text().contains("WHQL", ignoreCase = true)
                DetailEntry(toAbsoluteUrl(doc, value), whql)
            }
            .distinctBy { it.url }

        if (optionEntries.isNotEmpty()) return optionEntries.take(MAX_VERSIONS)

        val urls = doc.select("a[href]")
            .mapNotNull { it.absUrl("href") }
            .filter { href ->
                href.contains("intel-arc-graphics-windows") && href.contains("/download/")
            }
            .distinct()

        if (urls.isNotEmpty()) return urls.take(MAX_VERSIONS).map { DetailEntry(it, false) }

        return doc.select("a[href]")
            .mapNotNull { it.absUrl("href") }
            .filter { href -> href.contains("/download/") && href.contains("intel") }
            .distinct()
            .take(MAX_VERSIONS)
            .map { DetailEntry(it, false) }
    }

    private fun extractValueByLabels(
        doc: Document,
        labels: List<String>,
        maxLen: Int? = null,
        requireMatch: Regex? = null
    ): String {
        val labelSet = labels.map { it.lowercase(Locale.getDefault()) }.toSet()

        // Try definition list: dt -> dd
        doc.select("dt").forEach { dt ->
            val text = dt.text().trim().lowercase(Locale.getDefault())
            if (text in labelSet) {
                val dd = dt.nextElementSibling()
                if (dd != null && dd.tagName().equals("dd", ignoreCase = true)) {
                    val v = dd.text().trim()
                    if (isAcceptableValue(v, maxLen, requireMatch)) return v
                }
            }
        }

        // Try table rows: th -> td
        doc.select("tr").forEach { tr ->
            val th = tr.selectFirst("th")
            val td = tr.selectFirst("td")
            if (th != null && td != null) {
                val text = th.text().trim().lowercase(Locale.getDefault())
                if (text in labelSet) {
                    val v = td.text().trim()
                    if (isAcceptableValue(v, maxLen, requireMatch)) return v
                }
            }
        }

        // Try strong/b label inside a block
        doc.select("p,div,li").forEach { block ->
            val strong = block.selectFirst("strong,b")
            if (strong != null) {
                val text = strong.text().trim().lowercase(Locale.getDefault())
                if (text in labelSet) {
                    val v = block.ownText().trim()
                    if (isAcceptableValue(v, maxLen, requireMatch)) return v
                }
            }
        }

        // Try list item "Label: value"
        doc.select("li").forEach { li ->
            val text = li.text().trim()
            val lower = text.lowercase(Locale.getDefault())
            if (labelSet.any { label -> lower.startsWith(label) }) {
                val v = when {
                    text.contains("：") -> text.substringAfter("：").trim()
                    text.contains(":") -> text.substringAfter(":").trim()
                    else -> ""
                }
                if (isAcceptableValue(v, maxLen, requireMatch)) return v
            }
        }



        // Try label + span pattern (Intel banner blocks)
        doc.select("label").forEach { label ->
            val text = label.text().trim().lowercase(Locale.getDefault())
            if (text in labelSet) {
                val parent = label.parent()
                val valueEl = parent?.selectFirst(".dc-page-banner-actions-action__value, span")
                    ?: label.nextElementSibling()
                val v = valueEl?.text()?.trim().orEmpty()
                if (isAcceptableValue(v, maxLen, requireMatch)) return v
            }
        }

        return ""
    }

    private fun extractVersionFromText(doc: Document): String {
        val h1 = doc.selectFirst("h1")?.text()?.trim().orEmpty()
        val m1 = VERSION_REGEX.find(h1)
        if (m1 != null) return m1.value

        val text = doc.text()
        val m2 = VERSION_REGEX.find(text)
        return m2?.value.orEmpty()
    }

    private fun extractDateFromText(doc: Document): String {
        val text = doc.text()
        val m = DATE_REGEX.find(text)
        return m?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun extractSizeFromText(doc: Document): String {
        val text = doc.text()
        val m = SIZE_REGEX.find(text)
        return m?.value.orEmpty()
    }

    private fun extractSha256FromText(doc: Document): String {
        val text = doc.text()
        val m = SHA256_REGEX.find(text)
        return m?.value.orEmpty()
    }

    private fun detectWhql(doc: Document): Boolean {
        val text = doc.text()
        if (text.contains("Non-WHQL", ignoreCase = true)) return false
        if (text.contains("WHQL", ignoreCase = true)) return true
        return false
    }
    private fun sanitizeVersion(value: String): String {
        val v = value.trim()
        if (v.isBlank()) return v
        val m = VERSION_REGEX.find(v)
        return m?.value ?: sanitizeValue(v)
    }

    private fun sanitizeValue(value: String, maxLen: Int = 80): String {
        val clean = value.replace("\u00A0", " ").trim()
        if (clean.length <= maxLen) return clean
        return clean.take(maxLen)
    }

    private fun isAcceptableValue(value: String, maxLen: Int?, requireMatch: Regex?): Boolean {
        val v = value.trim()
        if (v.isBlank()) return false
        if (maxLen != null && v.length > maxLen) return false
        if (requireMatch != null && !requireMatch.containsMatchIn(v)) return false
        return true
    }

    private fun extractJsonDetail(doc: Document): JsonDetail? {
        for (script in doc.select("script")) {
            val data = script.data().ifBlank { script.html() }
            if (data.length < 50) continue
            if (!data.contains("version", ignoreCase = true) &&
                !data.contains("sha256", ignoreCase = true) &&
                !data.contains("checksum", ignoreCase = true)
            ) continue

            val versionRaw = JSON_VERSION_REGEX.find(data)?.groupValues?.getOrNull(1)
            val version = versionRaw?.let { sanitizeVersion(it) }?.takeIf { it.isNotBlank() }

            val dateRaw = JSON_DATE_REGEX.find(data)?.groupValues?.getOrNull(1)
            val date = dateRaw?.let { sanitizeValue(it, 40) }?.takeIf { it.isNotBlank() }

            val sizeRaw = JSON_SIZE_REGEX.find(data)?.groupValues?.getOrNull(1)
            val size = sizeRaw?.let { normalizeSize(it) }?.takeIf { it.isNotBlank() }



            val sha = JSON_SHA256_REGEX.find(data)?.groupValues?.getOrNull(1)
            val sha256 = sha?.let { it.trim() }?.takeIf { it.isNotBlank() }

            val download = JSON_URL_REGEX.find(data)?.groupValues?.getOrNull(1)
                ?.takeIf { it.contains("download", ignoreCase = true) }
                ?.let { it.trim() }

            if (version != null || date != null || size != null || sha256 != null || download != null) {
                return JsonDetail(version, date, size, sha256, download)
            }
        }
        return null
    }

    private fun extractMetaContent(doc: Document, names: List<String>): String {
        for (name in names) {
            val v = doc.selectFirst("meta[name=$name]")?.attr("content")?.trim().orEmpty()
            if (v.isNotBlank()) return v
        }
        return ""
    }

    private fun extractMetaDate(doc: Document): String {
        val raw = extractMetaContent(doc, listOf("LastUpdate", "lastModifieddate"))
        val match = DATE_VALUE_REGEX.find(raw)
        return match?.value.orEmpty()
    }


    private fun toAbsoluteUrl(doc: Document, value: String): String {
        if (value.startsWith("http://", true) || value.startsWith("https://", true)) return value
        return runCatching { URI(doc.baseUri()).resolve(value).toString() }.getOrDefault(value)
    }

    private fun extractDownloadUrl(doc: Document): String {
        val buttonUrl = doc.select("button[data-href]")
            .mapNotNull { it.attr("data-href")?.trim() }
            .firstOrNull { it.contains("download", ignoreCase = true) || it.endsWith(".exe", true) }
            .orEmpty()

        if (buttonUrl.isNotBlank()) {
            return if (buttonUrl.startsWith("http", ignoreCase = true)) {
                buttonUrl
            } else {
                runCatching { URI(doc.baseUri()).resolve(buttonUrl).toString() }.getOrDefault(buttonUrl)
            }
        }

        val anchorUrl = doc.select("a[href]")
            .mapNotNull { it.absUrl("href") }
            .firstOrNull { it.contains("download", ignoreCase = true) }
            .orEmpty()

        return anchorUrl
    }


    private fun normalizeSize(raw: String): String {
        val v = raw.trim()
        if (v.isBlank()) return v
        if (SIZE_REGEX.containsMatchIn(v)) return v
        val digits = v.filter { it.isDigit() }
        val bytes = digits.toLongOrNull()
        if (bytes != null && bytes > 0) {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }
        return v
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
        private const val MAX_VERSIONS = 8

        private val INTRO_LABELS = listOf("Introduction", "介绍")
        private val DOWNLOADS_LABELS = listOf("Available Downloads", "可供下载")
        private val DETAIL_LABELS = listOf("Detailed Description", "详细说明")
        private val VALID_LABELS = listOf("This download is valid for the product(s) listed below", "此下载对下面列出的产品有效")

        private val VERSION_REGEX = Regex("\\b\\d+\\.\\d+\\.\\d+\\.\\d+\\b")
        private val DATE_VALUE_REGEX = Regex(
            "\\b([A-Za-z]{3,9}\\s+\\d{1,2},\\s+\\d{4}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}/\\d{1,2}/\\d{4})\\b"
        )
        private val DATE_REGEX = Regex("(?:Date|日期)\\s*[:：]?\\s*([A-Za-z]{3,9}\\s+\\d{1,2},\\s+\\d{4}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}/\\d{1,2}/\\d{4})")
        private val SIZE_REGEX = Regex("\\b\\d+(?:\\.\\d+)?\\s*(?:MB|GB)\\b", RegexOption.IGNORE_CASE)
        private val SHA256_REGEX = Regex("\\b[A-Fa-f0-9]{64}\\b")

        private val JSON_VERSION_REGEX = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        private val JSON_DATE_REGEX = Regex("\"(?:releaseDate|date)\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        private val JSON_SIZE_REGEX = Regex("\"(?:fileSize|size)\"\\s*:\\s*\"?([^\"]+?)\"?(?:,|\\})", RegexOption.IGNORE_CASE)
        private val JSON_SHA256_REGEX = Regex("\"sha256\"\\s*:\\s*\"([A-Fa-f0-9]{64})\"", RegexOption.IGNORE_CASE)
        private val JSON_URL_REGEX = Regex("\"(https?://[^\"]+)\"", RegexOption.IGNORE_CASE)
    }
}

private data class DetailEntry(
    val url: String,
    val whqlCertified: Boolean
)
private data class JsonDetail(
    val version: String?,
    val date: String?,
    val size: String?,
    val sha256: String?,
    val downloadUrl: String?
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


















