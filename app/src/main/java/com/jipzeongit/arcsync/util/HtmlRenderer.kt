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
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

@Composable
fun HtmlText(html: String, onOpenUrl: (String) -> Unit) {
    val annotated = remember(html) { htmlToAnnotatedString(html) }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { onOpenUrl(it.item) }
        }
    )
}

fun htmlToAnnotatedString(html: String): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")

    val doc = Jsoup.parseBodyFragment(html)
    val builder = AnnotatedString.Builder()

    doc.body().childNodes().forEach { node ->
        appendNode(builder, node, 0)
    }

    return builder.toAnnotatedString()
}

private fun appendNode(builder: AnnotatedString.Builder, node: Node, indent: Int) {
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
                        node.childNodes().forEach { child -> appendNode(this, child, indent) }
                    }
                }
                "ul" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent + 1) }
                }
                "li" -> {
                    builder.append("\n")
                    builder.append("  ".repeat(indent))
                    builder.append("• ")
                    node.childNodes().forEach { child -> appendNode(builder, child, indent) }
                }
                "a" -> {
                    val href = node.attr("href")
                    val start = builder.length
                    node.childNodes().forEach { child -> appendNode(builder, child, indent) }
                    val end = builder.length
                    if (href.isNotBlank()) {
                        builder.addStringAnnotation("URL", href, start, end)
                        builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    }
                }
                "p", "div", "section" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent) }
                    builder.append("\n\n")
                }
                "table" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent) }
                    builder.append("\n")
                }
                "tr" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent) }
                    builder.append("\n")
                }
                "td", "th" -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent) }
                    builder.append(" | ")
                }
                else -> {
                    node.childNodes().forEach { child -> appendNode(builder, child, indent) }
                }
            }
        }
    }
}
