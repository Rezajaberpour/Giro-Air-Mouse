package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.GiroAirMouseActiveSessionScreen
import com.example.ui.screens.GiroAirMouseConnectScreen
import com.example.ui.theme.GyroMouseTheme
import com.example.viewmodel.MouseViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: MouseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val config by viewModel.config.collectAsState()
      val isInSession by viewModel.isInSession.collectAsState()

      // Handle keep-screen-on
      if (config.keepScreenAwake) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }

      GyroMouseTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF090D14)
        ) {
          Crossfade(targetState = isInSession, label = "screen_transition") { inSession ->
            if (inSession) {
              GiroAirMouseActiveSessionScreen(
                viewModel = viewModel,
                onExitSession = { viewModel.exitSession() }
              )
            } else {
              GiroAirMouseConnectScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      val isDown = event.action == KeyEvent.ACTION_DOWN
      // Dispatches hardware volume key press to mouse client and intercepts Android volume pop-up
      if (viewModel.handleVolumeKey(keyCode, isDown)) {
        return true
      }
    }
    return super.dispatchKeyEvent(event)
  }
}
