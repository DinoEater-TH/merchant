package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OrderStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.GoldenCrisp
import com.example.ui.theme.OrangePrimary
import com.example.util.SoundHelper
import java.util.Locale

@Composable
fun OrderTrackerDialog(viewModel: MainViewModel) {
    val isOpen by viewModel.isTrackerModalOpen.collectAsState()
    if (!isOpen) return

    val orders by viewModel.orders.collectAsState()
    val activeOrderId by viewModel.currentActiveOrderId.collectAsState()
    val order = orders.find { it.id == activeOrderId } ?: orders.firstOrNull()
    val context = LocalContext.current

    if (order == null) return

    Dialog(
        onDismissRequest = { viewModel.closeTrackerModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("order_tracker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🛵 สถานะออเดอร์ #${order.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "สั่งเมื่อ: ${order.timestamp.ifEmpty { "สักครู่" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.closeTrackerModal() }) {
                        Icon(Icons.Default.Close, contentDescription = "ปิด")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Stepper Card
                Surface(
                    color = when (order.status) {
                        OrderStatus.READY -> Color(0xFFE8F5E9)
                        OrderStatus.COOKING -> Color(0xFFFFF8E1)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (order.status) {
                                OrderStatus.PENDING -> "⏳ รอดำเนินการ..."
                                OrderStatus.COOKING -> "🍳 ทางร้านกำลังทอดร้อนๆ กรอบๆ..."
                                OrderStatus.READY -> "🎉 อาหารเสร็จแล้ว พร้อมจัดส่ง/รับหน้าร้าน!"
                                OrderStatus.COMPLETED -> "✅ ส่งมอบอาหารเรียบร้อย อิ่มอร่อยนะคร้าบ!"
                                OrderStatus.CANCELLED -> "❌ ยกเลิกออเดอร์"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = when (order.status) {
                                OrderStatus.READY -> CucumberGreen
                                OrderStatus.COOKING -> OrangePrimary
                                else -> Color.Black
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            StatusStepCircle(
                                icon = "⏳",
                                label = "รอดำเนินการ",
                                isActive = true,
                                isCompleted = order.status.stepIndex >= 1
                            )
                            StatusStepLine(isActive = order.status.stepIndex >= 1)
                            StatusStepCircle(
                                icon = "🍳",
                                label = "กำลังทอด",
                                isActive = order.status.stepIndex >= 1,
                                isCompleted = order.status.stepIndex >= 2
                            )
                            StatusStepLine(isActive = order.status.stepIndex >= 2)
                            StatusStepCircle(
                                icon = "🎉",
                                label = "เสร็จแล้ว",
                                isActive = order.status.stepIndex >= 2,
                                isCompleted = order.status.stepIndex >= 3
                            )
                        }

                        if (order.status == OrderStatus.READY) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CucumberGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "มีเสียงเตือนเมื่ออาหารเสร็จแล้ว!",
                                    fontSize = 11.sp,
                                    color = CucumberGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Status Progress for Demo Testing
                Surface(
                    color = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "🛠️ ปุ่มจำลองการเปลี่ยนสถานะ (Real-time Demo):", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.COOKING) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("กำลังทอด", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.updateOrderStatus(order.id, OrderStatus.READY)
                                    SoundHelper.playOrderReadyChime(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CucumberGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("เสร็จแล้ว!", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // Order summary details
                Text(
                    text = "รายละเอียดเมนูที่สั่ง",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                order.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• ${item.name} x${item.quantity}", fontSize = 13.sp)
                        Text(text = "฿${String.format(Locale.US, "%.0f", item.price * item.quantity)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Options badges
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "📌 ตัวเลือกที่เลือก:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OrangePrimary)
                        Text(text = "- รูปแบบ: ${order.options.skewer.displayName}", fontSize = 12.sp)
                        val extras = mutableListOf<String>()
                        if (order.options.friedChili) extras.add("พริกทอด")
                        if (order.options.cucumber) extras.add("แตงกวา")
                        if (order.options.sauceCup) extras.add("ถ้วยน้ำจิ้ม")
                        Text(text = "- เครื่องเคียง: ${if (extras.isEmpty()) "ไม่รับ" else extras.joinToString(", ")}", fontSize = 12.sp)
                        Text(text = "- น้ำจิ้ม: ${order.options.sauceChoice.displayName}", fontSize = 12.sp)
                        if (order.comment.isNotEmpty()) {
                            Text(
                                text = "💬 หมายเหตุ: \"${order.comment}\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChiliRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ยอดเงินสุทธิ:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "฿${String.format(Locale.US, "%.2f", order.totalAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = OrangePrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Print Thermal Slip or Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.openPrintingOrder(order) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("พิมพ์ใบเสร็จ")
                    }

                    Button(
                        onClick = { viewModel.closeTrackerModal() },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("กลับหน้าหลัก")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusStepCircle(
    icon: String,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = when {
                isCompleted -> CucumberGreen
                isActive -> OrangePrimary
                else -> Color(0xFFDDDDDD)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text(text = icon, fontSize = 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.Black else Color.Gray
        )
    }
}

@Composable
fun StatusStepLine(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(2.dp)
            .background(if (isActive) CucumberGreen else Color(0xFFDDDDDD))
            .padding(bottom = 14.dp)
    )
}
