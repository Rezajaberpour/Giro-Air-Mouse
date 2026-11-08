package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ConnectionStatus
import com.example.model.LogLevel
import com.example.model.NetworkLogEntry
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MouseViewModel

enum class LogFilter {
  ALL,
  ERRORS,
  WEBSOCKET,
  UDP
}

@Composable
fun NetworkLogsDialog(
  viewModel: MouseViewModel,
  onDismiss: () -> Unit
) {
  val logs by viewModel.networkLogs.collectAsState()
  val connInfo by viewModel.connectionInfo.collectAsState()
  val context = LocalContext.current

  var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }

  val errorCount = remember(logs) { logs.count { it.level == LogLevel.ERROR } }
  val warnCount = remember(logs) { logs.count { it.level == LogLevel.WARN } }

  val filteredLogs = remember(logs, selectedFilter) {
    when (selectedFilter) {
      LogFilter.ALL -> logs
      LogFilter.ERRORS -> logs.filter { it.level == LogLevel.ERROR || it.level == LogLevel.WARN }
      LogFilter.WEBSOCKET -> logs.filter { it.tag.contains("WS", ignoreCase = true) || it.tag.contains("HTTP", ignoreCase = true) }
      LogFilter.UDP -> logs.filter { it.tag.contains("UDP", ignoreCase = true) || it.tag.contains("TCP", ignoreCase = true) }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF10151E),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243042)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.9f)
        .padding(8.dp)
        .testTag("network_logs_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // 1. Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (errorCount > 0) Color(0x33D32F2F) else Color(0x334DB6AC)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (errorCount > 0) Icons.Default.BugReport else Icons.Default.NetworkCheck,
                contentDescription = null,
                tint = if (errorCount > 0) Color(0xFFEF5350) else Color(0xFF4DB6AC),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "عیب‌یابی شبکه و لاگ سوکت",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "بررسی علت عدم اتصال به ویندوز",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B98A5),
                fontSize = 11.sp
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Status & Diagnostic Summary Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF171F2B)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26354A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "میزبان مقصد (Target Host):",
                  fontSize = 11.sp,
                  color = Color(0xFF8899A6)
                )
                Text(
                  text = "${connInfo.serverIp}:${connInfo.serverPort}",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  fontFamily = FontFamily.Monospace
                )
              }

              // Status Pill
              val statusBg = when (connInfo.status) {
                ConnectionStatus.CONNECTED -> Color(0xFF1B5E20)
                ConnectionStatus.CONNECTING, ConnectionStatus.SEARCHING -> Color(0xFFE65100)
                ConnectionStatus.DISCONNECTED -> Color(0xFFB71C1C)
              }
              val statusText = when (connInfo.status) {
                ConnectionStatus.CONNECTED -> "متصل (Connected)"
                ConnectionStatus.CONNECTING -> "در حال اتصال..."
                ConnectionStatus.SEARCHING -> "در حال جستجو..."
                ConnectionStatus.DISCONNECTED -> "قطع اتصال (Disconnected)"
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(statusBg)
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = statusText,
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            if (connInfo.lastError != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Error,
                  contentDescription = null,
                  tint = Color(0xFFEF5350),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = connInfo.lastError ?: "",
                  color = Color(0xFFFFCDD2),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Run Test, Reconnect, Copy, Clear
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { viewModel.runNetworkDiagnostics() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تست سوکت", fontSize = 12.sp)
              }

              Button(
                onClick = { viewModel.connect(connInfo.rawAddress, connInfo.pin) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("اتصال مجدد", fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = {
                  val allText = logs.joinToString("\n") {
                    "[${it.time}][${it.level}][${it.tag}] ${it.message}${if (it.details != null) " | Details: ${it.details}" else ""}${if (it.diagnosticHint != null) " | Hint: ${it.diagnosticHint}" else ""}"
                  }
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Giro Air Mouse Logs", allText)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "لاگ‌ها کپی شدند", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "کپی لاگ‌ها", tint = Color.White, modifier = Modifier.size(16.dp))
              }

              OutlinedButton(
                onClick = { viewModel.clearNetworkLogs() },
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "پاکسازی", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Filter Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedFilter == LogFilter.ALL,
            onClick = { selectedFilter = LogFilter.ALL },
            label = { Text("همه (${logs.size})", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF2A394D),
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = selectedFilter == LogFilter.ERRORS,
            onClick = { selectedFilter = LogFilter.ERRORS },
            label = {
              Text(
                "خطاها (${errorCount + warnCount})",
                fontSize = 11.sp,
                color = if (errorCount > 0) Color(0xFFFF8A80) else Color.White
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF5C1D24),
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = selectedFilter == LogFilter.WEBSOCKET,
            onClick = { selectedFilter = LogFilter.WEBSOCKET },
            label = { Text("وب‌سوکت/HTTP", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF1D3E4A),
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = selectedFilter == LogFilter.UDP,
            onClick = { selectedFilter = LogFilter.UDP },
            label = { Text("UDP / TCP", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF1D4A3E),
              selectedLabelColor = Color.White
            )
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Logs List
        if (filteredLogs.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF607286), modifier = Modifier.size(40.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "هنوز لاگی در این بخش ثبت نشده است.",
                color = Color(0xFF8899A6),
                fontSize = 13.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "دکمه «تست سوکت» را بزنید تا بررسی آغاز شود.",
                color = Color(0xFF607286),
                fontSize = 11.sp
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(filteredLogs, key = { it.id }) { logItem ->
              LogItemCard(logItem)
            }
          }
        }
      }
    }
  }
}

@Composable
fun LogItemCard(item: NetworkLogEntry) {
  var isExpanded by remember { mutableStateOf(false) }

  val levelColor = when (item.level) {
    LogLevel.ERROR -> Color(0xFFEF5350)
    LogLevel.WARN -> Color(0xFFFFB74D)
    LogLevel.SUCCESS -> Color(0xFF66BB6A)
    LogLevel.INFO -> Color(0xFF4FC3F7)
  }

  val levelBg = when (item.level) {
    LogLevel.ERROR -> Color(0x33EF5350)
    LogLevel.WARN -> Color(0x33FFB74D)
    LogLevel.SUCCESS -> Color(0x3366BB6A)
    LogLevel.INFO -> Color(0x334FC3F7)
  }

  val levelLabel = when (item.level) {
    LogLevel.ERROR -> "ERROR"
    LogLevel.WARN -> "WARN"
    LogLevel.SUCCESS -> "OK"
    LogLevel.INFO -> "INFO"
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B26)),
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (item.level == LogLevel.ERROR) Color(0x55EF5350) else Color(0xFF1E2838)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { isExpanded = !isExpanded }
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Level badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(levelBg)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = levelLabel,
              color = levelColor,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Tag
          Text(
            text = "[${item.tag}]",
            color = Color(0xFF8B9CB0),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
          )
        }

        // Time
        Text(
          text = item.time,
          color = Color(0xFF607286),
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Main Message
      Text(
        text = item.message,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = if (item.level == LogLevel.ERROR) FontWeight.Bold else FontWeight.Normal
      )

      // Diagnostic Hint Box if available (Explains what's wrong and how to fix)
      if (item.diagnosticHint != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = Color(0xFF261D1A),
          shape = RoundedCornerShape(6.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6D3628)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              Icons.Default.Lightbulb,
              contentDescription = null,
              tint = Color(0xFFFFB74D),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = "راهنمای عیب‌یابی:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB74D)
              )
              Text(
                text = item.diagnosticHint,
                fontSize = 11.sp,
                color = Color(0xFFFFE0B2),
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      // Technical Stack Trace / Details (Expandable)
      if (item.details != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isExpanded) "بستن جزئیات فنی" else "مشاهده جزئیات خطا / استک‌تریس",
            color = Color(0xFF81D4FA),
            fontSize = 10.sp
          )
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFF81D4FA),
            modifier = Modifier.size(14.dp)
          )
        }

        AnimatedVisibility(visible = isExpanded) {
          Surface(
            color = Color(0xFF090D13),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp)
          ) {
            Text(
              text = item.details,
              color = Color(0xFFEF9A9A),
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(6.dp)
            )
          }
        }
      }
    }
  }
}
