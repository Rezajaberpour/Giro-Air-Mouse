package com.example.network

import android.util.Log
import com.example.model.ConnectionStatus
import com.example.model.LogLevel
import com.example.model.NetworkLogEntry
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MouseClient(private val coroutineScope: CoroutineScope) {
  private val TAG = "MouseClient"

  // UDP Socket
  private var udpSocket: DatagramSocket? = null
  private var targetAddress: InetAddress? = null
  private var targetPort: Int = 8620

  // OkHttp WebSocket for Giro Air Mouse
  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websocket
    .build()
  private var webSocket: WebSocket? = null
  private var isWsConnected = false
  private var sessionCookie: String? = null

  private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
  val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

  private val _latencyMs = MutableStateFlow(0L)
  val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

  private val _packetsSent = MutableStateFlow(0L)
  val packetsSent: StateFlow<Long> = _packetsSent.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(null)
  val lastError: StateFlow<String?> = _lastError.asStateFlow()

  private val _activeProtocolName = MutableStateFlow("Giro Air Mouse / UDP")
  val activeProtocolName: StateFlow<String> = _activeProtocolName.asStateFlow()

  // Socket & Network Event Logs captured for UI inspection
  private val _logs = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
  val logs: StateFlow<List<NetworkLogEntry>> = _logs.asStateFlow()

  private var pingJob: Job? = null
  private var receiveJob: Job? = null

  var currentHost: String = "192.168.1.100"
  var currentPort: Int = 8620
  var currentPin: String = ""

  /**
   * Log an event or socket error and expose it to the UI in real time.
   */
  fun log(
    level: LogLevel,
    tag: String,
    message: String,
    details: String? = null,
    diagnosticHint: String? = null
  ) {
    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    val entry = NetworkLogEntry(
      time = time,
      level = level,
      tag = tag,
      message = message,
      details = details,
      diagnosticHint = diagnosticHint
    )
    Log.d(TAG, "[$time][$level][$tag] $message ${details ?: ""}")
    _logs.value = (listOf(entry) + _logs.value).take(150)
    if (level == LogLevel.ERROR) {
      _lastError.value = message
    }
  }

  fun clearLogs() {
    _logs.value = emptyList()
    _lastError.value = null
  }

  /**
   * Intelligent diagnostic advice based on socket exception & HTTP status
   */
  private fun getDiagnosticHint(e: Throwable?, responseCode: Int? = null): String {
    val msg = e?.message ?: ""
    return when {
      responseCode == 403 || msg.contains("403", ignoreCase = true) ->
        "ویندوز درخواست را به دلیل هدر Origin یا دسترسی رد کرد (HTTP 403 Forbidden)."
      responseCode == 401 || msg.contains("401", ignoreCase = true) ->
        "خطای احراز هویت (401 Unauthorized): رمز Pairing PIN اشتباه است یا با کد ۶ رقمی پنجره ویندوز مطابقت ندارد. پنجره برنامه را در کامپیوتر باز کرده و کد ۶ رقمی Pairing PIN فعلی را بررسی و وارد کنید."
      responseCode == 429 || msg.contains("429", ignoreCase = true) ->
        "به دلیل ۵ تلاش ناموفق، سیستم برای ۶۰ ثانیه قفل شده است. لطفاً یک دقیقه صبر کرده و سپس مجدداً تلاش کنید."
      responseCode == 404 || msg.contains("404", ignoreCase = true) ->
        "مسیر وب‌سوکت /input روی سرور ویندوز پیدا نشد (HTTP 404 Not Found)."
      msg.contains("ECONNREFUSED", ignoreCase = true) || msg.contains("Connection refused", ignoreCase = true) ->
        "کامپیوتر ارتباط را رد کرد (Connection refused). مطمئن شوید برنامه روی ویندوز ۱۰ باز است و پورت $currentPort فعال است."
      msg.contains("EHOSTUNREACH", ignoreCase = true) || msg.contains("No route to host", ignoreCase = true) ->
        "مسیر شبکه یافت نشد (No route to host). بررسی کنید گوشی و لپ‌تاپ به یک شبکه Wi-Fi متصل باشند و AP Isolation خاموش باشد."
      msg.contains("ETIMEDOUT", ignoreCase = true) || msg.contains("timed out", ignoreCase = true) || e is SocketTimeoutException ->
        "زمان انتظار سوکت به پایان رسید (Timeout). احتمالاً فایروال ویندوز جلوی پورت $currentPort را گرفته یا IP نادرست است."
      msg.contains("UnknownHost", ignoreCase = true) || e is UnknownHostException ->
        "نام میزبان یا آدرس IP نامعتبر است و در شبکه حل نشد."
      msg.contains("ENETUNREACH", ignoreCase = true) || msg.contains("Network is unreachable", ignoreCase = true) ->
        "شبکه در دسترس نیست. اتصال Wi-Fi یا کابل USB گوشی را بررسی کنید."
      msg.contains("EPERM", ignoreCase = true) ->
        "سیستم‌عامل اجازه ارسال سوکت را نداد. VPN گوشی را خاموش کنید."
      else -> "خطای سوکت در ارتباط با $currentHost:$currentPort. اتصال وای‌فای و باز بودن برنامه در ویندوز را چک کنید."
    }
  }

  /**
   * Parse Giro Air Mouse addressing schemes:
   * e.g. "http://192.168.1.5:8620", "192.168.1.5:8620", "192.168.1.5", "ws://192.168.1.5:8620/input"
   */
  fun parseAddress(rawInput: String): Pair<String, Int> {
    var cleaned = rawInput.trim()
    if (cleaned.startsWith("http://")) cleaned = cleaned.removePrefix("http://")
    if (cleaned.startsWith("https://")) cleaned = cleaned.removePrefix("https://")
    if (cleaned.startsWith("ws://")) cleaned = cleaned.removePrefix("ws://")
    if (cleaned.startsWith("wss://")) cleaned = cleaned.removePrefix("wss://")

    // Remove any trailing path e.g. "/input" or "/"
    if (cleaned.contains("/")) {
      cleaned = cleaned.substringBefore("/")
    }

    return if (cleaned.contains(":")) {
      val host = cleaned.substringBefore(":")
      val port = cleaned.substringAfter(":").toIntOrNull() ?: 8620
      Pair(host, port)
    } else {
      Pair(cleaned, 8620) // Default to server port 8620
    }
  }

  // Sub-pixel delta accumulation for smooth integer streaming to WinDock
  private var accumulatedDx: Float = 0f
  private var accumulatedDy: Float = 0f

  @Synchronized
  fun connect(rawAddress: String, pin: String = "", forcedPort: Int? = null) {
    disconnect()

    val (host, parsedPort) = parseAddress(rawAddress)
    currentHost = host
    currentPort = forcedPort ?: parsedPort
    currentPin = pin.trim()
    targetPort = currentPort

    _status.value = ConnectionStatus.CONNECTING
    _lastError.value = null

    log(
      LogLevel.INFO,
      "Connect",
      "شروع فرآیند اتصال به $currentHost:$currentPort",
      details = "آدرس خام: $rawAddress | PIN: ${if (currentPin.isNotEmpty()) currentPin else "(خالی)"}"
    )

    coroutineScope.launch(Dispatchers.IO) {
      // Step 1: Probe TCP reachability on the port
      val tcpOk = probeTcpSocket(currentHost, currentPort)

      // Step 2: Check if cookie already exists in WebView CookieManager or authenticate via PIN
      val existingCookie = extractCookieFromStorage(currentHost, currentPort)
      if (existingCookie != null) {
        sessionCookie = existingCookie
        log(LogLevel.SUCCESS, "Auth", "کوکی نشست معتبر در حافظه یافت شد.")
      } else if (currentPin.isNotEmpty()) {
        val authOk = authenticateWithPin(currentHost, currentPort, currentPin)
        if (!authOk) {
          log(
            LogLevel.WARN,
            "Connect",
            "کد Pairing PIN توسط ویندوز تایید نشد (۴۰۱). در صورت تغییر PIN در ویندوز، کد ۶ رقمی جدید را وارد فرمایید."
          )
        }
      }

      // Step 3: Connect Giro Air Mouse WebSocket to /input
      connectGiroAirMouseWebSocket(currentHost, currentPort)

      // Step 4: Connect low-latency UDP socket alongside
      connectUdpSocket(currentHost, currentPort)

      startPinging()
    }
  }

  private fun extractCookieFromStorage(host: String, port: Int): String? {
    try {
      val cookieManager = android.webkit.CookieManager.getInstance()
      val urlHttp = "http://$host:$port"
      val rawCookies = cookieManager.getCookie(urlHttp)
      if (!rawCookies.isNullOrEmpty()) {
        for (part in rawCookies.split(";")) {
          val clean = part.trim()
          if (clean.startsWith("windock_session=") || clean.startsWith("__Host-windock_session=")) {
            return clean
          }
        }
      }
    } catch (_: Exception) {}
    return sessionCookie
  }

  fun syncCookieFromWebView(): Boolean {
    val cookie = extractCookieFromStorage(currentHost, currentPort)
    if (cookie != null && cookie != sessionCookie) {
      sessionCookie = cookie
      log(LogLevel.SUCCESS, "CookieSync", "کوکی نشست ویندوز از مرورگر داخلی همگام‌سازی شد.")
      if (!isWsConnected) {
        coroutineScope.launch(Dispatchers.IO) {
          connectGiroAirMouseWebSocket(currentHost, currentPort)
        }
      }
      return true
    }
    return false
  }

  private fun probeTcpSocket(host: String, port: Int): Boolean {
    var socket: Socket? = null
    return try {
      log(LogLevel.INFO, "TCP-Probe", "بررسی باز بودن پورت سوکت TCP در $host:$port...")
      socket = Socket()
      socket.connect(InetSocketAddress(host, port), 2500)
      log(LogLevel.SUCCESS, "TCP-Probe", "پورت سوکت $port روی $host باز و در دسترس است.")
      true
    } catch (e: Exception) {
      val hint = getDiagnosticHint(e)
      log(
        LogLevel.ERROR,
        "TCP-Probe",
        "خطای سوکت TCP در دسترسی به $host:$port: ${e.message}",
        details = e.toString(),
        diagnosticHint = hint
      )
      false
    } finally {
      try { socket?.close() } catch (_: Exception) {}
    }
  }

  fun authenticateWithPin(host: String, port: Int, pin: String): Boolean {
    val cleanPin = pin.trim()
    if (cleanPin.isEmpty()) {
      log(
        LogLevel.WARN,
        "HTTP-Auth",
        "کد Pairing PIN وارد نشده است. در صورت نیاز کد ۶ رقمی پنجره ویندوز را وارد کنید."
      )
      return false
    }

    try {
      log(LogLevel.INFO, "HTTP-Auth", "ارسال درخواست احراز هویت با PIN ($cleanPin) به http://$host:$port/api/auth...")
      val json = "{\"pin\":\"$cleanPin\"}"
      val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
      val request = Request.Builder()
        .url("http://$host:$port/api/auth")
        .addHeader("Origin", "http://$host:$port")
        .addHeader("Host", "$host:$port")
        .post(body)
        .build()

      val response = httpClient.newCall(request).execute()
      val code = response.code
      val respBody = response.body?.string() ?: ""

      if (response.isSuccessful) {
        val setCookieHeaders = response.headers("Set-Cookie")
        var sessionTokenFound = false
        for (sc in setCookieHeaders) {
          val cookieClean = sc.substringBefore(";").trim()
          if (cookieClean.contains("windock_session")) {
            sessionCookie = cookieClean
            sessionTokenFound = true
            break
          }
        }
        if (!sessionTokenFound && setCookieHeaders.isNotEmpty()) {
          sessionCookie = setCookieHeaders[0].substringBefore(";").trim()
        }

        // Also inject into Android WebView CookieManager
        try {
          val cookieManager = android.webkit.CookieManager.getInstance()
          cookieManager.setAcceptCookie(true)
          cookieManager.setCookie("http://$host:$port", sessionCookie ?: "")
          cookieManager.flush()
        } catch (_: Exception) {}

        log(
          LogLevel.SUCCESS,
          "HTTP-Auth",
          "احراز هویت موفق با PIN (کد HTTP 200). کوکی نشست ویندوز دریافت شد."
        )
        response.close()

        // Reconnect WebSocket if needed
        if (!isWsConnected) {
          connectGiroAirMouseWebSocket(host, port)
        }
        return true
      } else {
        val hint = when {
          code == 401 -> "رمز Pairing PIN اشتباه است! لطفاً کد ۶ رقمی فعلی نمایش‌داده‌شده در پنجره کامپیوتر را بررسی و وارد کنید."
          code == 429 -> "به دلیل ۵ تلاش اشتباه، ورود به مدت ۶۰ ثانیه قفل شده است. لطفاً یک دقیقه صبر کرده و مجدداً تلاش کنید."
          code == 400 -> "کد PIN باید دقیقاً یک عدد ۶ رقمی باشد."
          code == 403 -> "ویندوز درخواست را رد کرد (خطای Origin یا Host)."
          else -> "پاسخ خطای HTTP $code از سرور ویندوز: $respBody"
        }
        log(
          LogLevel.ERROR,
          "HTTP-Auth",
          "رد احراز هویت با کد PIN (HTTP $code): $respBody",
          details = respBody,
          diagnosticHint = hint
        )
        _lastError.value = if (code == 401) "کد PIN اشتباه است (۴۰۱)" else "خطای احراز هویت (HTTP $code)"
        response.close()
        return false
      }
    } catch (e: Exception) {
      log(
        LogLevel.ERROR,
        "HTTP-Auth",
        "خطای شبکه در درخواست احراز هویت PIN: ${e.message}",
        details = e.message,
        diagnosticHint = getDiagnosticHint(e)
      )
      return false
    }
  }

  fun connectGiroAirMouseWebSocket(host: String, port: Int) {
    try {
      // Close previous socket if open
      try {
        webSocket?.close(1000, "Reconnecting")
      } catch (_: Exception) {}
      webSocket = null
      isWsConnected = false

      val wsUrl = "ws://$host:$port/input"
      log(LogLevel.INFO, "WebSocket", "در حال برقراری ارتباط با سوکت ورودی $wsUrl...")

      val reqBuilder = Request.Builder()
        .url(wsUrl)
        .addHeader("Origin", "http://$host:$port")
        .addHeader("Host", "$host:$port")

      // Extract cookie if available
      val cookie = sessionCookie ?: extractCookieFromStorage(host, port)
      if (cookie != null) {
        val cleanCookie = cookie.substringBefore(";").trim()
        reqBuilder.addHeader("Cookie", cleanCookie)
      }

      webSocket = httpClient.newWebSocket(reqBuilder.build(), object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          log(LogLevel.SUCCESS, "WebSocket", "سوکت وب ورودی متصل شد (HTTP ${response.code}). در انتظار تایید ready...")
          this@MouseClient.webSocket = webSocket
          // Note: WinDock confirms session readiness with {"type":"ready"} message
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
          Log.d(TAG, "WS message: $text")
          if (text.contains("\"ready\"")) {
            isWsConnected = true
            _status.value = ConnectionStatus.CONNECTED
            _lastError.value = null
            _activeProtocolName.value = "Giro Air Mouse (:8620)"
            log(LogLevel.SUCCESS, "WebSocket", "کانال ورودی ماوس با کامپیوتر کاملاً فعال و آماده شد (Ready ✓)")
          } else if (text.contains("\"error\"")) {
            log(LogLevel.WARN, "WebSocket", "پیام وضعیت از کامپیوتر: $text")
          }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          val hint = getDiagnosticHint(t, response?.code)
          val errDesc = if (response != null) "HTTP ${response.code} ${response.message} (${t.message})" else "${t.message}"
          log(
            LogLevel.ERROR,
            "WebSocket",
            "خطای اتصال سوکت ورودی /input: $errDesc",
            details = t.toString(),
            diagnosticHint = hint
          )
          isWsConnected = false
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          log(LogLevel.WARN, "WebSocket", "سوکت بسته شد (کد: $code, علت: $reason)")
          isWsConnected = false
        }
      })
    } catch (e: Exception) {
      log(
        LogLevel.ERROR,
        "WebSocket",
        "خطا در ایجاد درخواست سوکت: ${e.message}",
        details = e.message,
        diagnosticHint = getDiagnosticHint(e)
      )
    }
  }

  private fun connectUdpSocket(host: String, port: Int) {
    try {
      log(LogLevel.INFO, "UDP-Socket", "آماده‌سازی سوکت UDP برای کنترل مکمل ماوس روی پورت $port...")
      val s = DatagramSocket()
      s.soTimeout = 2000
      val addr = InetAddress.getByName(host)
      udpSocket = s
      targetAddress = addr

      log(LogLevel.SUCCESS, "UDP-Socket", "سوکت UDP باز شد (Local: ${s.localPort} -> Remote: $addr:$port)")
      startUdpReceiving(s)

      // Send initial UDP probe
      val pingBytes = "PING:${System.currentTimeMillis()}".toByteArray()
      s.send(DatagramPacket(pingBytes, pingBytes.size, addr, port))
      _packetsSent.value++
    } catch (e: Exception) {
      val hint = getDiagnosticHint(e)
      log(
        LogLevel.ERROR,
        "UDP-Socket",
        "خطای راه‌اندازی سوکت UDP: ${e.message}",
        details = e.toString(),
        diagnosticHint = hint
      )
      if (!isWsConnected) {
        _lastError.value = e.message ?: "اتصال برقرار نشد"
      }
    }
  }

  @Synchronized
  fun disconnect() {
    pingJob?.cancel()
    pingJob = null
    receiveJob?.cancel()
    receiveJob = null

    try {
      webSocket?.close(1000, "User disconnected")
    } catch (_: Exception) {}
    webSocket = null
    isWsConnected = false

    try {
      udpSocket?.close()
    } catch (_: Exception) {}
    udpSocket = null
    targetAddress = null

    accumulatedDx = 0f
    accumulatedDy = 0f

    _status.value = ConnectionStatus.DISCONNECTED
    log(LogLevel.INFO, "Disconnect", "ارتباط با کامپیوتر قطع شد.")
  }

  /**
   * Send gyroscope / pointer delta movement.
   * WinDock validation strictly requires integer values: {"type":"move","dx":int,"dy":int}
   * Fractional components are accumulated and preserved for high-precision smooth control.
   */
  fun sendMove(dx: Float, dy: Float) {
    if (udpSocket == null && !isWsConnected) return

    accumulatedDx += dx
    accumulatedDy += dy

    val intDx = accumulatedDx.toInt()
    val intDy = accumulatedDy.toInt()

    if (intDx != 0 || intDy != 0) {
      accumulatedDx -= intDx
      accumulatedDy -= intDy

      val clampedDx = intDx.coerceIn(-2048, 2048)
      val clampedDy = intDy.coerceIn(-2048, 2048)

      // 1. Send via WebSocket (Strict WinDock integer JSON protocol)
      if (isWsConnected) {
        val json = "{\"type\":\"move\",\"dx\":$clampedDx,\"dy\":$clampedDy}"
        webSocket?.send(json)
        _packetsSent.value++
      }

      // 2. Also send via UDP socket
      sendUdpPacket("M:$clampedDx:$clampedDy")
    }
  }

  /**
   * Send Mouse Button Down.
   * WinDock schema: {"type":"button","button":"left"|"right","down":true}
   */
  fun sendMouseDown(button: String) {
    val btn = if (button == "R") "right" else "left"

    if (isWsConnected) {
      webSocket?.send("{\"type\":\"button\",\"button\":\"$btn\",\"down\":true}")
      _packetsSent.value++
    }

    sendUdpPacket("MD:$button")
  }

  /**
   * Send Mouse Button Up.
   * WinDock schema: {"type":"button","button":"left"|"right","down":false}
   */
  fun sendMouseUp(button: String) {
    val btn = if (button == "R") "right" else "left"

    if (isWsConnected) {
      webSocket?.send("{\"type\":\"button\",\"button\":\"$btn\",\"down\":false}")
      _packetsSent.value++
    }

    sendUdpPacket("MU:$button")
  }

  /**
   * Send complete Mouse Click (Down followed by Up).
   */
  fun sendClick(button: String) {
    val btn = if (button == "R") "right" else "left"

    if (isWsConnected) {
      webSocket?.send("{\"type\":\"button\",\"button\":\"$btn\",\"down\":true}")
      _packetsSent.value++
      coroutineScope.launch(Dispatchers.IO) {
        delay(40)
        webSocket?.send("{\"type\":\"button\",\"button\":\"$btn\",\"down\":false}")
        _packetsSent.value++
      }
    }

    sendUdpPacket("C:$button")
  }

  /**
   * Send Wheel Scroll.
   * WinDock schema: {"type":"scroll","dx":0,"dy":int}
   */
  fun sendScroll(amount: Float) {
    val intAmount = amount.toInt().coerceIn(-2048, 2048)
    if (intAmount == 0) return

    if (isWsConnected) {
      webSocket?.send("{\"type\":\"scroll\",\"dx\":0,\"dy\":$intAmount}")
      _packetsSent.value++
    }

    sendUdpPacket("S:$intAmount")
  }

  private fun sendUdpPacket(msg: String) {
    val s = udpSocket ?: return
    val addr = targetAddress ?: return
    val port = targetPort

    coroutineScope.launch(Dispatchers.IO) {
      try {
        val bytes = msg.toByteArray()
        val packet = DatagramPacket(bytes, bytes.size, addr, port)
        s.send(packet)
        _packetsSent.value++
      } catch (e: Exception) {
        log(
          LogLevel.ERROR,
          "UDP-Send",
          "خطا در ارسال پکت UDP ($msg): ${e.message}",
          details = e.message,
          diagnosticHint = getDiagnosticHint(e)
        )
      }
    }
  }

  /**
   * Keep-alive heartbeat: WinDock terminates the session if no packet is received for 2000ms.
   * Exact ping schema: {"type":"ping"} with no extra parameters.
   */
  private fun startPinging() {
    pingJob?.cancel()
    pingJob = coroutineScope.launch(Dispatchers.IO) {
      while (isActive) {
        try {
          if (isWsConnected) {
            webSocket?.send("{\"type\":\"ping\"}")
          }

          val s = udpSocket
          val addr = targetAddress
          if (s != null && addr != null && !s.isClosed) {
            val now = System.currentTimeMillis()
            val msg = "PING:$now".toByteArray()
            s.send(DatagramPacket(msg, msg.size, addr, targetPort))
          }
        } catch (_: Exception) {}
        delay(700) // Keep-alive interval well inside 2000ms lease
      }
    }
  }

  private fun startUdpReceiving(s: DatagramSocket) {
    receiveJob?.cancel()
    receiveJob = coroutineScope.launch(Dispatchers.IO) {
      val buffer = ByteArray(256)
      while (isActive && !s.isClosed) {
        try {
          val packet = DatagramPacket(buffer, buffer.size)
          s.receive(packet)
          val text = String(packet.data, 0, packet.length).trim()
          if (text.startsWith("PONG:")) {
            val sentTime = text.substringAfter("PONG:").toLongOrNull()
            if (sentTime != null) {
              val rtt = (System.currentTimeMillis() - sentTime).coerceAtLeast(1L)
              _latencyMs.value = rtt
            }
            _status.value = ConnectionStatus.CONNECTED
            if (!isWsConnected) {
              _activeProtocolName.value = "UDP Direct (:$targetPort)"
            }
          } else if (text == "PONG") {
            _status.value = ConnectionStatus.CONNECTED
            if (!isWsConnected) {
              _activeProtocolName.value = "UDP Direct (:$targetPort)"
            }
          }
        } catch (_: SocketTimeoutException) {
          // Normal timeout on UDP ping wait
        } catch (e: Exception) {
          if (!s.isClosed) {
            log(
              LogLevel.ERROR,
              "UDP-Receive",
              "خطا در دریافت پاسخ از سوکت UDP: ${e.message}",
              details = e.message,
              diagnosticHint = getDiagnosticHint(e)
            )
          }
        }
      }
    }
  }

  /**
   * Auto-discover Giro Air Mouse and PC server on the local Wi-Fi network.
   * Discovery port is UDP 41234, and server is UDP 8620/5000.
   */
  fun autoDiscoverServer(onFound: (String, Int) -> Unit) {
    coroutineScope.launch(Dispatchers.IO) {
      _status.value = ConnectionStatus.SEARCHING
      log(LogLevel.INFO, "Discovery", "جستجوی خودکار کامپیوتر در شبکه محلی از طریق برودکست UDP...")
      var broadcastSocket: DatagramSocket? = null
      try {
        broadcastSocket = DatagramSocket()
        broadcastSocket.broadcast = true
        broadcastSocket.soTimeout = 3000

        val broadcastAddr = InetAddress.getByName("255.255.255.255")

        // 1. Probe port 41234 (standard discovery port on PC)
        val winDockProbe = "WINDOCK_DISCOVERY".toByteArray()
        broadcastSocket.send(DatagramPacket(winDockProbe, winDockProbe.size, broadcastAddr, 41234))

        val probe41234 = "MOUSE_AIR_DISCOVERY".toByteArray()
        broadcastSocket.send(DatagramPacket(probe41234, probe41234.size, broadcastAddr, 41234))

        // 2. Probe port 8620 and 5000
        val probeMsg = "GYROMOUSE_DISCOVER".toByteArray()
        broadcastSocket.send(DatagramPacket(winDockProbe, winDockProbe.size, broadcastAddr, 8620))
        broadcastSocket.send(DatagramPacket(probeMsg, probeMsg.size, broadcastAddr, 8620))
        broadcastSocket.send(DatagramPacket(probeMsg, probeMsg.size, broadcastAddr, 5000))

        val rxBuf = ByteArray(512)
        val rxPacket = DatagramPacket(rxBuf, rxBuf.size)
        broadcastSocket.receive(rxPacket)

        val response = String(rxPacket.data, 0, rxPacket.length).trim()
        val foundIp = rxPacket.address.hostAddress ?: ""

        if (foundIp.isNotEmpty()) {
          val port = if (response.contains(":")) {
            response.substringAfter(":").toIntOrNull() ?: 8620
          } else {
            8620 // Default server port
          }
          log(LogLevel.SUCCESS, "Discovery", "کامپیوتر ویندوز پیدا شد: $foundIp:$port")
          withContext(Dispatchers.Main) {
            onFound(foundIp, port)
          }
        }
      } catch (e: Exception) {
        val hint = getDiagnosticHint(e)
        log(
          LogLevel.WARN,
          "Discovery",
          "پاسخی از برودکست UDP دریافت نشد: ${e.message}",
          details = e.message,
          diagnosticHint = hint
        )
        withContext(Dispatchers.Main) {
          _status.value = ConnectionStatus.DISCONNECTED
          _lastError.value = "سرور در شبکه یافت نشد. آدرس را به صورت 10.x.x.x:8620 دستی وارد کنید."
        }
      } finally {
        broadcastSocket?.close()
      }
    }
  }

  /**
   * Run an on-demand complete network diagnostic test to identify socket issues.
   */
  fun runDiagnostics(host: String = currentHost, port: Int = currentPort) {
    coroutineScope.launch(Dispatchers.IO) {
      log(LogLevel.INFO, "Diagnostics", "=== شروع تست عیب‌یابی سوکت و شبکه برای $host:$port ===")

      // 1. Host resolution
      try {
        val addr = InetAddress.getByName(host)
        log(LogLevel.SUCCESS, "DNS", "آدرس $host با موفقیت به IP ${addr.hostAddress} ترجمه شد.")
      } catch (e: Exception) {
        log(LogLevel.ERROR, "DNS", "خطا در حل آدرس IP: ${e.message}", details = e.toString(), diagnosticHint = getDiagnosticHint(e))
        return@launch
      }

      // 2. TCP Probe
      val tcpPassed = probeTcpSocket(host, port)

      // 3. HTTP Health Probe
      try {
        log(LogLevel.INFO, "HTTP-Check", "ارسال درخواست HTTP GET به http://$host:$port...")
        val req = Request.Builder()
          .url("http://$host:$port/")
          .addHeader("Origin", "http://$host:$port")
          .get()
          .build()
        val resp = httpClient.newCall(req).execute()
        log(LogLevel.SUCCESS, "HTTP-Check", "پاسخ HTTP از سرور دریافت شد (کد ${resp.code} ${resp.message})")
        resp.close()
      } catch (e: Exception) {
        log(LogLevel.WARN, "HTTP-Check", "سرور وب HTTP پاسخ نداد: ${e.message}", details = e.message, diagnosticHint = getDiagnosticHint(e))
      }

      // 4. UDP Socket Probe
      try {
        val s = DatagramSocket()
        s.soTimeout = 1500
        val target = InetAddress.getByName(host)
        val testBytes = "PING_TEST".toByteArray()
        s.send(DatagramPacket(testBytes, testBytes.size, target, port))
        log(LogLevel.SUCCESS, "UDP-Probe", "پکت تست UDP با موفقیت ارسال شد به $host:$port")
        s.close()
      } catch (e: Exception) {
        log(LogLevel.ERROR, "UDP-Probe", "خطا در ارسال تست UDP: ${e.message}", details = e.toString(), diagnosticHint = getDiagnosticHint(e))
      }

      log(LogLevel.INFO, "Diagnostics", "=== پایان تست عیب‌یابی شبکه ===")
    }
  }
}
