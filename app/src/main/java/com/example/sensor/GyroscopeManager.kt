package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.GyroReading
import com.example.model.MouseConfig
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GyroscopeManager(
  context: Context,
  private val onDeltaCalculated: (dx: Float, dy: Float) -> Unit
) : SensorEventListener {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

  private val _reading = MutableStateFlow(GyroReading())
  val reading: StateFlow<GyroReading> = _reading.asStateFlow()

  var config: MouseConfig = MouseConfig()
  var isTrackingActive: Boolean = false

  // Zero offsets from calibration
  private var zeroOffsetX = 0f
  private var zeroOffsetY = 0f
  private var zeroOffsetZ = 0f

  // Smoothing state
  private var smoothedDx = 0f
  private var smoothedDy = 0f

  val isSensorAvailable: Boolean get() = gyroSensor != null

  fun start() {
    gyroSensor?.let {
      sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
    }
  }

  fun stop() {
    sensorManager.unregisterListener(this)
    _reading.value = GyroReading()
  }

  fun calibrateZero() {
    val cur = _reading.value
    zeroOffsetX = cur.rawX
    zeroOffsetY = cur.rawY
    zeroOffsetZ = cur.rawZ
    smoothedDx = 0f
    smoothedDy = 0f
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null || event.values.isEmpty()) return

    val rawX = event.values[0] - zeroOffsetX
    val rawY = if (event.values.size > 1) event.values[1] - zeroOffsetY else 0f
    val rawZ = if (event.values.size > 2) event.values[2] - zeroOffsetZ else 0f

    if (!isTrackingActive) {
      _reading.value = GyroReading(rawX, rawY, rawZ, 0f, 0f, false)
      smoothedDx = 0f
      smoothedDy = 0f
      return
    }

    // Hybrid pointer algorithm:
    // When holding phone pointing forward or slightly angled:
    // Turning hand horizontally (yaw) primarily affects Z and Y.
    // Tilting hand up/down (pitch) affects X.
    var inputX = -(rawZ * 0.75f + rawY * 0.45f)
    var inputY = -rawX

    // Deadzone
    val deadzone = config.deadzone
    if (abs(inputX) < deadzone) inputX = 0f else inputX -= sign(inputX) * deadzone
    if (abs(inputY) < deadzone) inputY = 0f else inputY -= sign(inputY) * deadzone

    // Inversions
    if (config.invertX) inputX = -inputX
    if (config.invertY) inputY = -inputY

    // Non-linear acceleration for laser pointer precision vs fast flicks
    val accel = config.acceleration.coerceIn(1.0f, 2.0f)
    val accelX = sign(inputX) * abs(inputX).pow(accel) * (config.sensitivityX * 22f)
    val accelY = sign(inputY) * abs(inputY).pow(accel) * (config.sensitivityY * 22f)

    // Exponential Smoothing Filter (Low Pass)
    val alpha = (1f - config.smoothing).coerceIn(0.1f, 1.0f)
    smoothedDx = alpha * accelX + (1f - alpha) * smoothedDx
    smoothedDy = alpha * accelY + (1f - alpha) * smoothedDy

    _reading.value = GyroReading(rawX, rawY, rawZ, smoothedDx, smoothedDy, true)

    if (abs(smoothedDx) > 0.05f || abs(smoothedDy) > 0.05f) {
      onDeltaCalculated(smoothedDx, smoothedDy)
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
