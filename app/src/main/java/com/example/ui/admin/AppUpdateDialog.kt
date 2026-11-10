package com.example.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary
import com.example.util.AppUpdater

/**
 * Shows the "new version available" dialog for the sideloaded shop app.
 * A mandatory update cannot be dismissed.
 */
@Composable
fun AppUpdateDialog(viewModel: MainViewModel) {
    val info by viewModel.updateInfo.collectAsState()
    val status by viewModel.updateStatus.collectAsState()
    val progress by viewModel.updateProgress.collectAsState()
    val dismissed by viewModel.updateDismissed.collectAsState()
    val context = LocalContext.current

    val update = info ?: return
    if (dismissed && !update.mandatory) return

    val downloading = progress in 0..99

    AlertDialog(
        onDismissRequest = { if (!update.mandatory) viewModel.dismissUpdate() },
        title = {
            Text(
                text = "มีเวอร์ชันใหม่ v${update.versionName}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "เวอร์ชันในเครื่องนี้: v${AppUpdater.installedVersionName(context)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (update.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "สิ่งที่แก้ไข:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(text = update.notes, fontSize = 12.sp)
                }
                if (update.mandatory) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "เวอร์ชันนี้จำเป็นต้องอัปเดตก่อนใช้งาน",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
                if (downloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "กำลังดาวน์โหลด... $progress%", fontSize = 12.sp)
                }
                if (status.isNotBlank() && !downloading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = status,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (progress == 100) viewModel.retryUpdateInstall()
                    else viewModel.downloadAndInstallUpdate()
                },
                enabled = !downloading
            ) {
                Text(if (progress == 100) "ติดตั้งอีกครั้ง" else "อัปเดตเลย")
            }
        },
        dismissButton = {
            if (!update.mandatory) {
                TextButton(onClick = { viewModel.dismissUpdate() }) { Text("ไว้ทีหลัง") }
            }
        }
    )
}
