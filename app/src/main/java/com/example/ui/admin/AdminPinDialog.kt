package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.OrangePrimary

@Composable
fun AdminPinDialog(viewModel: MainViewModel) {
    val isOpen by viewModel.isPinDialogOpen.collectAsState()
    val adminPin by viewModel.adminPin.collectAsState()
    if (!isOpen) return

    val pinLength = adminPin.length.coerceIn(4, 8)
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { viewModel.closePinDialog() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("admin_pin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "เข้าสู่ระบบจัดการร้านค้า",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "ใส่รหัส PIN",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PIN Digits Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pinLength) {
                        val isFilled = i < pinInput.length
                        Surface(
                            shape = CircleShape,
                            color = if (isFilled) OrangePrimary else Color(0xFFEEEEEE),
                            modifier = Modifier.size(16.dp)
                        ) {}
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = ChiliRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Number Pad (1-9, C, 0, Backspace)
                val keyPad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                keyPad.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .padding(4.dp)
                                    .background(Color(0xFFF7F7F7), CircleShape)
                                    .clickable {
                                        errorMessage = ""
                                        when (digit) {
                                            "C" -> pinInput = ""
                                            "DEL" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                            else -> {
                                                if (pinInput.length < pinLength) {
                                                    val next = pinInput + digit
                                                    pinInput = next
                                                    if (next.length == pinLength) {
                                                        val success = viewModel.verifyPin(next)
                                                        if (!success) {
                                                            errorMessage = "รหัส PIN ไม่ถูกต้อง"
                                                            pinInput = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (digit == "DEL") {
                                    Icon(Icons.Default.Backspace, contentDescription = "ลบ", modifier = Modifier.size(18.dp))
                                } else {
                                    Text(
                                        text = digit,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { viewModel.closePinDialog() }) {
                    Text("ยกเลิก", color = Color.Gray)
                }
            }
        }
    }
}
