package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun MouseButtonsSection(
  isLeftPressed: Boolean,
  isRightPressed: Boolean,
  onLeftDown: () -> Unit,
  onLeftUp: () -> Unit,
  onRightDown: () -> Unit,
  onRightUp: () -> Unit,
  onMiddleClick: () -> Unit,
  onScroll: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Left Click Button (Primary - Linked with Volume Up)
    val leftScale by animateFloatAsState(targetValue = if (isLeftPressed) 0.96f else 1f, label = "leftScale")
    val leftBorderColor by animateColorAsState(
      targetValue = if (isLeftPressed) CyberCyan else CyberCyan.copy(alpha = 0.35f),
      label = "leftBorder"
    )
    val leftBgColor by animateColorAsState(
      targetValue = if (isLeftPressed) CyberCyan.copy(alpha = 0.25f) else DarkSurfaceVariant,
      label = "leftBg"
    )

    Box(
      modifier = Modifier
        .weight(1.3f)
        .height(130.dp)
        .scale(leftScale)
        .clip(RoundedCornerShape(16.dp))
        .border(2.dp, leftBorderColor, RoundedCornerShape(16.dp))
        .background(leftBgColor)
        .testTag("left_click_button")
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {
              onLeftDown()
              tryAwaitRelease()
              onLeftUp()
            }
          )
        },
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp)
      ) {
        // Hardware key indicator badge
        Surface(
          color = if (isLeftPressed) CyberCyan else DarkSurfaceHighlight,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = null,
              tint = if (isLeftPressed) Color.Black else CyberCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "دکمه ولوم بالا",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isLeftPressed) Color.Black else CyberCyan,
              fontSize = 11.sp
            )
          }
        }

        Text(
          text = "کلیک چپ",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = if (isLeftPressed) CyberCyan else TextPrimary
        )
        Text(
          text = "Left Click",
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted
        )
      }
    }

    // Scroll Wheel Strip
    Box(
      modifier = Modifier
        .width(58.dp)
        .height(130.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(1.5.dp, DarkSurfaceHighlight, RoundedCornerShape(16.dp))
        .background(DarkSurface)
        .testTag("scroll_wheel_strip")
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = { onMiddleClick() }
          )
        }
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            // Invert drag to natural scroll: dragging down scrolls down
            onScroll(-dragAmount.y / 35f)
          }
        },
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxHeight().padding(vertical = 10.dp)
      ) {
        Icon(
          imageVector = Icons.Default.SwapVert,
          contentDescription = "Scroll",
          tint = CyberCyan.copy(alpha = 0.7f),
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = "اسکرول\nوسط",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = TextMuted,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Box(
          modifier = Modifier
            .size(16.dp, 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(DarkSurfaceHighlight)
        )
      }
    }

    // Right Click Button (Secondary - Linked with Volume Down)
    val rightScale by animateFloatAsState(targetValue = if (isRightPressed) 0.96f else 1f, label = "rightScale")
    val rightBorderColor by animateColorAsState(
      targetValue = if (isRightPressed) AccentOrange else AccentOrange.copy(alpha = 0.35f),
      label = "rightBorder"
    )
    val rightBgColor by animateColorAsState(
      targetValue = if (isRightPressed) AccentOrange.copy(alpha = 0.25f) else DarkSurfaceVariant,
      label = "rightBg"
    )

    Box(
      modifier = Modifier
        .weight(1.3f)
        .height(130.dp)
        .scale(rightScale)
        .clip(RoundedCornerShape(16.dp))
        .border(2.dp, rightBorderColor, RoundedCornerShape(16.dp))
        .background(rightBgColor)
        .testTag("right_click_button")
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {
              onRightDown()
              tryAwaitRelease()
              onRightUp()
            }
          )
        },
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp)
      ) {
        // Hardware key indicator badge
        Surface(
          color = if (isRightPressed) AccentOrange else DarkSurfaceHighlight,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VolumeDown,
              contentDescription = null,
              tint = if (isRightPressed) Color.Black else AccentOrange,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "دکمه ولوم پایین",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isRightPressed) Color.Black else AccentOrange,
              fontSize = 11.sp
            )
          }
        }

        Text(
          text = "کلیک راست",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = if (isRightPressed) AccentOrange else TextPrimary
        )
        Text(
          text = "Right Click",
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted
        )
      }
    }
  }
}
