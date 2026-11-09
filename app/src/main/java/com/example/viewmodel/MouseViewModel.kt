package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ButtonAction
import com.example.model.ConnectionInfo
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.GyroReading
import com.example.model.MouseConfig
import com.example.model.SavedPc
import com.example.network.MouseClient
import com.example.sensor.GyroscopeManager
import com.example.util.VibratorHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MouseViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs: SharedPreferences = application.getSharedPreferences("gyromouse_prefs", Context.MODE_PRIVATE)
  private val vibratorHelper = VibratorHelper(application)
  private val mouseClient = MouseClient(viewModelScope)

  private lateinit var gyroManager: GyroscopeManager

  private val _config = MutableStateFlow(loadConfig())
  val config: StateFlow<MouseConfig> = _config.asStateFlow()

  private val _connectionMode = MutableStateFlow(ConnectionMode.WIFI)
  val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

  private val _isAirMouseActive = MutableStateFlow(false)
  val isAirMouseActive: StateFlow<Boolean> = _isAirMouseActive.asStateFlow()

  private val _isLeftPressed = MutableStateFlow(false)
  val isLeftPressed: StateFlow<Boolean> = _isLeftPressed.asStateFlow()

  private val _isRightPressed = MutableStateFlow(false)
  val isRightPressed: StateFlow<Boolean> = _isRightPressed.asStateFlow()

  private val _isMiddlePressed = MutableStateFlow(false)
  val isMiddlePressed: StateFlow<Boolean> = _isMiddlePressed.asStateFlow()

  private val _isInSession = MutableStateFlow(false)
  val isInSession: StateFlow<Boolean> = _isInSession.asStateFlow()

  // Default Wi-Fi (LAN) and USB addresses matching user's screenshots
  private val defaultWifiAddress = "http://10.121.168.197:8620"
  private val defaultUsbAddress = "http://10.140.42.236:8620"
  private val defaultPairingPin = "136460"

  private val _connectionInfo = MutableStateFlow(
    ConnectionInfo(
      rawAddress = prefs.getString("server_address", defaultWifiAddress) ?: defaultWifiAddress,
      serverIp = prefs.getString("server_ip", "10.121.168.197") ?: "10.121.168.197",
      serverPort = prefs.getInt("server_port", 8620),
      pin = prefs.getString("server_pin", defaultPairingPin) ?: defaultPairingPin
    )
  )
  val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()

  // Saved / Discovered PCs matching screenshots
  private val _savedPcs = MutableStateFlow(
    listOf(
      SavedPc(
        id = "pc_lan_1",
        name = "DESKTOP-3H32SKA",
        connectionType = "LAN",
        address = "http://10.121.168.197:8620",
        isCurrent = true
      ),
      SavedPc(
        id = "pc_usb_1",
        name = "DESKTOP-3H32SKA",
        connectionType = "USB",
        address = "http://10.140.42.236:8620",
        isCurrent = false
      )
    )
  )
  val savedPcs: StateFlow<List<SavedPc>> = _savedPcs.asStateFlow()

  val networkLogs: StateFlow<List<com.example.model.NetworkLogEntry>> = mouseClient.logs

  val gyroReading: StateFlow<GyroReading>
    get() = gyroManager.reading

  val hasGyroscope: Boolean
    get() = gyroManager.isSensorAvailable

  init {
    gyroManager = GyroscopeManager(application) { dx, dy ->
      mouseClient.sendMove(dx, dy)
    }
    gyroManager.config = _config.value
    gyroManager.start()

    // Sync client network states with ViewModel state
    viewModelScope.launch {
      combine(
        mouseClient.status,
        mouseClient.latencyMs,
        mouseClient.packetsSent,
        mouseClient.lastError,
        mouseClient.activeProtocolName
      ) { status, latency, packets, error, protocolName ->
        _connectionInfo.value = _connectionInfo.value.copy(
          status = status,
          latencyMs = latency,
          packetsSent = packets,
          lastError = error,
          activeProtocolName = protocolName
        )
      }.collect {}
    }
  }

  override fun onCleared() {
    super.onCleared()
    gyroManager.stop()
    mouseClient.disconnect()
  }

  fun setConnectionMode(mode: ConnectionMode) {
    _connectionMode.value = mode
    val currentPc = _savedPcs.value.firstOrNull {
      if (mode == ConnectionMode.WIFI) it.connectionType == "LAN" else it.connectionType == "USB"
    }
    if (currentPc != null) {
      updateAddress(currentPc.address)
    }
    if (_config.value.hapticsEnabled) {
      vibratorHelper.tick()
    }
  }

  fun updateAddress(rawAddress: String) {
    val clean = rawAddress.trim()
    val (host, port) = mouseClient.parseAddress(clean)
    _connectionInfo.value = _connectionInfo.value.copy(
      rawAddress = clean,
      serverIp = host,
      serverPort = port
    )
  }

  fun updatePin(pin: String) {
    val cleanPin = pin.trim()
    prefs.edit().putString("server_pin", cleanPin).apply()
    _connectionInfo.value = _connectionInfo.value.copy(pin = cleanPin)
  }

  fun updatePinAndReconnect(pin: String) {
    updatePin(pin)
    connect(_connectionInfo.value.rawAddress, pin)
  }

  fun connect(rawAddress: String, pin: String = "", portOverride: Int? = null) {
    val cleanAddress = rawAddress.trim()
    val (host, defaultPort) = mouseClient.parseAddress(cleanAddress)
    val port = portOverride ?: defaultPort
    val cleanPin = pin.trim()

    prefs.edit()
      .putString("server_address", cleanAddress)
      .putString("server_ip", host)
      .putInt("server_port", port)
      .putString("server_pin", cleanPin)
      .apply()

    _connectionInfo.value = _connectionInfo.value.copy(
      rawAddress = cleanAddress,
      serverIp = host,
      serverPort = port,
      pin = cleanPin
    )

    // Mark current in saved PCs
    _savedPcs.value = _savedPcs.value.map {
      it.copy(isCurrent = it.address == cleanAddress)
    }

    mouseClient.connect(cleanAddress, cleanPin, port)
    _isInSession.value = true
    _isAirMouseActive.value = true
    gyroManager.isTrackingActive = true

    if (_config.value.hapticsEnabled) {
      vibratorHelper.tick()
    }
  }

  fun selectPc(pc: SavedPc) {
    updateAddress(pc.address)
    connect(pc.address, _connectionInfo.value.pin)
  }

  fun forgetPc(id: String) {
    _savedPcs.value = _savedPcs.value.filter { it.id != id }
    if (_config.value.hapticsEnabled) {
      vibratorHelper.tick()
    }
  }

  fun disconnect() {
    mouseClient.disconnect()
    _isAirMouseActive.value = false
    gyroManager.isTrackingActive = false
    _isInSession.value = false
    if (_config.value.hapticsEnabled) {
      vibratorHelper.tick()
    }
  }

  fun exitSession() {
    _isInSession.value = false
    _isAirMouseActive.value = false
    gyroManager.isTrackingActive = false
  }

  fun autoDiscoverServer() {
    mouseClient.autoDiscoverServer { ip, port ->
      val newAddr = "http://$ip:$port"
      updateAddress(newAddr)
      val existing = _savedPcs.value.find { it.address.contains(ip) }
      if (existing == null) {
        val newPc = SavedPc(
          id = "pc_${System.currentTimeMillis()}",
          name = "Windows PC ($ip)",
          connectionType = "LAN",
          address = newAddr,
          isCurrent = true
        )
        _savedPcs.value = listOf(newPc) + _savedPcs.value
      }
      connect(newAddr, _connectionInfo.value.pin)
    }
  }

  fun toggleAirMouse() {
    val next = !_isAirMouseActive.value
    setAirMouseActive(next)
  }

  fun setAirMouseActive(active: Boolean) {
    _isAirMouseActive.value = active
    gyroManager.isTrackingActive = active
    if (_config.value.hapticsEnabled) {
      if (active) vibratorHelper.click() else vibratorHelper.tick()
    }
  }

  fun calibrateGyro() {
    gyroManager.calibrateZero()
    if (_config.value.hapticsEnabled) {
      vibratorHelper.heavyClick()
    }
  }

  fun clearNetworkLogs() {
    mouseClient.clearLogs()
  }

  fun syncCookieFromWebView() {
    val synced = mouseClient.syncCookieFromWebView()
    if (synced) {
      _isAirMouseActive.value = true
      gyroManager.isTrackingActive = true
    }
  }

  fun pairWithPin(pin: String) {
    updatePin(pin)
    viewModelScope.launch {
      val success = mouseClient.authenticateWithPin(_connectionInfo.value.serverIp, _connectionInfo.value.serverPort, pin)
      if (success) {
        _isAirMouseActive.value = true
        gyroManager.isTrackingActive = true
      }
    }
  }

  fun runNetworkDiagnostics() {
    mouseClient.runDiagnostics(_connectionInfo.value.serverIp, _connectionInfo.value.serverPort)
  }


  // --- Volume Key Interception ---
  fun handleVolumeKey(keyCode: Int, isDown: Boolean): Boolean {
    val currentConfig = _config.value
    if (!currentConfig.volumeInterceptEnabled) return false

    val action = when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> currentConfig.volumeUpAction
      KeyEvent.KEYCODE_VOLUME_DOWN -> currentConfig.volumeDownAction
      else -> return false
    }

    when (action) {
      ButtonAction.LEFT_CLICK -> {
        if (isDown) {
          if (!_isLeftPressed.value) {
            _isLeftPressed.value = true
            mouseClient.sendMouseDown("L")
            if (currentConfig.hapticsEnabled) vibratorHelper.click()
          }
        } else {
          _isLeftPressed.value = false
          mouseClient.sendMouseUp("L")
        }
        return true
      }
      ButtonAction.RIGHT_CLICK -> {
        if (isDown) {
          if (!_isRightPressed.value) {
            _isRightPressed.value = true
            mouseClient.sendMouseDown("R")
            if (currentConfig.hapticsEnabled) vibratorHelper.click()
          }
        } else {
          _isRightPressed.value = false
          mouseClient.sendMouseUp("R")
        }
        return true
      }
      ButtonAction.MIDDLE_CLICK -> {
        if (isDown) {
          if (!_isMiddlePressed.value) {
            _isMiddlePressed.value = true
            mouseClient.sendMouseDown("M")
            if (currentConfig.hapticsEnabled) vibratorHelper.click()
          }
        } else {
          _isMiddlePressed.value = false
          mouseClient.sendMouseUp("M")
        }
        return true
      }
      ButtonAction.GYRO_CLUTCH -> {
        setAirMouseActive(isDown)
        return true
      }
      ButtonAction.TOGGLE_GYRO -> {
        if (isDown) {
          toggleAirMouse()
        }
        return true
      }
      ButtonAction.SCROLL_UP -> {
        if (isDown) {
          mouseClient.sendScroll(1.0f)
          if (currentConfig.hapticsEnabled) vibratorHelper.tick()
        }
        return true
      }
      ButtonAction.SCROLL_DOWN -> {
        if (isDown) {
          mouseClient.sendScroll(-1.0f)
          if (currentConfig.hapticsEnabled) vibratorHelper.tick()
        }
        return true
      }
    }
  }

  // --- Touch Screen Mouse Actions ---
  fun onLeftDown() {
    _isLeftPressed.value = true
    mouseClient.sendMouseDown("L")
    if (_config.value.hapticsEnabled) vibratorHelper.click()
  }

  fun onLeftUp() {
    _isLeftPressed.value = false
    mouseClient.sendMouseUp("L")
  }

  fun onLeftClick() {
    mouseClient.sendClick("L")
    if (_config.value.hapticsEnabled) vibratorHelper.click()
  }

  fun onRightDown() {
    _isRightPressed.value = true
    mouseClient.sendMouseDown("R")
    if (_config.value.hapticsEnabled) vibratorHelper.click()
  }

  fun onRightUp() {
    _isRightPressed.value = false
    mouseClient.sendMouseUp("R")
  }

  fun onRightClick() {
    mouseClient.sendClick("R")
    if (_config.value.hapticsEnabled) vibratorHelper.click()
  }

  fun onMiddleClick() {
    mouseClient.sendClick("M")
    if (_config.value.hapticsEnabled) vibratorHelper.click()
  }

  fun onTrackpadMove(dx: Float, dy: Float) {
    var moveX = dx * (_config.value.sensitivityX * 0.9f)
    var moveY = dy * (_config.value.sensitivityY * 0.9f)
    if (_config.value.invertX) moveX = -moveX
    if (_config.value.invertY) moveY = -moveY
    mouseClient.sendMove(moveX, moveY)
  }

  fun onScrollWheel(amount: Float) {
    mouseClient.sendScroll(amount)
    if (_config.value.hapticsEnabled) vibratorHelper.tick()
  }

  fun updateConfig(newConfig: MouseConfig) {
    _config.value = newConfig
    gyroManager.config = newConfig
    saveConfig(newConfig)
  }

  fun toggleVolumeIntercept() {
    val updated = _config.value.copy(volumeInterceptEnabled = !_config.value.volumeInterceptEnabled)
    updateConfig(updated)
  }

  private fun loadConfig(): MouseConfig {
    return MouseConfig(
      sensitivityX = prefs.getFloat("sens_x", 2.0f),
      sensitivityY = prefs.getFloat("sens_y", 2.0f),
      smoothing = prefs.getFloat("smoothing", 0.65f),
      deadzone = prefs.getFloat("deadzone", 0.035f),
      acceleration = prefs.getFloat("acceleration", 1.3f),
      invertX = prefs.getBoolean("invert_x", false),
      invertY = prefs.getBoolean("invert_y", false),
      volumeUpAction = ButtonAction.valueOf(prefs.getString("vol_up_act", ButtonAction.LEFT_CLICK.name) ?: ButtonAction.LEFT_CLICK.name),
      volumeDownAction = ButtonAction.valueOf(prefs.getString("vol_down_act", ButtonAction.RIGHT_CLICK.name) ?: ButtonAction.RIGHT_CLICK.name),
      volumeInterceptEnabled = prefs.getBoolean("vol_intercept", true),
      hapticsEnabled = prefs.getBoolean("haptics", true),
      keepScreenAwake = prefs.getBoolean("keep_awake", true),
      dimInactivityTimeout = prefs.getString("dim_timeout", "After 30 seconds") ?: "After 30 seconds"
    )
  }

  private fun saveConfig(cfg: MouseConfig) {
    prefs.edit()
      .putFloat("sens_x", cfg.sensitivityX)
      .putFloat("sens_y", cfg.sensitivityY)
      .putFloat("smoothing", cfg.smoothing)
      .putFloat("deadzone", cfg.deadzone)
      .putFloat("acceleration", cfg.acceleration)
      .putBoolean("invert_x", cfg.invertX)
      .putBoolean("invert_y", cfg.invertY)
      .putString("vol_up_act", cfg.volumeUpAction.name)
      .putString("vol_down_act", cfg.volumeDownAction.name)
      .putBoolean("vol_intercept", cfg.volumeInterceptEnabled)
      .putBoolean("haptics", cfg.hapticsEnabled)
      .putBoolean("keep_awake", cfg.keepScreenAwake)
      .putString("dim_timeout", cfg.dimInactivityTimeout)
      .apply()
  }
}
