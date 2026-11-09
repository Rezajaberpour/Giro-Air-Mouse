package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import com.example.model.ConnectionMode
import com.example.model.LogLevel
import com.example.ui.dialogs.NetworkLogsDialog
import com.example.ui.dialogs.ServerGuideDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.SwitchPcDialog
import com.example.ui.dialogs.ThisPhoneDialog
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CyberCyan
import com.example.viewmodel.MouseViewModel

// Giro Air Mouse Android Connection Screen
@Composable
fun GiroAirMouseConnectScreen(
  viewModel: MouseViewModel,
  modifier: Modifier = Modifier
) {
  val connInfo by viewModel.connectionInfo.collectAsState()
  val connMode by viewModel.connectionMode.collectAsState()
  val savedPcs by viewModel.savedPcs.collectAsState()
  val config by viewModel.config.collectAsState()
  val networkLogs by viewModel.networkLogs.collectAsState()

  var addressInput by remember(connInfo.rawAddress) { mutableStateOf(connInfo.rawAddress) }
  var pinInput by remember(connInfo.pin) { mutableStateOf(connInfo.pin) }

  androidx.compose.runtime.LaunchedEffect(connInfo.rawAddress) {
    addressInput = connInfo.rawAddress
  }
  androidx.compose.runtime.LaunchedEffect(connInfo.pin) {
    pinInput = connInfo.pin
  }

  var showThisPhoneDialog by remember { mutableStateOf(false) }
  var showSwitchPcDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showGuideDialog by remember { mutableStateOf(false) }
  var showLogsDialog by remember { mutableStateOf(false) }

  val bgDark = Color(0xFF090D14)
  val toggleActiveBg = Color(0xFF383E4B)
  val toggleInactiveBg = Color(0xFF1E232C)
  val btnGrey = Color(0xFF323742)
  val tealUnderline = Color(0xFF4DB6AC)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(bgDark)
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    // Top action icons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF161B24))
            .clickable { showSwitchPcDialog = true }
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DesktopWindows,
            contentDescription = null,
            tint = tealUnderline,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Switch PC",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        val errCount = networkLogs.count { it.level == LogLevel.ERROR }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (errCount > 0) Color(0x33D32F2F) else Color(0xFF161B24))
            .border(1.dp, if (errCount > 0) Color(0xFFD32F2F) else Color(0xFF263342), RoundedCornerShape(20.dp))
            .clickable { showLogsDialog = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("network_logs_top_button")
        ) {
          Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            tint = if (errCount > 0) Color(0xFFFF5252) else Color(0xFF4DB6AC),
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (errCount > 0) "خطای سوکت ($errCount)" else "لاگ شبکه",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Row {
        IconButton(onClick = { showGuideDialog = true }) {
          Icon(Icons.Default.Info, contentDescription = "Guide", tint = Color(0xFF90A4AE))
        }
        IconButton(onClick = { showSettingsDialog = true }) {
          Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF90A4AE))
        }
      }
    }

    // Main Giro Air Mouse Centered Panel
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .padding(vertical = 24.dp)
    ) {
      // Title
      Text(
        text = "Giro Air Mouse",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        fontSize = 32.sp,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Subtitle
      Text(
        text = "Connect to your PC, or search for another one.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9EAAAF),
        fontSize = 14.sp,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(26.dp))

      // Toggle Buttons: [Connect by Wi-Fi] [Connect by USB]
      Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val isWifi = connMode == ConnectionMode.WIFI
        Button(
          onClick = { viewModel.setConnectionMode(ConnectionMode.WIFI) },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isWifi) toggleActiveBg else toggleInactiveBg,
            contentColor = if (isWifi) Color.White else Color(0xFF8A939E)
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
          Text("Connect by Wi-Fi", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        val isUsb = connMode == ConnectionMode.USB
        Button(
          onClick = { viewModel.setConnectionMode(ConnectionMode.USB) },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isUsb) toggleActiveBg else toggleInactiveBg,
            contentColor = if (isUsb) Color.White else Color(0xFF8A939E)
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
          Text("Connect by USB", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Status text under toggle
      Text(
        text = if (connMode == ConnectionMode.WIFI) {
          "Wi-Fi selected • Phone and PC on the same network"
        } else {
          "USB selected • Phone tethered via USB"
        },
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF8C96A0),
        fontSize = 12.sp,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Input field with green/teal underline exactly matching Screenshot 2 & 5
      Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        BasicTextField(
          value = addressInput,
          onValueChange = {
            addressInput = it
            viewModel.updateAddress(it)
          },
          textStyle = TextStyle(
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          ),
          cursorBrush = SolidColor(tealUnderline),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("giro_air_mouse_address_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Green/teal underline bar
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(tealUnderline)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pairing PIN field matching user's Windows screenshot ("Pairing PIN: 136460")
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161C26))
            .border(1.dp, Color(0xFF2E394A), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = tealUnderline, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Pairing PIN:",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB0BEC5),
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.width(8.dp))
          BasicTextField(
            value = pinInput,
            onValueChange = {
              pinInput = it
              viewModel.updatePin(it)
            },
            textStyle = TextStyle(
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(tealUnderline),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(76.dp).testTag("pairing_pin_input")
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Two Buttons: [Use this address] (RED as requested) [This phone]
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = {
            viewModel.connect(addressInput, pinInput)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F), // Red button as requested
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
          modifier = Modifier.testTag("use_this_address_button")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Use this address", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("اینجا کلیک کنید", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Color(0xFFFFCDD2))
          }
        }

        Button(
          onClick = { showThisPhoneDialog = true },
          colors = ButtonDefaults.buttonColors(
            containerColor = btnGrey,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 12.dp),
          modifier = Modifier.testTag("this_phone_button")
        ) {
          Text("This phone", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }

      // Socket Error Diagnostic Alert Banner (if error occurred)
      if (connInfo.lastError != null || networkLogs.any { it.level == LogLevel.ERROR }) {
        Spacer(modifier = Modifier.height(14.dp))
        val firstError = networkLogs.firstOrNull { it.level == LogLevel.ERROR }
        val is401 = connInfo.lastError?.contains("401") == true ||
          (firstError?.message?.contains("401") == true) ||
          (firstError?.details?.contains("401") == true)

        Card(
          colors = CardDefaults.cardColors(containerColor = if (is401) Color(0xFF331518) else Color(0xFF2B1419)),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (is401) Color(0xFFE53935) else Color(0xFF9E2A36)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth(0.92f)
            .clickable { showLogsDialog = true }
            .testTag("socket_error_alert_banner")
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = if (is401) Icons.Default.Warning else Icons.Default.Error,
                contentDescription = null,
                tint = if (is401) Color(0xFFFF7043) else Color(0xFFFF5252),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (is401) "خطای ۴۰۱: رمز Pairing PIN با ویندوز تطابق ندارد" else "خطای سوکت در اتصال به کامپیوتر",
                  color = Color(0xFFFFCDD2),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (is401) "کد ۶ رقمی جدید نمایش‌داده‌شده در پنجره WinDock ویندوز را وارد کنید" else (connInfo.lastError ?: (firstError?.message ?: "برای عیب‌یابی و راهنما کلیک کنید")),
                  color = Color(0xFFEF9A9A),
                  fontSize = 11.sp,
                  maxLines = 1
                )
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = { showLogsDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(if (is401) "اصلاح PIN" else "بررسی لاگ", fontSize = 11.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(26.dp))

      // Air Mouse & Volume Up feature badge
      Surface(
        color = Color(0xFF141A24),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .border(1.dp, Color(0xFF283240), RoundedCornerShape(16.dp))
          .padding(4.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.MotionPhotosOn,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "موس ژیروسکوپ هوایی (Air Mouse)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp
              )
              Text(
                text = "دکمه ولوم بالا = کلیک چپ موس",
                style = MaterialTheme.typography.bodySmall,
                color = CyberCyan,
                fontSize = 11.sp
              )
            }
          }

          TextButton(onClick = { viewModel.autoDiscoverServer() }) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = tealUnderline, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("اسکن UDP", color = tealUnderline, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Dialogs
  if (showThisPhoneDialog) {
    ThisPhoneDialog(
      currentTimeout = config.dimInactivityTimeout,
      onSave = { newTimeout ->
        viewModel.updateConfig(config.copy(dimInactivityTimeout = newTimeout))
      },
      onDismiss = { showThisPhoneDialog = false }
    )
  }

  if (showSwitchPcDialog) {
    SwitchPcDialog(
      savedPcs = savedPcs,
      onSelectPc = { pc ->
        viewModel.selectPc(pc)
      },
      onForgetPc = { id ->
        viewModel.forgetPc(id)
      },
      onEnterAddress = {
        showSwitchPcDialog = false
      },
      onRefresh = {
        viewModel.autoDiscoverServer()
      },
      onDismiss = { showSwitchPcDialog = false }
    )
  }

  if (showGuideDialog) {
    ServerGuideDialog(onDismiss = { showGuideDialog = false })
  }

  if (showSettingsDialog) {
    SettingsDialog(
      currentConfig = config,
      onSaveConfig = { newConfig -> viewModel.updateConfig(newConfig) },
      onCalibrate = { viewModel.calibrateGyro() },
      onDismiss = { showSettingsDialog = false }
    )
  }

  if (showLogsDialog) {
    NetworkLogsDialog(
      viewModel = viewModel,
      onDismiss = { showLogsDialog = false }
    )
  }
}
