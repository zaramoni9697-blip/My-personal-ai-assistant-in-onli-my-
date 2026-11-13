package com.mdfoysal.jarvis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlue
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlueBright
import com.mdfoysal.jarvis.ui.theme.JarvisViolet

/** The four states of the JARVIS core. */
enum class CoreState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * The animated holographic AI core.
 *
 * It reacts to the assistant state: the rings spin faster while thinking, the amplitude
 * follows the microphone level while listening and the glow pulses while speaking.
 */
@Composable
fun JarvisCore(
    state: CoreState = CoreState.IDLE,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    amplitude: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "jarvis-core")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CoreState.THINKING -> 2600
                    CoreState.LISTENING -> 4200
                    CoreState.SPEAKING -> 3200
                    CoreState.IDLE -> 7000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CoreState.SPEAKING -> 620
                    CoreState.LISTENING -> 900
                    else -> 2200
                },
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primary = when (state) {
        CoreState.LISTENING -> JarvisHoloBlueBright
        CoreState.THINKING -> JarvisViolet
        CoreState.SPEAKING -> JarvisHoloBlue
        CoreState.IDLE -> JarvisHoloBlue
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val wave = amplitude.coerceIn(0f, 1f)

            // Soft holographic glow.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.30f + wave * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer ring.
            drawCircle(
                color = primary.copy(alpha = 0.45f),
                radius = radius * 0.92f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Rotating dashed arcs.
            val arcRadius = radius * 0.74f
            val sweep = 62f + wave * 40f
            listOf(
                spin to 0f,
                -spin * 0.7f to 130f,
                spin * 0.45f to 250f
            ).forEach { (rotation, startAngle) ->
                drawArc(
                    color = primary.copy(alpha = 0.85f),
                    startAngle = startAngle + rotation,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner pulsing ring.
            val innerRadius = radius * (0.42f + wave * 0.06f) * pulse
            drawCircle(
                color = primary.copy(alpha = 0.9f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Core.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisHoloBlueBright.copy(alpha = 0.95f),
                        primary.copy(alpha = 0.45f)
                    ),
                    center = center,
                    radius = radius * 0.3f
                ),
                radius = radius * 0.26f * pulse,
                center = center
            )
        }
    }
}

/** A small pulsing dot used for status indicators. */
@Composable
fun PulsingDot(
    color: Color = JarvisHoloBlue,
    modifier: Modifier = Modifier,
    diameter: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "pulsing-dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot-alpha"
    )
    Box(
        modifier = modifier
            .size(diameter)
            .background(color.copy(alpha = alpha), CircleShape)
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
    )
}
