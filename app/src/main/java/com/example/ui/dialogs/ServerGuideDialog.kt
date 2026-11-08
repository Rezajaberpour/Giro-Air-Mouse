package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ServerScripts

@Composable
fun ServerGuideDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Official Server, 1: Python, 2: PowerShell
  var copied by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .heightIn(max = 680.dp)
        .padding(10.dp)
        .testTag("server_guide_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Title Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DesktopWindows,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "راهنمای اتصال Giro Air Mouse به ویندوز ۱۰",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Windows 10 Server Setup & Addressing",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Steps summary
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp)
        ) {
          GuideStepItem(
            number = "۱",
            title = "شبکه Wi-Fi مشترک",
            desc = "گوشی و سیستم ویندوزی باید به یک وای‌فای (یا هات‌اسپات گوشی) وصل باشند."
          )
          Spacer(modifier = Modifier.height(6.dp))
          GuideStepItem(
            number = "۲",
            title = "پورت پیش‌فرض سرور: 8620",
            desc = "آدرس‌دهی به صورت http://192.168.x.x:8620 یا 192.168.x.x:8620 است."
          )
          Spacer(modifier = Modifier.height(6.dp))
          GuideStepItem(
            number = "۳",
            title = "کد PIN در سرور ویندوز",
            desc = "در صورت درخواست PIN، روی آیکون برنامه در تسک‌بار راست‌کلیک کرده و Show connection info را بزنید."
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Script Selector Tabs
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceHighlight)
            .padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val tab0 = selectedTab == 0
          Button(
            onClick = { selectedTab = 0; copied = false },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (tab0) CyberCyan else Color.Transparent,
              contentColor = if (tab0) Color.Black else TextSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1.2f)
          ) {
            Text("سرور رسمی", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          val tab1 = selectedTab == 1
          Button(
            onClick = { selectedTab = 1; copied = false },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (tab1) CyberCyan else Color.Transparent,
              contentColor = if (tab1) Color.Black else TextSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("پایتون (8620)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          val tab2 = selectedTab == 2
          Button(
            onClick = { selectedTab = 2; copied = false },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (tab2) CyberCyan else Color.Transparent,
              contentColor = if (tab2) Color.Black else TextSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("PowerShell", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
          // Official Server Guide Tab
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(12.dp))
              .background(DarkBackground)
              .padding(14.dp)
              .verticalScroll(rememberScrollState())
          ) {
            Text(
              text = "اتصال مستقیم به سرور ویندوز Giro Air Mouse:",
              fontWeight = FontWeight.Bold,
              color = CyberCyan,
              style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "۱. نرم‌افزار سرور را دریافت و روی سیستم ویندوز ۱۰ اجرا کنید.",
              color = TextPrimary,
              style = MaterialTheme.typography.bodySmall,
              lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "۲. بعد از اجرا، سرور روی پورت 8620 فعال می‌شود.\nدر بخش اعلان‌های ویندوز (System Tray)، روی آیکون برنامه راست‌کلیک کرده و گزینه «Show connection info» را بزنید.",
              color = TextPrimary,
              style = MaterialTheme.typography.bodySmall,
              lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "۳. آدرس نمایش داده شده (مثلاً http://192.168.1.5:8620) و کد PIN را در این برنامه وارد کنید.",
              color = TextPrimary,
              style = MaterialTheme.typography.bodySmall,
              lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "۴. همچنین می‌توانید از دکمه «جستجوی خودکار Giro Air Mouse» استفاده کنید تا پورت 41234 و 8620 به طور اتوماتیک اسکن شوند.",
              color = AccentGreen,
              style = MaterialTheme.typography.bodySmall,
              lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                context.startActivity(intent)
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("مشاهده اطلاعات سرور در گیت‌هاب")
            }
          }
        } else {
          // Script Code Box
          val currentScript = if (selectedTab == 1) ServerScripts.PYTHON_SCRIPT else ServerScripts.POWERSHELL_SCRIPT
          val scriptFileName = if (selectedTab == 1) "windows_server.py" else "server.ps1"

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(12.dp))
              .background(DarkBackground)
              .padding(12.dp)
              .verticalScroll(rememberScrollState())
          ) {
            Text(
              text = currentScript,
              style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 14.sp
              ),
              color = CyberCyan.copy(alpha = 0.9f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Action Buttons: Copy, Share
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(scriptFileName, currentScript)
                clipboard.setPrimaryClip(clip)
                copied = true
                Toast.makeText(context, "کد $scriptFileName کپی شد!", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (copied) AccentGreen else CyberCyan,
                contentColor = Color.Black
              ),
              modifier = Modifier.weight(1.4f)
            ) {
              Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (copied) "کپی شد ✓" else "کپی کد اسکریپت",
                fontWeight = FontWeight.Bold
              )
            }

            FilledTonalButton(
              onClick = {
                val sendIntent: Intent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, currentScript)
                  type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "ارسال اسکریپت سرور")
                context.startActivity(shareIntent)
              },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ارسال")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GuideStepItem(
  number: String,
  title: String,
  desc: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = Modifier.fillMaxWidth()
  ) {
    Surface(
      color = CyberCyan,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.size(24.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = number,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = Color.Black
        )
      }
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
      )
      Text(
        text = desc,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        fontSize = 11.sp
      )
    }
  }
}
