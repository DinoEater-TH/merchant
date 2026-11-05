package com.example.ui.customer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.MainViewModel
import coil.compose.AsyncImage
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.OrangePrimary
import com.example.util.PromptPayQrView
import java.util.Locale

@Composable
fun PromptPayDialog(viewModel: MainViewModel) {
    val isOpen by viewModel.isPaymentModalOpen.collectAsState()
    if (!isOpen) return

    val shopInfo by viewModel.shopInfo.collectAsState()
    val cartSummary by viewModel.cartSummary.collectAsState()
    val slipImageUri by viewModel.slipImageUri.collectAsState()
    val context = LocalContext.current
    val hasSlip = slipImageUri != null

    val slipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setSlipImage(uri.toString())
        }
    }

    Dialog(
        onDismissRequest = { viewModel.closePaymentModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("promptpay_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF1E3A8A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "PP", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "ชำระเงินผ่าน พร้อมเพย์ (PromptPay)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = shopInfo.promptPayName, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = { viewModel.closePaymentModal() }) {
                        Icon(Icons.Default.Close, contentDescription = "ปิด")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PromptPay QR Code with precise amount
                PromptPayQrView(
                    promptPayNumber = shopInfo.promptPayNumber,
                    amount = cartSummary.totalAmount,
                    sizeDp = 180.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount display
                Text(
                    text = "ยอดชำระสุทธิ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "฿${String.format(Locale.US, "%.2f", cartSummary.totalAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = OrangePrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Phone copy box
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "เบอร์พร้อมเพย์ร้านค้า", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = shopInfo.promptPayNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PromptPay", shopInfo.promptPayNumber.filter { it.isDigit() })
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "คัดลอกเบอร์พร้อมเพย์แล้ว", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "คัดลอกเบอร์พร้อมเพย์", tint = OrangePrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))

                // Slip Upload Section
                Text(
                    text = "หลักฐานการชำระเงิน (แนบสลิป)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { slipPicker.launch(arrayOf("image/*")) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasSlip) Color(0xFFE8F5E9) else Color(0xFFF9F9F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasSlip) CucumberGreen else Color(0xFFDDDDDD)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasSlip) {
                            AsyncImage(
                                model = slipImageUri,
                                contentDescription = "สลิปการโอนเงิน",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasSlip) "แนบสลิปเรียบร้อยแล้ว" else "กดเพื่อเลือกไฟล์สลิปจากเครื่อง",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (hasSlip) CucumberGreen else Color.Black
                            )
                            Text(
                                text = if (hasSlip) "แตะเพื่อเปลี่ยนไฟล์สลิป" else "รูปภาพสลิปโอนเงิน (JPEG / PNG)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        if (hasSlip) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "แนบสลิปแล้ว",
                                tint = CucumberGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        viewModel.confirmOrderAndSubmit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_order_button")
                ) {
                    Text(text = "ยืนยันการชำระเงินและสั่งอาหาร", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
