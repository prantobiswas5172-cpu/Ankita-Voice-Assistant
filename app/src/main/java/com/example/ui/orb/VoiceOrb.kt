package com.example.ui.orb

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.service.AssistantVisualState
import com.example.ui.theme.AmberSpeaking
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: AssistantVisualState,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceOrbTransition")

    // Slow breathing rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse fast rotation for secondary ring
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "revRotation"
    )

    // Breathing scale for idle/listening
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantVisualState.SLEEPING -> 2400
                    AssistantVisualState.LISTENING_WAKE -> 1400
                    AssistantVisualState.LISTENING_COMMAND -> 800
                    AssistantVisualState.PROCESSING -> 600
                    AssistantVisualState.SPEAKING -> 400
                    AssistantVisualState.WAKE_DETECTED -> 300
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .testTag("voice_orb")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2.7f
            val dynamicScale = pulseScale + (audioLevel * 0.35f)

            // Primary Theme Colors according to current state
            val (coreColorA, coreColorB, auraColor) = when (state) {
                AssistantVisualState.SLEEPING -> Triple(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A),
                    Color(0x3300F0FF)
                )
                AssistantVisualState.LISTENING_WAKE -> Triple(
                    NeonCyan,
                    ElectricViolet,
                    Color(0x5500F0FF)
                )
                AssistantVisualState.WAKE_DETECTED -> Triple(
                    Color(0xFFFACC15),
                    NeonCyan,
                    Color(0x88FACC15)
                )
                AssistantVisualState.LISTENING_COMMAND -> Triple(
                    NeonCyan,
                    NeonPink,
                    Color(0x7700F0FF)
                )
                AssistantVisualState.PROCESSING -> Triple(
                    ElectricViolet,
                    NeonPink,
                    Color(0x668B5CF6)
                )
                AssistantVisualState.SPEAKING -> Triple(
                    AmberSpeaking,
                    ElectricViolet,
                    Color(0x77F59E0B)
                )
            }

            // 1. Outer Diffuse Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = center,
                    radius = baseRadius * 1.6f * dynamicScale
                ),
                radius = baseRadius * 1.6f * dynamicScale,
                center = center
            )

            // 2. Orbital Waveform Particle Ring 1
            val ring1Radius = baseRadius * 1.25f * dynamicScale
            val ring1Points = 36
            val rotRad1 = Math.toRadians(rotation.toDouble())
            for (i in 0 until ring1Points) {
                val angle = (i.toDouble() / ring1Points) * 2 * Math.PI + rotRad1
                val r = ring1Radius + (sin(angle * 4 + rotRad1) * 6f * (1f + audioLevel * 3f)).toFloat()
                val x = center.x + (cos(angle) * r).toFloat()
                val y = center.y + (sin(angle) * r).toFloat()
                val pColor = if (i % 2 == 0) coreColorA else coreColorB
                drawCircle(
                    color = pColor.copy(alpha = 0.7f),
                    radius = if (i % 4 == 0) 4f else 2.5f,
                    center = Offset(x, y)
                )
            }

            // 3. Orbital Waveform Ring 2 (Counter-rotating)
            val ring2Radius = baseRadius * 1.1f * dynamicScale
            val rotRad2 = Math.toRadians(reverseRotation.toDouble())
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(coreColorA, coreColorB, coreColorA),
                    center = center
                ),
                radius = ring2Radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 4. Central Solid Glowing Nucleus Orb
            val nucleusRadius = baseRadius * 0.85f * dynamicScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColorA.copy(alpha = 0.95f),
                        coreColorB.copy(alpha = 0.8f),
                        Color(0xFF070A12)
                    ),
                    center = Offset(center.x - nucleusRadius * 0.2f, center.y - nucleusRadius * 0.25f),
                    radius = nucleusRadius
                ),
                radius = nucleusRadius,
                center = center
            )

            // 5. High-tech Inner Reticle Rings
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = nucleusRadius * 0.45f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Dynamic Speaking Waveform cross bars inside nucleus
            if (state == AssistantVisualState.SPEAKING || audioLevel > 0.08f) {
                val bars = 7
                val barMaxHeight = nucleusRadius * 0.7f
                val spacing = nucleusRadius * 0.16f
                val startX = center.x - (spacing * (bars - 1) / 2f)

                for (b in 0 until bars) {
                    val factor = sin(rotation * 0.1f + b).toFloat()
                    val h = ((factor * factor) * barMaxHeight * (0.3f + audioLevel * 0.7f)).coerceAtLeast(6f)
                    val x = startX + b * spacing
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(x, center.y - h / 2f),
                        end = Offset(x, center.y + h / 2f),
                        strokeWidth = 3f
                    )
                }
            }
        }
    }
}
