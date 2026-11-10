package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary

/**
 * Review dialog shown before a POS sale is confirmed. Lists every item,
 * every applicable promotion, the prices and the member stamp result so the
 * shop can verify everything before tapping "ยืนยันขาย".
 */
@Composable
fun PosOrderReviewDialog(
    review: MainViewModel.PosReview,
    phone: String,
    onConfirm: (note: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "ตรวจสอบออเดอร์ก่อนยืนยันขาย", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ---- Items ----
                SectionCard(title = "รายการ (${review.totalItemsCount} ชิ้น)") {
                    review.orderItems.forEach { oi ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = oi.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "฿${fmt(oi.price)} x ${oi.quantity}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = "฿${fmt(oi.price * oi.quantity)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ---- Promotions ----
                SectionCard(title = "โปรโมชั่นที่ใช้ได้") {
                    if (review.appliedPromos.isEmpty()) {
                        Text(
                            text = "ไม่มีโปรโมชั่นที่เข้าเงื่อนไขในออเดอร์นี้",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        review.appliedPromos.forEach { p ->
                            Text(text = "• $p", fontSize = 12.sp, color = OrangePrimary)
                        }
                    }
                }

                // ---- Summary ----
                SectionCard(title = "สรุปยอดเงิน") {
                    SummaryRow("ยอดรวมสินค้า", "฿${fmt(review.subtotal)}")
                    if (review.discount > 0) {
                        SummaryRow("ส่วนลดรวม", "-฿${fmt(review.discount)}", valueColor = OrangePrimary)
                    } else {
                        SummaryRow("ส่วนลดรวม", "-฿0")
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                        Text(text = "ยอดสุทธิที่ต้องเก็บ", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(text = "฿${fmt(review.totalAmount)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                    }
                }

                // ---- Member / stamps ----
                SectionCard(title = "สมาชิกและแต้มสะสม") {
                    if (phone.isBlank()) {
                        Text(
                            text = "ขายแบบลูกค้าทั่วไป (ไม่สะสมแต้ม)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (review.memberName.isBlank()) "สมาชิก $phone" else "${review.memberName} ($phone)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "แต้มที่มีอยู่ ${review.memberStamps} ดวง + ออเดอร์นี้ ${review.stampsEarned} ดวง = ${review.stampsAfter} ดวง",
                            fontSize = 12.sp
                        )
                        if (review.stampsEarned == 0) {
                            Text(
                                text = "ยอดนี้ยังไม่ถึงเกณฑ์รับแต้ม",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(text = "สะสมครบ ${review.stampsPerReward} ดวง แลกรางวัลได้", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (review.rewardReached) {
                            Text(
                                text = "ถึงเกณฑ์แลกฟรีแล้ว: " + (review.rewardItemName.ifBlank { review.rewardDescription }.ifBlank { "เมนูรางวัล" }),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm("") }) {
                Text("ยืนยันขาย ฿${fmt(review.totalAmount)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("กลับไปแก้ไข") }
        }
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 12.sp, color = valueColor)
    }
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)
