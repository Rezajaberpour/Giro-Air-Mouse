package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ButtonAction
import com.example.model.MouseConfig
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  currentConfig: MouseConfig,
  onSaveConfig: (MouseConfig) -> Unit,
  onCalibrate: () -> Unit,
  onDismiss: () -> Unit
) {
  var config by remember { mutableStateOf(currentConfig) }
  var volUpExpanded by remember { mutableStateOf(false) }
  var volDownExpanded by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .heightIn(max = 680.dp)
        .padding(12.dp)
        .testTag("settings_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "تنظیمات موس و ژیروسکوپ",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable settings body
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Hardware buttons mapping section
          Text(
            text = "تخصیص دکمه‌های ولوم سخت‌افزاری",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
          )

          // Volume Intercept Switch
          SettingToggleRow(
            title = "فعال‌سازی کنترل با دکمه‌های ولوم",
            subtitle = "جلوگیری از تغییر صدای گوشی هنگام استفاده از موس",
            checked = config.volumeInterceptEnabled,
            onCheckedChange = { config = config.copy(volumeInterceptEnabled = it) }
          )

          // Volume Up Action Dropdown
          ExposedDropdownMenuBox(
            expanded = volUpExpanded,
            onExpandedChange = { volUpExpanded = !volUpExpanded }
          ) {
            OutlinedTextField(
              value = config.volumeUpAction.labelFa,
              onValueChange = {},
              readOnly = true,
              label = { Text("عملکرد دکمه ولوم بالا (Vol Up)") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = volUpExpanded) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
              colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
              expanded = volUpExpanded,
              onDismissRequest = { volUpExpanded = false },
              modifier = Modifier.background(DarkSurfaceVariant)
            ) {
              ButtonAction.values().forEach { action ->
                DropdownMenuItem(
                  text = { Text("${action.labelFa} (${action.labelEn})") },
                  onClick = {
                    config = config.copy(volumeUpAction = action)
                    volUpExpanded = false
                  }
                )
              }
            }
          }

          // Volume Down Action Dropdown
          ExposedDropdownMenuBox(
            expanded = volDownExpanded,
            onExpandedChange = { volDownExpanded = !volDownExpanded }
          ) {
            OutlinedTextField(
              value = config.volumeDownAction.labelFa,
              onValueChange = {},
              readOnly = true,
              label = { Text("عملکرد دکمه ولوم پایین (Vol Down)") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = volDownExpanded) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
              colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
              expanded = volDownExpanded,
              onDismissRequest = { volDownExpanded = false },
              modifier = Modifier.background(DarkSurfaceVariant)
            ) {
              ButtonAction.values().forEach { action ->
                DropdownMenuItem(
                  text = { Text("${action.labelFa} (${action.labelEn})") },
                  onClick = {
                    config = config.copy(volumeDownAction = action)
                    volDownExpanded = false
                  }
                )
              }
            }
          }

          HorizontalDivider(color = DarkSurfaceHighlight)

          // Sensitivity Section
          Text(
            text = "حساسیت و شتاب ژیروسکوپ",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
          )

          // Horizontal Sensitivity (X)
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("حساسیت افقی (محور X)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              Text("%.1f".format(config.sensitivityX), style = MaterialTheme.typography.bodyMedium, color = CyberCyan, fontWeight = FontWeight.Bold)
            }
            Slider(
              value = config.sensitivityX,
              onValueChange = { config = config.copy(sensitivityX = it) },
              valueRange = 0.5f..5.0f,
              steps = 17,
              colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )
          }

          // Vertical Sensitivity (Y)
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("حساسیت عمودی (محور Y)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              Text("%.1f".format(config.sensitivityY), style = MaterialTheme.typography.bodyMedium, color = CyberCyan, fontWeight = FontWeight.Bold)
            }
            Slider(
              value = config.sensitivityY,
              onValueChange = { config = config.copy(sensitivityY = it) },
              valueRange = 0.5f..5.0f,
              steps = 17,
              colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )
          }

          // Smoothing Filter
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("نرمی حرکت (Smoothing)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              Text("%.2f".format(config.smoothing), style = MaterialTheme.typography.bodyMedium, color = CyberCyan, fontWeight = FontWeight.Bold)
            }
            Slider(
              value = config.smoothing,
              onValueChange = { config = config.copy(smoothing = it) },
              valueRange = 0.2f..0.9f,
              colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )
          }

          // Invert Axis Toggles
          SettingToggleRow(
            title = "معکوس کردن جهت افقی (Invert X)",
            subtitle = "حرکت معکوس در محور چپ و راست",
            checked = config.invertX,
            onCheckedChange = { config = config.copy(invertX = it) }
          )

          SettingToggleRow(
            title = "معکوس کردن جهت عمودی (Invert Y)",
            subtitle = "حرکت معکوس در محور بالا و پایین",
            checked = config.invertY,
            onCheckedChange = { config = config.copy(invertY = it) }
          )

          HorizontalDivider(color = DarkSurfaceHighlight)

          // Other Preferences
          Text(
            text = "تنظیمات عمومی",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
          )

          SettingToggleRow(
            title = "لرزش و فیدبک لمسی (Haptic)",
            subtitle = "ویبره هنگام کلیک روی دکمه‌ها و ولوم",
            checked = config.hapticsEnabled,
            onCheckedChange = { config = config.copy(hapticsEnabled = it) }
          )

          SettingToggleRow(
            title = "روشن نگه داشتن صفحه",
            subtitle = "جلوگیری از خاموش شدن صفحه حین کار با موس",
            checked = config.keepScreenAwake,
            onCheckedChange = { config = config.copy(keepScreenAwake = it) }
          )

          // Calibration Button
          FilledTonalButton(
            onClick = onCalibrate,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("کالیبراسیون و صفر کردن سنسور ژیروسکوپ")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Save & Dismiss Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              onSaveConfig(config)
              onDismiss()
            },
            modifier = Modifier.weight(1f)
          ) {
            Text("ذخیره تنظیمات", fontWeight = FontWeight.Bold)
          }

          FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("انصراف")
          }
        }
      }
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        fontSize = 11.sp
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = DarkSurface,
        checkedTrackColor = CyberCyan
      )
    )
  }
}
