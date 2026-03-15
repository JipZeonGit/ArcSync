package com.jipzeongit.arcsync.util

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

@Composable
fun HtmlText(html: String, onOpenUrl: (String) -> Unit, baseUrl: String? = null) {
    val annotated = remember(html, baseUrl) { htmlToAnnotatedString(html, baseUrl) }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { onOpenUrl(it.item) }
        }
    )
}

fun htmlToAnnotatedString(html: String, baseUrl: String? = null): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")

    val doc = Jsoup.parseBodyFragment(html)
    val builder = AnnotatedString.Builder()

    doc.body().childNodes().forEach { node ->
        appendNode(builder, node, 0, baseUrl)
    }

    return builder.toAnnotatedString()
}

private fun appendNode(builder: AnnotatedString.Builder, node: Node, indent: Int, baseUrl: String?) {
    when (node) {
        is TextNode -> {
            val text = node.text().replace("\u00A0", " ")
            builder.append(text)
        }
        else -> {
            val name = node.nodeName().lowercase()
            when (name) {
                "br" -> builder.append("\n")
                "b", "strong" -> {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        node.childNodes().forEach { child -> appendNode(this, child, indent, baseUrl) }
                    }
                }
                "ul", "ol" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent + 1, baseUrl) }
                    builder.append("\n")
                }
                "li" -> {
                    builder.append("\n")
                    builder.append("  ".repeat(indent))
                    builder.append("• ")
                    node.childNodes().forEach { child -> appendNode(builder, child, indent, baseUrl) }
                }
                "a" -> {
                    val href = node.attr("href")
                    val resolved = resolveHref(baseUrl, href)
                    val start = builder.length
                    node.childNodes().forEach { child -> appendNode(builder, child, indent, baseUrl) }
                    val end = builder.length
                    if (resolved.isNotBlank()) {
                        builder.addStringAnnotation("URL", resolved, start, end)
                        builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    }
                }
                "p", "div", "section" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent, baseUrl) }
                    builder.append("\n\n")
                }
                "table" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent, baseUrl) }
                    builder.append("\n")
                }
                "tr" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent, baseUrl) }
                    builder.append("\n")
                }
                "td", "th" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent, baseUrl) }
                    builder.append(" | ")
                }
                else -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent, baseUrl) }
                }
            }
        }
    }
}

private fun resolveHref(baseUrl: String?, href: String): String {
    if (href.isBlank()) return ""
    if (href.startsWith("http://") || href.startsWith("https://")) return href
    if (baseUrl.isNullOrBlank()) return href
    return try {
        URI(baseUrl).resolve(href).toString()
    } catch (_: Exception) {
        href
    }
}
