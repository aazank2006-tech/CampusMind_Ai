package com.example.ui.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeBlockView(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val codeBg = Color(0xFF1E1F20)
    val codeBorder = Color(0xFF333537)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(codeBg, RoundedCornerShape(14.dp))
            .border(1.dp, codeBorder, RoundedCornerShape(14.dp))
            .testTag("code_block_${language.lowercase()}")
    ) {
        // Code Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "CODE" }.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA8C7FA),
                fontFamily = FontFamily.Monospace
            )

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Code snippet", code))
                    Toast.makeText(context, "Copied code snippet", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("copy_code_block_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = Color(0xFFC4C7C5),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Horizontal scrollable code body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            Text(
                text = highlightSyntax(code),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Basic syntax highlighting for keywords, numbers, strings, and comments.
 */
private fun highlightSyntax(code: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = code.split("\n")
        lines.forEachIndexed { index, line ->
            var i = 0
            while (i < line.length) {
                // Comments (# or //)
                if (line.startsWith("#", i) || line.startsWith("//", i)) {
                    pushStyle(SpanStyle(color = Color(0xFF8F9397), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    append(line.substring(i))
                    pop()
                    break
                }
                // Strings ("..." or '...')
                else if (line[i] == '"' || line[i] == '\'') {
                    val quote = line[i]
                    val nextQuote = line.indexOf(quote, i + 1)
                    if (nextQuote != -1) {
                        pushStyle(SpanStyle(color = Color(0xFFC3E88D)))
                        append(line.substring(i, nextQuote + 1))
                        pop()
                        i = nextQuote + 1
                    } else {
                        pushStyle(SpanStyle(color = Color(0xFFC3E88D)))
                        append(line.substring(i))
                        pop()
                        break
                    }
                } else {
                    // Extract word
                    var j = i
                    while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) {
                        j++
                    }
                    if (j > i) {
                        val word = line.substring(i, j)
                        when (word) {
                            "def", "class", "fun", "val", "var", "return", "if", "else", "for", "while", "import", "from", "None", "True", "False", "in", "is", "not", "and", "or" -> {
                                pushStyle(SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.SemiBold))
                                append(word)
                                pop()
                            }
                            "int", "str", "float", "bool", "list", "dict", "set", "String", "Int", "Boolean", "List" -> {
                                pushStyle(SpanStyle(color = Color(0xFF79C0FF)))
                                append(word)
                                pop()
                            }
                            else -> {
                                pushStyle(SpanStyle(color = Color(0xFFE6EDF3)))
                                append(word)
                                pop()
                            }
                        }
                        i = j
                    } else {
                        pushStyle(SpanStyle(color = Color(0xFFE6EDF3)))
                        append(line[i].toString())
                        pop()
                        i++
                    }
                }
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}
