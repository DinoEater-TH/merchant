package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
    var showReview by remember { mutableStateOf(false) }

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
    val review = if (showReview) viewModel.posOrderPreview(cart.toMap(), phone) else null

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

        // Cart summary
        Spacer(modifier = Modifier.height(8.dp))
        val preview = viewModel.posOrderPreview(cart.toMap(), phone)
        val subtotal = preview?.subtotal ?: 0.0
        val discount = preview?.discount ?: 0.0
        val total = preview?.totalAmount ?: 0.0
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ยอดรวม: ฿${String.format("%.0f", total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OrangePrimary
                )
                if (discount > 0) {
                    Text(
                        text = "ส่วนลด: -฿${String.format("%.0f", discount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        message = ""
                        showReview = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ตรวจสอบออเดอร์")
                }
                if (message.isNotBlank()) {
                    Text(message, fontSize = 12.sp, color = OrangePrimary)
                }
            }
        }

        // Review dialog: items, promotions, prices and stamps before confirming
        if (showReview && review != null) {
            PosOrderReviewDialog(
                review = review,
                phone = phone,
                onConfirm = { _ ->
                    val orderId = viewModel.submitPosOrder(cart.toMap(), phone)
                    showReview = false
                    if (orderId == null) {
                        message = "บันทึกไม่สำเร็จ: ไม่พบรายการเมนู"
                    } else {
                        message = "สร้างออเดอร์ $orderId สำเร็จ ฿${String.format("%.0f", review.totalAmount)}"
                        cart.clear()
                    }
                },
                onDismiss = { showReview = false }
            )
        }
    }
}
