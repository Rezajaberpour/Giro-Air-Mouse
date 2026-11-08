package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.GyroReading
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceHighlight
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GyroVisualizer(
  reading: GyroReading,
  isActive: Boolean,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val maxRadius = minOf(size.width, size.height) / 2f

      // Background subtle grid rings
      drawCircle(
        color = DarkSurfaceHighlight.copy(alpha = 0.6f),
        radius = maxRadius,
        center = center,
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
      )
      drawCircle(
        color = DarkSurfaceHighlight.copy(alpha = 0.4f),
        radius = maxRadius * 0.65f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
      )
      drawCircle(
        color = DarkSurfaceHighlight.copy(alpha = 0.25f),
        radius = maxRadius * 0.35f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )

      // Axis crosshairs
      drawLine(
        color = DarkSurfaceHighlight.copy(alpha = 0.5f),
        start = Offset(center.x, center.y - maxRadius),
        end = Offset(center.x, center.y + maxRadius),
        strokeWidth = 1.5.dp.toPx()
      )
      drawLine(
        color = DarkSurfaceHighlight.copy(alpha = 0.5f),
        start = Offset(center.x - maxRadius, center.y),
        end = Offset(center.x + maxRadius, center.y),
        strokeWidth = 1.5.dp.toPx()
      )

      if (isActive) {
        // Glowing animated outer ring
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(CyberCyan.copy(alpha = 0.2f), Color.Transparent),
            center = center,
            radius = maxRadius * pulseScale
          ),
          radius = maxRadius * pulseScale,
          center = center
        )

        drawCircle(
          color = CyberCyan.copy(alpha = 0.8f),
          radius = maxRadius * 0.98f,
          center = center,
          style = Stroke(width = 2.5.dp.toPx())
        )

        // Dynamic pointer displacement based on gyro delta
        val clampDist = maxRadius * 0.85f
        val targetX = (center.x + reading.deltaX * 3.5f).coerceIn(center.x - clampDist, center.x + clampDist)
        val targetY = (center.y + reading.deltaY * 3.5f).coerceIn(center.y - clampDist, center.y + clampDist)
        val pointerPos = Offset(targetX, targetY)

        // Motion vector line
        drawLine(
          color = CyberCyan,
          start = center,
          end = pointerPos,
          strokeWidth = 3.dp.toPx()
        )

        // Motion pointer dot with glow
        drawCircle(
          color = CyberCyan.copy(alpha = 0.4f),
          radius = 16.dp.toPx(),
          center = pointerPos
        )
        drawCircle(
          color = CyberCyan,
          radius = 8.dp.toPx(),
          center = pointerPos
        )
        drawCircle(
          color = Color.White,
          radius = 3.5.dp.toPx(),
          center = pointerPos
        )

        // Center reticle
        drawCircle(
          color = AccentGreen,
          radius = 4.dp.toPx(),
          center = center
        )
      } else {
        // Idle center dot
        drawCircle(
          color = Color.Gray.copy(alpha = 0.5f),
          radius = 5.dp.toPx(),
          center = center
        )
      }
    }
  }
}
