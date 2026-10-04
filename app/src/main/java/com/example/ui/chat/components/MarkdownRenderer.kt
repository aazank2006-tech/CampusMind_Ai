package com.example.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier
) {
    // Split into code blocks vs non-code blocks
    val codeBlockRegex = Regex("```([a-zA-Z0-9_]*)\\n([\\s\\S]*?)```")
    val matches = codeBlockRegex.findAll(content).toList()

    if (matches.isEmpty()) {
        RenderMarkdownBlocks(text = content, modifier = modifier)
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        var cursor = 0
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                val pre = content.substring(cursor, start).trim()
                if (pre.isNotEmpty()) {
                    RenderMarkdownBlocks(text = pre)
                }
            }

            val lang = match.groupValues[1]
            val code = match.groupValues[2].trimEnd()
            CodeBlockView(language = lang, code = code)

            cursor = end
        }

        if (cursor < content.length) {
            val post = content.substring(cursor).trim()
            if (post.isNotEmpty()) {
                RenderMarkdownBlocks(text = post)
            }
        }
    }
}

@Composable
private fun RenderMarkdownBlocks(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")
    var i = 0

    Column(modifier = modifier.fillMaxWidth()) {
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Table Detection
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                RenderMarkdownTable(tableLines)
                Spacer(modifier = Modifier.height(8.dp))
                continue
            }

            // 2. Headings
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ").trim()),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ").trim()),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ").trim()),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                // 3. Blockquote
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(trimmed.removePrefix("> ").trim()),
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 4. Bullet lists
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(trimmed.substring(2).trim()),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // 5. Standard paragraph line
                trimmed.isNotEmpty() -> {
                    Text(
                        text = parseInlineMarkdown(trimmed),
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            i++
        }
    }
}

/**
 * Renders horizontally scrollable Markdown table
 */
@Composable
private fun RenderMarkdownTable(lines: List<String>) {
    if (lines.isEmpty()) return

    val cleanRows = lines.map { line ->
        line.split("|")
            .map { it.trim() }
            .filterIndexed { index, _ -> index != 0 && index != line.split("|").size - 1 }
    }.filterNot { row -> row.all { cell -> cell.all { it == '-' || it == ':' || it == ' ' } } }

    if (cleanRows.isEmpty()) return

    val header = cleanRows.first()
    val dataRows = cleanRows.drop(1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .horizontalScroll(rememberScrollState())
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                header.forEach { colTitle ->
                    Text(
                        text = parseInlineMarkdown(colTitle),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            // Data Rows
            dataRows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    row.forEachIndexed { colIndex, cellValue ->
                        Text(
                            text = parseInlineMarkdown(cellValue),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .width(130.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Handles inline bold (**text**), italic (*text*), and inline code (`code`)
 */
fun parseInlineMarkdown(input: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < input.length) {
            // Inline code `...`
            if (input[i] == '`') {
                val next = input.indexOf('`', i + 1)
                if (next != -1) {
                    val codeContent = input.substring(i + 1, next)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            background = Color(0x334285F4),
                            color = Color(0xFFA8C7FA)
                        )
                    )
                    append(" $codeContent ")
                    pop()
                    i = next + 1
                    continue
                }
            }

            // Bold **...**
            if (input.startsWith("**", i)) {
                val next = input.indexOf("**", i + 2)
                if (next != -1) {
                    val boldContent = input.substring(i + 2, next)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldContent)
                    pop()
                    i = next + 2
                    continue
                }
            }

            // Italic *...*
            if (input[i] == '*' && (i + 1 < input.length && input[i + 1] != '*')) {
                val next = input.indexOf('*', i + 1)
                if (next != -1) {
                    val italicContent = input.substring(i + 1, next)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicContent)
                    pop()
                    i = next + 1
                    continue
                }
            }

            append(input[i])
            i++
        }
    }
}
