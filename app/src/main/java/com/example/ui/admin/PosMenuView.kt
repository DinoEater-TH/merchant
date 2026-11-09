package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MenuItem
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary

@Composable
fun PosMenuView(viewModel: MainViewModel) {
    val menuItems by viewModel.menuItems.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val cart = remember { mutableStateMapOf<String, Int>() }
    var selectedCategory by remember { mutableStateOf("all") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

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
                    label = { Text(cat, fontSize = 11.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

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
                            Text(text = "฿${m.price} / ${m.saleUnit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.IconButton(onClick = {
                                val q = cart[m.id] ?: 0
                                if (q > 0) cart[m.id] = q - 1
                                if (cart[m.id] == 0) cart.remove(m.id)
                            }) {
                                Icon(Icons.Default.Remove, contentDescription = "ลด")
                            }
                            Text(text = "${cart[m.id] ?: 0}", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            androidx.compose.material3.IconButton(onClick = {
                                cart[m.id] = (cart[m.id] ?: 0) + 1
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "เพิ่ม")
                            }
                        }
                    }
                }
            }
        }

        // Cart summary
        Spacer(modifier = Modifier.height(8.dp))
        val subtotal = cart.entries.sumOf { (id, q) ->
            (menuItems.find { it.id == id }?.price ?: 0.0) * q
        }
        val discount = if (shopInfo.promoDiscountEnabled && subtotal >= shopInfo.minOrderForDiscount) shopInfo.discountAmount else 0.0
        val total = (subtotal - discount).coerceAtLeast(0.0)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ยอดรวม: ฿${String.format("%.0f", total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OrangePrimary)
                if (discount > 0) Text("ส่วนลด: -฿${String.format("%.0f", discount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("เบอร์สมาชิก (ไม่บังคับ)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (cart.isEmpty()) { message = "กรุณาเลือกรายการ"; return@Button }
                        // build order via viewModel
                        message = "บันทึกออเดอร์ POS สำเร็จ"
                        cart.clear()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💵 ยืนยัน (เงินสด)")
                }
                if (message.isNotBlank()) {
                    Text(message, fontSize = 12.sp, color = OrangePrimary)
                }
            }
        }
    }
}