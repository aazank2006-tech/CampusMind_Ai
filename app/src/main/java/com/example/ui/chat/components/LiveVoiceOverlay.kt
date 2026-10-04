package com.example.ui.chat.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LiveVoiceState {
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun LiveVoiceOverlay(
    state: LiveVoiceState,
    isMuted: Boolean,
    userTranscript: String,
    aiTranscript: String,
    onToggleMute: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    // Pulsing orb animation
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF131314))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("live_voice_overlay")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x334285F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎓", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CampusMind Live",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .testTag("live_voice_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Live mode",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center: Pulsing Gemini Gradient Orb
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Ripple Halo
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x444285F4),
                                Color(0x339B72CB),
                                Color(0x11D96570),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width / 1.6f
                        )
                    )
                }

                // Inner Main Gradient Orb
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(if (state == LiveVoiceState.LISTENING) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4285F4),
                                    Color(0xFF9B72CB),
                                    Color(0xFFD96570)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(250f, 250f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (state) {
                            LiveVoiceState.LISTENING -> "🎙️"
                            LiveVoiceState.THINKING -> "✨"
                            LiveVoiceState.SPEAKING -> "🔊"
                        },
                        fontSize = 38.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // State Pill Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (state) {
                    LiveVoiceState.LISTENING -> Color(0x224285F4)
                    LiveVoiceState.THINKING -> Color(0x229B72CB)
                    LiveVoiceState.SPEAKING -> Color(0x22D96570)
                },
                modifier = Modifier
                    .border(
                        1.dp,
                        when (state) {
                            LiveVoiceState.LISTENING -> Color(0x664285F4)
                            LiveVoiceState.THINKING -> Color(0x669B72CB)
                            LiveVoiceState.SPEAKING -> Color(0x66D96570)
                        },
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Text(
                    text = when (state) {
                        LiveVoiceState.LISTENING -> if (isMuted) "Microphone muted" else "Listening to you..."
                        LiveVoiceState.THINKING -> "Thinking..."
                        LiveVoiceState.SPEAKING -> "CampusMind is speaking..."
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Transcript Display
            val transcript = if (state == LiveVoiceState.SPEAKING) aiTranscript else userTranscript
            if (transcript.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x26FFFFFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = transcript,
                        fontSize = 14.sp,
                        color = Color(0xFFE3E3E3),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Bottom Controls Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Unmute Button
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) Color(0xFFD96570) else Color(0x2BFFFFFF))
                    .testTag("live_voice_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Stop Speaking / Interrupt Button (when AI is speaking)
            AnimatedVisibility(
                visible = state == LiveVoiceState.SPEAKING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4))
                        .testTag("live_voice_stop_speaking_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // End Call Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEA4335))
                    .testTag("live_voice_end_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "End Live session",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
