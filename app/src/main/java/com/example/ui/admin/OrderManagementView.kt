package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.GoldenCrisp
import com.example.ui.theme.OrangePrimary
import java.util.Locale

@Composable
fun OrderManagementView(viewModel: MainViewModel) {
    val orders by viewModel.orders.collectAsState()
    val activeFilter by viewModel.adminOrderFilter.collectAsState()

    val filteredOrders = if (activeFilter == null) {
        orders
    } else {
        orders.filter { it.status == activeFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == null,
                onClick = { viewModel.setAdminOrderFilter(null) },
                label = { Text("ทั้งหมด (${orders.size})") }
            )
            FilterChip(
                selected = activeFilter == OrderStatus.PENDING,
                onClick = { viewModel.setAdminOrderFilter(OrderStatus.PENDING) },
                label = { Text("รอดำเนินการ (${orders.count { it.status == OrderStatus.PENDING }})") }
            )
            FilterChip(
                selected = activeFilter == OrderStatus.COOKING,
                onClick = { viewModel.setAdminOrderFilter(OrderStatus.COOKING) },
                label = { Text("กำลังทำ (${orders.count { it.status == OrderStatus.COOKING }})") }
            )
            FilterChip(
                selected = activeFilter == OrderStatus.READY,
                onClick = { viewModel.setAdminOrderFilter(OrderStatus.READY) },
                label = { Text("เสร็จแล้ว (${orders.count { it.status == OrderStatus.READY }})") }
            )
            FilterChip(
                selected = activeFilter == OrderStatus.COMPLETED,
                onClick = { viewModel.setAdminOrderFilter(OrderStatus.COMPLETED) },
                label = { Text("สำเร็จ (${orders.count { it.status == OrderStatus.COMPLETED }})") }
            )
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ไม่มีรายการออเดอร์ในหมวดนี้",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    AdminOrderCard(
                        order = order,
                        onUpdateStatus = { newStatus -> viewModel.updateOrderStatus(order.id, newStatus) },
                        onRemoveItem = { itemId -> viewModel.removeOrderItem(order.id, itemId) },
                        onDeleteOrder = { viewModel.deleteOrder(order.id) },
                        onPrintSlip = { viewModel.openPrintingOrder(order) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onUpdateStatus: (OrderStatus) -> Unit,
    onRemoveItem: (String) -> Unit = {},
    onDeleteOrder: () -> Unit = {},
    onPrintSlip: () -> Unit
) {
    var awaitingCancelConfirm by androidx.compose.runtime.remember(order.id) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var awaitingDeleteConfirm by androidx.compose.runtime.remember(order.id) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var itemToRemove by androidx.compose.runtime.remember(order.id) {
        androidx.compose.runtime.mutableStateOf<com.example.data.model.OrderItem?>(null)
    }
    val canEditItems = order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_order_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Order ID, Time, Fulfillment, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${order.id}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = OrangePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (order.fulfillment == com.example.data.model.FulfillmentType.DELIVERY) Color(0xFFE3F2FD) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (order.fulfillment == com.example.data.model.FulfillmentType.DELIVERY) "🛵 จัดส่ง" else "🏪 รับหน้าร้าน",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (order.fulfillment == com.example.data.model.FulfillmentType.DELIVERY) Color(0xFF1976D2) else OrangePrimary
                        )
                    }
                }

                Surface(
                    color = when (order.status) {
                        OrderStatus.PENDING -> Color(0xFFFFEBEE)
                        OrderStatus.COOKING -> Color(0xFFFFF8E1)
                        OrderStatus.READY -> Color(0xFFE8F5E9)
                        OrderStatus.COMPLETED -> Color(0xFFEEEEEE)
                        OrderStatus.CANCELLED -> Color(0xFFFFEBEE)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = order.status.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (order.status) {
                            OrderStatus.PENDING -> ChiliRed
                            OrderStatus.COOKING -> OrangePrimary
                            OrderStatus.READY -> CucumberGreen
                            OrderStatus.COMPLETED -> Color.DarkGray
                            OrderStatus.CANCELLED -> Color.Red
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "เวลา: ${order.timestamp.ifEmpty { "12:00" }} | ลูกค้า: ${if (order.isGuest) "Guest (ลูกค้าทั่วไป)" else order.customerPhone}",
                fontSize = 11.sp,
                color = Color.Gray
            )

            if (order.deliveryAddress.isNotEmpty()) {
                Text(
                    text = "📍 ที่อยู่: ${order.deliveryAddress}",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // Order Items
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.name} x${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "฿${String.format(Locale.US, "%.0f", item.price * item.quantity)}",
                            fontSize = 13.sp
                        )
                        if (canEditItems) {
                            androidx.compose.material3.IconButton(
                                onClick = { itemToRemove = item },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("🗑️", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add-ons & Prominent Comment Badges
            Surface(
                color = Color(0xFFFFF9C4), // Vivid soft yellow highlight
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "🍡 เสียบไม้: ${order.options.skewerLabel.ifBlank { order.options.skewer.displayName }} | น้ำจิ้ม: ${order.options.sauceChoice.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val condiments = mutableListOf<String>()
                    if (order.options.friedChili) condiments.add("พริกทอด")
                    if (order.options.cucumber) condiments.add("แตงกวา")
                    if (order.options.sauceCup) condiments.add("ถ้วยน้ำจิ้ม")
                    Text(
                        text = "🥗 ผักเคียง: ${if (condiments.isEmpty()) "ไม่รับ" else condiments.joinToString(", ")}",
                        fontSize = 11.sp
                    )

                    if (order.comment.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "💬 หมายเหตุ: \"${order.comment}\"",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChiliRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "ยอดรวม: ", fontSize = 12.sp)
                    Text(
                        text = "฿${String.format(Locale.US, "%.2f", order.totalAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = OrangePrimary
                    )
                    if (order.slipAttached) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "• สลิปโอนแล้ว ✅", fontSize = 11.sp, color = CucumberGreen, fontWeight = FontWeight.Bold)
                    }
                }

                // Print Slip Button
                OutlinedButton(
                    onClick = onPrintSlip,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "พิมพ์ใบเสร็จ", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons for Order Status Transitions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (order.status == OrderStatus.PENDING) {
                    Button(
                        onClick = { onUpdateStatus(OrderStatus.COOKING) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenCrisp),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("เริ่มทำ (ทอด)", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (order.status == OrderStatus.COOKING) {
                    Button(
                        onClick = { onUpdateStatus(OrderStatus.READY) },
                        colors = ButtonDefaults.buttonColors(containerColor = CucumberGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("เสร็จแล้ว (แจ้งลูกค้า)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (order.status == OrderStatus.READY) {
                    Button(
                        onClick = { onUpdateStatus(OrderStatus.COMPLETED) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("จัดส่งแล้ว / รับแล้ว", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                    if (awaitingCancelConfirm) {
                        Button(
                            onClick = {
                                awaitingCancelConfirm = false
                                onUpdateStatus(OrderStatus.CANCELLED)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChiliRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚠️ ยืนยันยกเลิก", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { awaitingCancelConfirm = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🚫 ปฏิเสธ/ยกเลิก", fontSize = 11.sp, color = ChiliRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (awaitingDeleteConfirm) {
                        Button(
                            onClick = {
                                awaitingDeleteConfirm = false
                                onDeleteOrder()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚠️ ยืนยันลบออเดอร์", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { awaitingDeleteConfirm = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🗑️ ลบออเดอร์", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    itemToRemove?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = { Text("ลบรายการออกจากออเดอร์?", fontWeight = FontWeight.Bold) },
            text = { Text("ต้องการลบ “${item.name} x${item.quantity}” ออกจากออเดอร์ #${order.id} ใช่หรือไม่? ยอดรวมจะคำนวณใหม่ และสต็อกที่หักไว้จะคืนให้") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onRemoveItem(item.itemId)
                        itemToRemove = null
                    }
                ) {
                    Text("ลบรายการ", color = ChiliRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { itemToRemove = null }) { Text("ยกเลิก") }
            }
        )
    }
}
