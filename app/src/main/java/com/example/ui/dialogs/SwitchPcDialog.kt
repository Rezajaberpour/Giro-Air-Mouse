package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.SavedPc

// "Switch PC" dialog for Giro Air Mouse
@Composable
fun SwitchPcDialog(
  savedPcs: List<SavedPc>,
  onSelectPc: (SavedPc) -> Unit,
  onForgetPc: (String) -> Unit,
  onEnterAddress: () -> Unit,
  onRefresh: () -> Unit,
  onDismiss: () -> Unit
) {
  val tealAccent = Color(0xFF4DB6AC)
  val dialogBg = Color(0xFF141A21)
  val cardBg = Color(0xFF1E252E)
  val forgetBtnBg = Color(0xFF2C3540)

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .testTag("switch_pc_dialog"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = dialogBg)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp)
      ) {
        Text(
          text = "Switch PC",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Choose a PC. New connections may ask for its pairing code.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF9EAAAF),
          fontSize = 13.sp,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Available now",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        // PC List
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(savedPcs) { pc ->
            val isCurrent = pc.isCurrent
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                  width = if (isCurrent) 1.5.dp else 1.dp,
                  color = if (isCurrent) tealAccent else Color(0xFF333C48),
                  shape = RoundedCornerShape(12.dp)
                )
                .background(cardBg)
                .clickable {
                  onSelectPc(pc)
                  onDismiss()
                }
                .padding(14.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = pc.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                  )
                  Spacer(modifier = Modifier.height(3.dp))
                  Text(
                    text = "Windows • ${pc.connectionType} • ${if (isCurrent) "Current PC" else "Available"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = pc.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCFD8DC),
                    fontSize = 12.sp
                  )
                }

                Button(
                  onClick = { onForgetPc(pc.id) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = forgetBtnBg,
                    contentColor = Color.White
                  ),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                  Text("Forget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Bottom Actions matching screenshot
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onEnterAddress) {
            Text(
              text = "ENTER ADDRESS",
              color = tealAccent,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }

          Row {
            TextButton(onClick = onDismiss) {
              Text(
                text = "CLOSE",
                color = tealAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = onRefresh) {
              Text(
                text = "REFRESH",
                color = tealAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
          }
        }
      }
    }
  }
}
