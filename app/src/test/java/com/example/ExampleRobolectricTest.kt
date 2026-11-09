package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.model.ButtonAction
import com.example.model.ConnectionMode
import com.example.model.MouseConfig
import com.example.viewmodel.MouseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Giro Air Mouse", appName)
  }

  @Test
  fun testVolumeKeyInterception() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MouseViewModel(app)

    // Ensure volume up acts as left click
    assertEquals(ButtonAction.LEFT_CLICK, viewModel.config.value.volumeUpAction)

    val intercepted = viewModel.handleVolumeKey(KeyEvent.KEYCODE_VOLUME_UP, isDown = true)
    assertTrue("Volume Up key should be intercepted", intercepted)
    assertTrue("Left mouse click state should be pressed", viewModel.isLeftPressed.value)

    viewModel.handleVolumeKey(KeyEvent.KEYCODE_VOLUME_UP, isDown = false)
    assertEquals(false, viewModel.isLeftPressed.value)
  }

  @Test
  fun testGiroAirMouseAddressingAndSavedPcs() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MouseViewModel(app)

    // Verify initial screenshot default PC and pairing PIN from Windows
    assertTrue(viewModel.savedPcs.value.any { it.address == "http://10.140.42.236:8620" })
    assertEquals("136460", viewModel.connectionInfo.value.pin)

    // Test Giro Air Mouse connect with PIN
    viewModel.connect("http://10.140.42.236:8620", "136460")
    assertEquals("10.140.42.236", viewModel.connectionInfo.value.serverIp)
    assertEquals(8620, viewModel.connectionInfo.value.serverPort)
    assertEquals("136460", viewModel.connectionInfo.value.pin)
    assertTrue(viewModel.isInSession.value)

    // Test mode toggle
    viewModel.setConnectionMode(ConnectionMode.WIFI)
    assertEquals(ConnectionMode.WIFI, viewModel.connectionMode.value)
    assertEquals("http://10.121.168.197:8620", viewModel.connectionInfo.value.rawAddress)
  }

  @Test
  fun testNetworkLoggingAndDiagnostics() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MouseViewModel(app)

    // Trigger connect to generate log entries
    viewModel.connect("http://10.121.168.197:8620", "136460")

    val logs = viewModel.networkLogs.value
    assertTrue("Should have recorded connection log entries", logs.isNotEmpty())
    val connectLog = logs.find { it.tag == "Connect" }
    assertTrue("Should have a Connect tag log entry", connectLog != null)

    // Clear logs
    viewModel.clearNetworkLogs()
    assertEquals(0, viewModel.networkLogs.value.size)
  }

  @Test
  fun testWinDockInputProtocolAndCookieSync() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MouseViewModel(app)

    // Test mouse moves and click events
    viewModel.onTrackpadMove(5.2f, -3.8f)
    viewModel.onLeftClick()
    viewModel.onRightClick()
    viewModel.onScrollWheel(2.0f)

    // Test cookie sync
    viewModel.syncCookieFromWebView()

    // Test PIN update and pair
    viewModel.pairWithPin("136460")
    assertEquals("136460", viewModel.connectionInfo.value.pin)
  }
}
