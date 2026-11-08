package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted

@Composable
fun TrackpadArea(
  onMove: (Float, Float) -> Unit,
  onClick: () -> Unit,
  onRightClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(DarkSurfaceVariant, DarkSurface)
        )
      )
      .testTag("trackpad_area")
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = { onClick() },
          onLongPress = { onRightClick() }
        )
      }
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          onMove(dragAmount.x, dragAmount.y)
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.TouchApp,
          contentDescription = null,
          tint = TextMuted.copy(alpha = 0.35f),
          modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
          text = "ترک‌پد لمسی کمکی\n(لمس برای حرکت و کلیک)",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted.copy(alpha = 0.6f),
          modifier = Modifier.padding(top = 32.dp),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }
    }
  }
}
