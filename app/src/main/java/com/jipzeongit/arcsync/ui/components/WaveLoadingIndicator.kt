package com.jipzeongit.arcsync.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun WaveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    strokeWidth: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcSpinner")

    // Rotation — full circle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Sweep breathe — arc length oscillates
    val sweepPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepPhase"
    )

    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Canvas(modifier = modifier.size(48.dp)) {
        val sw = strokeWidth.toPx()
        val padding = sw * 2
        val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
        val topLeft = Offset(padding, padding)

        // Background track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Breathing sweep angle: varies between 40° and 270°
        val sweep = 40f + 230f * abs(sin(sweepPhase * Math.PI).toFloat())

        // Glow halo
        drawArc(
            color = color.copy(alpha = glowAlpha),
            startAngle = rotation - 90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(topLeft.x - sw * 0.5f, topLeft.y - sw * 0.5f),
            size = Size(arcSize.width + sw, arcSize.height + sw),
            style = Stroke(width = sw * 2.5f, cap = StrokeCap.Round)
        )

        // Main arc
        drawArc(
            color = color,
            startAngle = rotation - 90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}
