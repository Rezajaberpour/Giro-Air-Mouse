package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.ui.components.GyroVisualizer
import com.example.ui.components.MouseButtonsSection
import com.example.ui.components.TrackpadArea
import com.example.ui.dialogs.ConnectDialog
import com.example.ui.dialogs.ServerGuideDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MouseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
  viewModel: MouseViewModel,
  modifier: Modifier = Modifier
) {
  val config by viewModel.config.collectAsState()
  val connInfo by viewModel.connectionInfo.collectAsState()
  val isAirMouseActive by viewModel.isAirMouseActive.collectAsState()
  val gyroReading by viewModel.gyroReading.collectAsState()
  val isLeftPressed by viewModel.isLeftPressed.collectAsState()
  val isRightPressed by viewModel.isRightPressed.collectAsState()
  val hasGyro = viewModel.hasGyroscope

  var showConnectDialog by remember { mutableStateOf(false) }
  var showGuideDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DarkSurface,
          titleContentColor = TextPrimary
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showConnectDialog = true }
          ) {
            Surface(
              shape = CircleShape,
              color = CyberCyan.copy(alpha = 0.15f),
              modifier = Modifier.size(34.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Sensors,
                  contentDescription = null,
                  tint = CyberCyan,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "موس ژیروسکوپ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              )
              // Connection status pill
              Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusDotColor, statusText) = when (connInfo.status) {
                  ConnectionStatus.CONNECTED -> AccentGreen to "متصل: ${connInfo.activeProtocolName} (${connInfo.latencyMs}ms)"
                  ConnectionStatus.CONNECTING -> AccentOrange to "در حال اتصال به ${connInfo.serverIp}:${connInfo.serverPort}..."
                  ConnectionStatus.SEARCHING -> CyberCyan to "جستجوی Giro Air Mouse..."
                  ConnectionStatus.DISCONNECTED -> Color.Gray to "قطع ارتباط (کلیک برای اتصال Giro Air Mouse)"
                }
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = statusText,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        },
        actions = {
          // Volume Interception toggle chip
          IconButton(
            onClick = { viewModel.toggleVolumeIntercept() },
            modifier = Modifier.testTag("toggle_volume_intercept_button")
          ) {
            Icon(
              imageVector = if (config.volumeInterceptEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
              contentDescription = "کنترل ولوم",
              tint = if (config.volumeInterceptEnabled) CyberCyan else TextMuted
            )
          }

          // Windows setup guide button
          IconButton(
            onClick = { showGuideDialog = true },
            modifier = Modifier.testTag("windows_guide_button")
          ) {
            Icon(
              imageVector = Icons.Default.DesktopWindows,
              contentDescription = "راهنمای ویندوز",
              tint = CyberCyan
            )
          }

          // Settings button
          IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "تنظیمات",
              tint = TextSecondary
            )
          }
        }
      )
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Sensor warning banner if gyroscope is unavailable
      if (!hasGyro) {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "سنسور فیزیکی ژیروسکوپ شناسایی نشد؛ کنترل از طریق ترک‌پد لمسی فعال است.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }

      // Air Mouse Hero / Gyro Visualizer Card
      val airBorderColor by animateColorAsState(
        targetValue = if (isAirMouseActive) CyberCyan else DarkSurfaceHighlight,
        label = "airBorder"
      )

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .border(2.dp, airBorderColor, RoundedCornerShape(20.dp))
          .testTag("air_mouse_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          // Real-time Gyro motion radar in background
          GyroVisualizer(
            reading = gyroReading,
            isActive = isAirMouseActive,
            modifier = Modifier.fillMaxSize()
          )

          // Overlay Control Panel
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            // Status bar inside hero
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = if (isAirMouseActive) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceHighlight,
                shape = RoundedCornerShape(14.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.MotionPhotosOn,
                    contentDescription = null,
                    tint = if (isAirMouseActive) CyberCyan else TextMuted,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isAirMouseActive) "ژیروسکوپ فعال (Air Mouse ON)" else "ژیروسکوپ متوقف (Air Mouse OFF)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAirMouseActive) CyberCyan else TextMuted
                  )
                }
              }

              // Calibrate Zero Button
              FilledTonalButton(
                onClick = { viewModel.calibrateGyro() },
                modifier = Modifier.height(34.dp).testTag("calibrate_zero_button")
              ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("کالیبره صفر", fontSize = 11.sp)
              }
            }

            // Big Action Button: Toggle Air Mouse
            Button(
              onClick = { viewModel.toggleAirMouse() },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isAirMouseActive) CyberCyan else DarkSurfaceHighlight,
                contentColor = if (isAirMouseActive) Color.Black else TextPrimary
              ),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("toggle_air_mouse_button")
            ) {
              Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isAirMouseActive) "توقف حرکت موس با ژیروسکوپ" else "شروع حرکت موس با حرکت گوشی",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
              )
            }
          }
        }
      }

      // Mouse Buttons Section (Left Click [Vol Up], Scroll, Right Click [Vol Down])
      MouseButtonsSection(
        isLeftPressed = isLeftPressed,
        isRightPressed = isRightPressed,
        onLeftDown = { viewModel.onLeftDown() },
        onLeftUp = { viewModel.onLeftUp() },
        onRightDown = { viewModel.onRightDown() },
        onRightUp = { viewModel.onRightUp() },
        onMiddleClick = { viewModel.onMiddleClick() },
        onScroll = { amount -> viewModel.onScrollWheel(amount) }
      )

      // Hybrid Trackpad Area (for thumb resting & fine cursor targeting)
      TrackpadArea(
        onMove = { dx, dy -> viewModel.onTrackpadMove(dx, dy) },
        onClick = { viewModel.onLeftClick() },
        onRightClick = { viewModel.onRightClick() },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )

      // Quick sensitivity tuning slider on bottom
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurface)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Speed,
          contentDescription = null,
          tint = CyberCyan,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "سرعت:",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 11.sp
        )
        Slider(
          value = config.sensitivityX,
          onValueChange = { newSens ->
            viewModel.updateConfig(config.copy(sensitivityX = newSens, sensitivityY = newSens))
          },
          valueRange = 0.5f..5.0f,
          colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
            .testTag("quick_sensitivity_slider")
        )
        Text(
          text = "%.1fx".format(config.sensitivityX),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = CyberCyan,
          fontSize = 11.sp
        )
      }
    }
  }

  // Dialogs
  if (showConnectDialog) {
    ConnectDialog(
      currentInfo = connInfo,
      onConnect = { rawAddress, pin -> viewModel.connect(rawAddress, pin) },
      onDisconnect = { viewModel.disconnect() },
      onAutoDiscover = { viewModel.autoDiscoverServer() },
      onOpenGuide = { showGuideDialog = true },
      onDismiss = { showConnectDialog = false }
    )
  }

  if (showGuideDialog) {
    ServerGuideDialog(
      onDismiss = { showGuideDialog = false }
    )
  }

  if (showSettingsDialog) {
    SettingsDialog(
      currentConfig = config,
      onSaveConfig = { newConfig -> viewModel.updateConfig(newConfig) },
      onCalibrate = { viewModel.calibrateGyro() },
      onDismiss = { showSettingsDialog = false }
    )
  }
}
