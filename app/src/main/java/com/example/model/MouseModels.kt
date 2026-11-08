package com.example.model

enum class ButtonAction(val labelFa: String, val labelEn: String) {
  LEFT_CLICK("کلیک چپ", "Left Click"),
  RIGHT_CLICK("کلیک راست", "Right Click"),
  MIDDLE_CLICK("کلیک وسط", "Middle Click"),
  GYRO_CLUTCH("کلاچ ژیروسکوپ (نگه‌داشتن برای حرکت)", "Hold to Move"),
  TOGGLE_GYRO("تغییر وضعیت ژیروسکوپ", "Toggle Air Mouse"),
  SCROLL_UP("اسکرول به بالا", "Scroll Up"),
  SCROLL_DOWN("اسکرول به پایین", "Scroll Down")
}

data class MouseConfig(
  val sensitivityX: Float = 2.0f,
  val sensitivityY: Float = 2.0f,
  val smoothing: Float = 0.65f,
  val deadzone: Float = 0.035f,
  val acceleration: Float = 1.3f,
  val invertX: Boolean = false,
  val invertY: Boolean = false,
  val volumeUpAction: ButtonAction = ButtonAction.LEFT_CLICK,
  val volumeDownAction: ButtonAction = ButtonAction.RIGHT_CLICK,
  val volumeInterceptEnabled: Boolean = true,
  val hapticsEnabled: Boolean = true,
  val keepScreenAwake: Boolean = true,
  val dimInactivityTimeout: String = "After 30 seconds"
)

enum class ConnectionStatus {
  DISCONNECTED,
  SEARCHING,
  CONNECTING,
  CONNECTED
}

enum class ConnectionMode {
  WIFI,
  USB
}

data class SavedPc(
  val id: String,
  val name: String = "DESKTOP-3H32SKA",
  val connectionType: String = "LAN", // "LAN" or "USB"
  val address: String = "http://10.121.168.197:8620",
  val isCurrent: Boolean = true
)

data class ConnectionInfo(
  val rawAddress: String = "http://10.121.168.197:8620",
  val serverIp: String = "10.121.168.197",
  val serverPort: Int = 8620,
  val pin: String = "",
  val activeProtocolName: String = "Giro Air Mouse (WebSocket/HTTP)",
  val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
  val latencyMs: Long = 0L,
  val packetsSent: Long = 0L,
  val lastError: String? = null
)

data class GyroReading(
  val rawX: Float = 0f,
  val rawY: Float = 0f,
  val rawZ: Float = 0f,
  val deltaX: Float = 0f,
  val deltaY: Float = 0f,
  val isActive: Boolean = false
)

enum class LogLevel {
  INFO,
  SUCCESS,
  WARN,
  ERROR
}

data class NetworkLogEntry(
  val id: Long = System.currentTimeMillis(),
  val time: String,
  val level: LogLevel,
  val tag: String,
  val message: String,
  val details: String? = null,
  val diagnosticHint: String? = null
)

