package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary

@Composable
fun PosMenuView(viewModel: MainViewModel) {
    val menuItems by viewModel.menuItems.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val cart = remember { mutableStateMapOf<String, Int>() }
    var selectedCategory by remember { mutableStateOf("all") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // map categoryId -> display name (Thai). Fall back to id when no category found.
    fun categoryName(id: String): String {
        val c = categoriesState.find { it.id == id }
        return c?.name?.takeIf { it.isNotBlank() } ?: id
    }
    val categories = remember(menuItems) {
        menuItems.map { it.categoryId }.distinct().filter { it.isNotBlank() && it != "all" }
    }
    val filtered = remember(menuItems, selectedCategory) {
        menuItems.filter { it.status.name != "HIDDEN" && (selectedCategory == "all" || it.categoryId == selectedCategory) }
    }
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Category chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == "all",
                onClick = { selectedCategory = "all" },
                label = { Text("ทั้งหมด", fontSize = 11.sp) }
            )
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(categoryName(cat), fontSize = 11.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            // Never leave the screen blank: explain what is happening.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (menuItems.isEmpty()) "ยังไม่มีข้อมูลเมนูในเครื่อง" else "ไม่มีเมนูในหมวดนี้",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = syncStatus,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.retrySync() }) {
                        Text("เชื่อมต่อ Firebase ใหม่", fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Menu list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { m ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = m.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "฿${m.price} / ${m.saleUnit}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val q = cart[m.id] ?: 0
                                    if (q > 0) cart[m.id] = q - 1
                                    if (cart[m.id] == 0) cart.remove(m.id)
                                }) {
                                    Icon(Icons.Default.Remove, contentDescription = "ลด")
                                }
                                Text(
                                    text = "${cart[m.id] ?: 0}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { cart[m.id] = (cart[m.id] ?: 0) + 1 }) {
                                    Icon(Icons.Default.Add, contentDescription = "เพิ่ม")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live order detail: updates the moment an item is added or removed,
        // so the shop never has to open a separate review screen.
        Spacer(modifier = Modifier.height(8.dp))
        val preview = viewModel.posOrderPreview(cart.toMap(), phone)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "สรุปออเดอร์ (${preview?.totalItemsCount ?: 0} ชิ้น)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (preview == null) {
                    Text(
                        text = "ยังไม่ได้เลือกรายการ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    preview.orderItems.forEach { oi ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = oi.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "฿${fmtBaht(oi.price)} x ${oi.quantity}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "฿${fmtBaht(oi.price * oi.quantity)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "โปรโมชั่นที่ใช้ได้", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (preview.appliedPromos.isEmpty()) {
                        Text(
                            text = "ไม่มีโปรโมชั่นที่เข้าเงื่อนไขในออเดอร์นี้",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        preview.appliedPromos.forEach { p ->
                            Text(text = "• $p", fontSize = 11.sp, color = OrangePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryLine("ยอดรวมสินค้า", "฿${fmtBaht(preview.subtotal)}")
                    SummaryLine(
                        "ส่วนลดรวม",
                        "-฿${fmtBaht(preview.discount)}",
                        valueColor = if (preview.discount > 0) OrangePrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ยอดสุทธิที่ต้องเก็บ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "฿${fmtBaht(preview.totalAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    if (phone.isBlank()) {
                        Text(
                            text = "ลูกค้าทั่วไป (ไม่สะสมแต้ม)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "แต้ม: ${preview.memberStamps} + ${preview.stampsEarned} = ${preview.stampsAfter} ดวง" +
                                if (preview.stampsEarned == 0) " (ยอดยังไม่ถึงเกณฑ์)" else "",
                            fontSize = 11.sp
                        )
                        if (preview.rewardReached) {
                            Text(
                                text = "ถึงเกณฑ์แลกฟรี: " + preview.rewardItemName.ifBlank { preview.rewardDescription }.ifBlank { "เมนูรางวัล" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("เบอร์สมาชิก (ไม่บังคับ)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (cart.isEmpty()) {
                            message = "กรุณาเลือกรายการก่อน"
                            return@Button
                        }
                        val orderId = viewModel.submitPosOrder(cart.toMap(), phone)
                        if (orderId == null) {
                            message = "บันทึกไม่สำเร็จ: ไม่พบรายการเมนู"
                        } else {
                            message = "สร้างออเดอร์ $orderId สำเร็จ ฿${fmtBaht(preview?.totalAmount ?: 0.0)}"
                            cart.clear()
                        }
                    },
                    enabled = preview != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ยืนยันขาย ฿${fmtBaht(preview?.totalAmount ?: 0.0)}")
                }
                if (message.isNotBlank()) {
                    Text(message, fontSize = 12.sp, color = OrangePrimary)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(text = label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 12.sp, color = valueColor)
    }
}

private fun fmtBaht(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)
