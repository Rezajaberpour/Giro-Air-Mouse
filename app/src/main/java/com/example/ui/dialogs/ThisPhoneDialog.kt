package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CyberCyan

// "This phone" dialog for Giro Air Mouse
@Composable
fun ThisPhoneDialog(
  currentTimeout: String,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedTimeout by remember { mutableStateOf(currentTimeout) }
  var isDropdownOpen by remember { mutableStateOf(false) }
  var dimBrightness by remember { mutableFloatStateOf(0.3f) }

  val timeoutOptions = listOf(
    "Never",
    "After 15 seconds",
    "After 30 seconds",
    "After 1 minute",
    "After 2 minutes",
    "After 5 minutes"
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .testTag("this_phone_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2F33))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Text(
          text = "This phone",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Display settings for this phone only. Your PC display is unchanged.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFB0B6BE),
          fontSize = 13.sp,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "Dim after inactivity",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFFE2E4E8)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Timeout Dropdown
        Box {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF383C44))
              .clickable { isDropdownOpen = true }
              .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = selectedTimeout,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Icon(
              imageVector = Icons.Default.ArrowDropDown,
              contentDescription = null,
              tint = Color(0xFFB0B6BE)
            )
          }

          DropdownMenu(
            expanded = isDropdownOpen,
            onDismissRequest = { isDropdownOpen = false },
            modifier = Modifier.background(Color(0xFF383C44))
          ) {
            timeoutOptions.forEach { option ->
              DropdownMenuItem(
                text = { Text(option, color = Color.White) },
                onClick = {
                  selectedTimeout = option
                  isDropdownOpen = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Dim brightness",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFFE2E4E8)
        )

        Slider(
          value = dimBrightness,
          onValueChange = { dimBrightness = it },
          colors = SliderDefaults.colors(
            thumbColor = Color(0xFF67B7A4),
            activeTrackColor = Color(0xFF67B7A4),
            inactiveTrackColor = Color(0xFF4A4E58)
          )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("CANCEL", color = Color(0xFF67B7A4), fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = {
              onSave(selectedTimeout)
              onDismiss()
            }
          ) {
            Text("SAVE", color = Color(0xFF67B7A4), fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
