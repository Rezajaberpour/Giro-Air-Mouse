package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.example.ui.dialogs.NetworkLogsDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ConnectionStatus
import com.example.ui.components.GyroVisualizer
import com.example.ui.components.MouseButtonsSection
import com.example.ui.components.TrackpadArea
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MouseViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GiroAirMouseActiveSessionScreen(
  viewModel: MouseViewModel,
  onExitSession: () -> Unit,
  modifier: Modifier = Modifier
) {
  val connInfo by viewModel.connectionInfo.collectAsState()
  val isAirMouseActive by viewModel.isAirMouseActive.collectAsState()
  val gyroReading by viewModel.gyroReading.collectAsState()
  val isLeftPressed by viewModel.isLeftPressed.collectAsState()
  val isRightPressed by viewModel.isRightPressed.collectAsState()
  val config by viewModel.config.collectAsState()

  var isHudExpanded by remember { mutableStateOf(true) }
  var isNativePadView by remember { mutableStateOf(false) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var webViewError by remember { mutableStateOf(false) }
  var showLogsDialog by remember { mutableStateOf(false) }
  var pinInput by remember(connInfo.pin) { mutableStateOf(connInfo.pin) }

  // Ensure tracking is active and cookies are synced on session start
  LaunchedEffect(Unit) {
    viewModel.setAirMouseActive(true)
    viewModel.syncCookieFromWebView()
  }

  // Handle hardware back button to exit session smoothly
  BackHandler {
    onExitSession()
  }

  val targetUrl = remember(connInfo.rawAddress) {
    var u = connInfo.rawAddress.trim()
    if (!u.startsWith("http://") && !u.startsWith("https://")) {
      u = "http://$u"
    }
    u
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0D1117))
  ) {
    if (!isNativePadView) {
      // 1. Giro Air Mouse WebView Screen
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                webViewError = false
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                if (request?.isForMainFrame == true) {
                  webViewError = true
                }
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // When webview finishes loading or authenticating, synchronize session cookie to mouse client
                viewModel.syncCookieFromWebView()
              }
            }

            webChromeClient = WebChromeClient()
            loadUrl(targetUrl)
            webViewRef = this
          }
        },
        update = { webView ->
          if (webView.url != targetUrl) {
            webView.loadUrl(targetUrl)
          }
        }
      )

      // Non-blocking fallback banner at bottom if PC web server is still connecting
      if (webViewError) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xF21E242E)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "در حال اتصال به $targetUrl",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "ژیروسکوپ و کنترل ماوس با دکمه ولوم فعال است",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
            Button(
              onClick = { webViewRef?.reload() },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38404D)),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text("تلاش مجدد", fontSize = 12.sp)
            }
            Button(
              onClick = { showLogsDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("لاگ سوکت", fontSize = 12.sp)
            }
            Button(
              onClick = { isNativePadView = true },
              colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text("ترک‌پد", fontSize = 12.sp)
            }
          }
        }
      }
    } else {
      // 2. Native Air Mouse & Touchpad Screen
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 100.dp, bottom = 12.dp, start = 14.dp, end = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Hero visualizer
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
          GyroVisualizer(
            reading = gyroReading,
            isActive = isAirMouseActive,
            modifier = Modifier.fillMaxSize()
          )
        }

        // Left Click, Scroll, Right Click
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

        // Trackpad area
        TrackpadArea(
          onMove = { dx, dy -> viewModel.onTrackpadMove(dx, dy) },
          onClick = { viewModel.onLeftClick() },
          onRightClick = { viewModel.onRightClick() },
          modifier = Modifier.weight(1f)
        )
      }
    }

    // 3. Floating Gyroscope HUD (Available on top of both WebView and Native view)
    Surface(
      color = Color(0xF2101622),
      shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .border(1.dp, Color(0xFF222C3D), RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        // Top compact row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onExitSession,
              modifier = Modifier.size(34.dp)
            ) {
              Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Giro Air Mouse",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                val isConnected = connInfo.status == ConnectionStatus.CONNECTED
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF9100), CircleShape)
                )
              }
              val isConnected = connInfo.status == ConnectionStatus.CONNECTED
              Text(
                text = if (isConnected) "متصل به کامپیوتر (:8620)" else "${connInfo.serverIp}:${connInfo.serverPort} (در انتظار تایید PIN)",
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected) Color(0xFF00E676) else Color(0xFFFFB74D),
                fontSize = 11.sp
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            // View Mode Toggle (WebView vs Native Pad)
            FilledTonalButton(
              onClick = { isNativePadView = !isNativePadView },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Icon(
                imageVector = if (isNativePadView) Icons.Default.DesktopWindows else Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isNativePadView) "نمای وب" else "نمای لمسی",
                fontSize = 11.sp
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Socket Logs Button
            IconButton(
              onClick = { showLogsDialog = true },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "لاگ سوکت",
                tint = if (connInfo.lastError != null) Color(0xFFFF5252) else CyberCyan
              )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Expand/Collapse HUD
            IconButton(
              onClick = { isHudExpanded = !isHudExpanded },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(
                imageVector = if (isHudExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "HUD",
                tint = CyberCyan
              )
            }
          }
        }

        // Expanded HUD controls: Gyro Toggle, Volume Up Indicator, Calibration, Sensitivity, Quick PIN
        AnimatedVisibility(visible = isHudExpanded) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Primary Air Mouse Button
              Button(
                onClick = { viewModel.toggleAirMouse() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isAirMouseActive) CyberCyan else Color(0xFF26303F),
                  contentColor = if (isAirMouseActive) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1.3f)
                  .height(42.dp)
              ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isAirMouseActive) "ژیروسکوپ فعال ✓" else "شروع Air Mouse",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }

              // Physical Volume Up Indicator
              Surface(
                color = if (isLeftPressed) CyberCyan else Color(0xFF1E2838),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1.2f)
                  .height(42.dp)
                  .border(
                    width = 1.dp,
                    color = if (isLeftPressed) CyberCyan else Color(0xFF334155),
                    shape = RoundedCornerShape(12.dp)
                  )
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                  modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (isLeftPressed) Color.Black else CyberCyan,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "ولوم بالا = چپ کلیک",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLeftPressed) Color.Black else Color.White,
                    fontSize = 10.sp
                  )
                }
              }

              // Calibrate Zero button
              IconButton(
                onClick = { viewModel.calibrateGyro() },
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF26303F))
              ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "کالیبره صفر", tint = Color.White)
              }
            }

            // Quick PIN pairing bar if not yet connected
            if (connInfo.status != ConnectionStatus.CONNECTED) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF1B2330))
                  .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Pairing PIN ویندوز:", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  BasicTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(CyberCyan),
                    singleLine = true,
                    modifier = Modifier.width(65.dp)
                  )
                }
                Button(
                  onClick = { viewModel.pairWithPin(pinInput) },
                  colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text("اتصال مجدد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            // Quick Sensitivity Slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("سرعت موس:", color = Color(0xFF94A3B8), fontSize = 11.sp)
              Slider(
                value = config.sensitivityX,
                onValueChange = { s ->
                  viewModel.updateConfig(config.copy(sensitivityX = s, sensitivityY = s))
                },
                valueRange = 0.5f..5.0f,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 8.dp)
              )
              Text("%.1fx".format(config.sensitivityX), color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    if (showLogsDialog) {
      NetworkLogsDialog(
        viewModel = viewModel,
        onDismiss = { showLogsDialog = false }
      )
    }
  }
}
