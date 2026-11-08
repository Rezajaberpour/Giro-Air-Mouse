package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ConnectionInfo
import com.example.model.ConnectionStatus
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConnectDialog(
  currentInfo: ConnectionInfo,
  onConnect: (String, String) -> Unit, // rawAddress, pin
  onDisconnect: () -> Unit,
  onAutoDiscover: () -> Unit,
  onOpenGuide: () -> Unit,
  onDismiss: () -> Unit
) {
  var addressInput by remember {
    mutableStateOf(
      if (currentInfo.rawAddress.isNotBlank()) currentInfo.rawAddress else "${currentInfo.serverIp}:${currentInfo.serverPort}"
    )
  }
  var pinInput by remember { mutableStateOf(currentInfo.pin) }

  val isSearching = currentInfo.status == ConnectionStatus.SEARCHING
  val isConnecting = currentInfo.status == ConnectionStatus.CONNECTING
  val isConnected = currentInfo.status == ConnectionStatus.CONNECTED

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .padding(10.dp)
        .testTag("connect_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Title Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lan,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "اتصال به ویندوز ۱۰ (Giro Air Mouse)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Giro Air Mouse Windows 10 Connection",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Info Banner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp)
        ) {
          Text(
            text = "فرمت آدرس‌دهی Giro Air Mouse:\n• آدرس کامل: http://192.168.1.5:8620\n• یا آی‌پی و پورت: 192.168.1.5:8620\n(پورت پیش‌فرض سرور برابر ۸۶۲۰ است)",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Address input field
        OutlinedTextField(
          value = addressInput,
          onValueChange = { addressInput = it },
          label = { Text("آدرس IP و پورت ویندوز (Host / Port)") },
          placeholder = { Text("192.168.1.100:8620") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("ip_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Optional PIN input field for authentication
        OutlinedTextField(
          value = pinInput,
          onValueChange = { pinInput = it },
          label = { Text("کد PIN پِرینگ (اختیاری)") },
          placeholder = { Text("در صورت فعال بودن در سرور وارد کنید") },
          leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("pin_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Auto Discover Button
        OutlinedButton(
          onClick = onAutoDiscover,
          enabled = !isSearching,
          modifier = Modifier.fillMaxWidth().testTag("auto_discover_button")
        ) {
          if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("در حال جستجوی Giro Air Mouse روی پورت 41234 و 8620...")
          } else {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("جستجوی خودکار سرور در شبکه وای‌فای")
          }
        }

        if (currentInfo.lastError != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = currentInfo.lastError,
            style = MaterialTheme.typography.bodySmall,
            color = AccentRed,
            fontSize = 11.sp
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Help guide prompt
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenGuide()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("راهنمای راه‌اندازی Giro Air Mouse و سرور ویندوز")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connect / Disconnect Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (isConnected) {
            Button(
              onClick = {
                onDisconnect()
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
              modifier = Modifier.weight(1f)
            ) {
              Text("قطع اتصال", fontWeight = FontWeight.Bold)
            }
          } else {
            Button(
              onClick = {
                onConnect(addressInput, pinInput)
                onDismiss()
              },
              enabled = addressInput.isNotBlank() && !isConnecting,
              modifier = Modifier.weight(1f).testTag("connect_submit_button")
            ) {
              if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkSurface, strokeWidth = 2.dp)
              } else {
                Text("اتصال به ویندوز", fontWeight = FontWeight.Bold)
              }
            }
          }

          FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier.weight(0.7f)
          ) {
            Text("بستن")
          }
        }
      }
    }
  }
}
