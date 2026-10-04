package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.CampusBlue
import com.example.ui.theme.CampusBlueDark
import com.example.ui.theme.CampusOrange
import com.example.ui.theme.DarkCodeBackground
import com.example.ui.theme.DarkCodeBorder
import com.example.ui.theme.LightCodeBackground
import com.example.ui.theme.LightCodeBorder

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isDarkMode: Boolean,
    isSpeakingThis: Boolean,
    onSpeakClick: () -> Unit,
    onStopSpeakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.isUser

    val userBubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 4.dp
    )

    val botBubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 4.dp,
        bottomEnd = 16.dp
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Assistant avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = if (isDarkMode) Color(0xFF2D3148) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎓",
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message body
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = if (isUser) userBubbleShape else botBubbleShape,
                color = if (isUser) {
                    if (isDarkMode) CampusBlueDark else Color(0xFFEFF6FF)
                } else {
                    if (isDarkMode) Color(0xFF1A1D27) else Color(0xFFF1F5F9)
                },
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isUser) CampusBlue else (if (isDarkMode) Color(0xFF2D3148) else Color(0xFFCBD5E1)),
                        shape = if (isUser) userBubbleShape else botBubbleShape
                    )
                    .testTag(if (isUser) "user_message_bubble" else "bot_message_bubble")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (!isUser && message.modelName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = message.modelName,
                                fontSize = 11.sp,
                                color = CampusOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (message.personaTitle.isNotBlank()) {
                                Text(
                                    text = " • ${message.personaTitle}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Render text and code blocks
                    RenderFormattedContent(
                        content = message.content,
                        isDarkMode = isDarkMode,
                        isUser = isUser
                    )
                }
            }

            // Message actions for assistant
            if (!isUser) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("CampusMind Answer", message.content))
                            Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("copy_response_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isSpeakingThis) onStopSpeakClick() else onSpeakClick()
                        },
                        modifier = Modifier.size(32.dp).testTag("tts_speak_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeakingThis) "Stop Audio" else "Read Aloud",
                            tint = if (isSpeakingThis) CampusOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color = CampusOrange, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun RenderFormattedContent(
    content: String,
    isDarkMode: Boolean,
    isUser: Boolean
) {
    val context = LocalContext.current
    val textColor = if (isUser) {
        if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E3A5F)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // Split markdown blocks: check for fenced code blocks ```lang ... ```
    val regex = Regex("```([a-zA-Z0-9_]*)\\n([\\s\\S]*?)```")
    val matches = regex.findAll(content).toList()

    if (matches.isEmpty()) {
        Text(
            text = content,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
        return
    }

    var lastIndex = 0
    Column {
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            // Text before the code block
            if (start > lastIndex) {
                val preText = content.substring(lastIndex, start).trim()
                if (preText.isNotEmpty()) {
                    Text(
                        text = preText,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            val language = match.groupValues[1].ifBlank { "code" }
            val codeContent = match.groupValues[2].trimEnd()

            // Fenced code box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(
                        color = if (isDarkMode) DarkCodeBackground else LightCodeBackground,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDarkMode) DarkCodeBorder else LightCodeBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // Header bar with language tag and copy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CampusOrange,
                        fontFamily = FontFamily.Monospace
                    )

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code snippet", codeContent))
                            Toast.makeText(context, "Copied code snippet", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Code text
                Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    Text(
                        text = codeContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = if (isDarkMode) Color(0xFFD4D4D4) else Color(0xFF1E293B)
                    )
                }
            }

            lastIndex = end
        }

        // Remaining text after last code block
        if (lastIndex < content.length) {
            val postText = content.substring(lastIndex).trim()
            if (postText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = postText,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
