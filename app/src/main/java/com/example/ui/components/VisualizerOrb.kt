package com.example.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.HologramTextMuted
import com.example.ui.theme.HologramTextPrimary
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDim
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonMagentaDim
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerOrb(
    state: AssistantState,
    audioRms: Float,
    isListening: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbMotion")

    // Continuous rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Reverse ring rotation
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 5500 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ReverseRotation"
    )

    // Pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 600
                    AssistantState.PROCESSING -> 350
                    AssistantState.SPEAKING -> 800
                    AssistantState.EXECUTING -> 400
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Glowing energy color based on state
    val coreGlowColor = when (state) {
        AssistantState.LISTENING -> NeonCyan
        AssistantState.PROCESSING -> NeonMagenta
        AssistantState.SPEAKING -> MatrixGreen
        AssistantState.EXECUTING -> Color(0xFFFF9900)
        AssistantState.ERROR -> Color(0xFFFF2222)
        AssistantState.IDLE -> NeonCyan
    }

    val secondaryColor = when (state) {
        AssistantState.LISTENING -> NeonMagenta
        AssistantState.PROCESSING -> NeonCyan
        AssistantState.SPEAKING -> NeonCyan
        AssistantState.EXECUTING -> NeonMagenta
        else -> NeonCyanDim
    }

    Box(
        modifier = modifier
            .size(260.dp)
            .testTag("visualizer_orb_button")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressStart()
                        tryAwaitRelease()
                        onPressEnd()
                    },
                    onTap = {
                        onClick?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.78f

            // Dynamic RMS ripple boost
            val rmsBoost = (audioRms * 32.dp.toPx())
            val effectiveRadius = (baseRadius * pulseScale) + rmsBoost

            // 1. Outermost Ambient Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlowColor.copy(alpha = if (isListening) 0.35f else 0.15f),
                        secondaryColor.copy(alpha = if (isListening) 0.18f else 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.35f
                ),
                radius = effectiveRadius * 1.35f,
                center = center
            )

            // 2. Outer Dashed HUD Ring (Forward rotation)
            rotate(degrees = rotationAngle, pivot = center) {
                drawCircle(
                    color = coreGlowColor.copy(alpha = 0.65f),
                    radius = effectiveRadius,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f, 8f, 16f), 0f)
                    )
                )

                // 4 Satellite Nodes
                for (i in 0 until 4) {
                    val angleRad = Math.toRadians((i * 90.0))
                    val nodeX = center.x + effectiveRadius * cos(angleRad).toFloat()
                    val nodeY = center.y + effectiveRadius * sin(angleRad).toFloat()
                    drawCircle(
                        color = coreGlowColor,
                        radius = 4.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // 3. Middle Counter-Rotating Gear Ring (Reverse)
            rotate(degrees = reverseRotation, pivot = center) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.5f),
                    radius = effectiveRadius * 0.82f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 12f), 0f),
                        cap = StrokeCap.Round
                    )
                )

                // 6 Tech Arc Ticks
                for (i in 0 until 6) {
                    val angleRad = Math.toRadians((i * 60.0))
                    val innerX = center.x + (effectiveRadius * 0.74f) * cos(angleRad).toFloat()
                    val innerY = center.y + (effectiveRadius * 0.74f) * sin(angleRad).toFloat()
                    val outerX = center.x + (effectiveRadius * 0.88f) * cos(angleRad).toFloat()
                    val outerY = center.y + (effectiveRadius * 0.88f) * sin(angleRad).toFloat()
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.8f),
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // 4. Reactive Audio Wave Bars when listening
            if (isListening) {
                val waveCount = 24
                for (i in 0 until waveCount) {
                    val angleRad = Math.toRadians((i * (360.0 / waveCount)))
                    val barHeight = (4.dp.toPx() + (audioRms * 18.dp.toPx()) * (1f + sin(i * 1.5f).toFloat() * 0.5f))
                    val startR = effectiveRadius * 0.86f
                    val endR = startR + barHeight

                    val sx = center.x + startR * cos(angleRad).toFloat()
                    val sy = center.y + startR * sin(angleRad).toFloat()
                    val ex = center.x + endR * cos(angleRad).toFloat()
                    val ey = center.y + endR * sin(angleRad).toFloat()

                    drawLine(
                        color = NeonCyan,
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Arc Reactor Inner Core (Gradient Spherical Energy)
            val coreRadius = effectiveRadius * 0.58f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreGlowColor,
                        CyberDarkBg.copy(alpha = 0.95f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Inner Core Cyber Rings
            drawCircle(
                color = coreGlowColor.copy(alpha = 0.85f),
                radius = coreRadius * 0.65f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = coreRadius * 0.28f,
                center = center
            )
        }

        // Center Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isListening) "أسمعك" else "تحدث معي",
                fontFamily = OrbitronFontFamily,
                fontSize = 15.sp,
                color = if (isListening) NeonMagenta else HologramTextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (isListening) "ارفع إصبعك عندما تنتهي" else "اضغط باستمرار وتحدث",
                fontFamily = RajdhaniFontFamily,
                fontSize = 12.sp,
                color = if (isListening) NeonMagenta else HologramTextMuted
            )
        }
    }
}
