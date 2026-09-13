package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary

@Composable
fun VoiceWaveformBar(
    isListening: Boolean,
    isSpeaking: Boolean,
    soundLevel: Float,
    statusText: String,
    modifier: Modifier = Modifier
) {
    if (!isListening && !isSpeaking) return

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isListening) "सुन रहा हूँ... बोलिए (Listening...)" else "AI बोल रहा है (Speaking...)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isListening) CyanSecondary else VioletTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
            }

            // Animated waveform bars
            Canvas(
                modifier = Modifier
                    .height(32.dp)
                    .padding(start = 12.dp)
            ) {
                val barCount = 7
                val barWidth = 4.dp.toPx()
                val spacing = 3.dp.toPx()
                val maxHeight = size.height

                val baseLevel = if (isListening) soundLevel.coerceAtLeast(0.15f) else 0.5f

                for (i in 0 until barCount) {
                    val multiplier = when (i % 3) {
                        0 -> phase1
                        1 -> phase2
                        else -> (phase1 + phase2) / 2f
                    }
                    val currentHeight = (maxHeight * baseLevel * multiplier).coerceIn(6f, maxHeight)
                    val x = i * (barWidth + spacing)
                    val y = (maxHeight - currentHeight) / 2f

                    val brush = Brush.verticalGradient(
                        colors = listOf(IndigoPrimary, CyanSecondary),
                        startY = y,
                        endY = y + currentHeight
                    )

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, currentHeight),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }
}
