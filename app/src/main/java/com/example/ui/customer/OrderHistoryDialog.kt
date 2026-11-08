package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FulfillmentType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.GoldenCrisp
import com.example.ui.theme.OrangePrimary
import java.util.Locale

@Composable
fun OrderHistoryDialog(viewModel: MainViewModel) {
    val isOpen by viewModel.isOrderHistoryDialogOpen.collectAsState()
    if (!isOpen) return

    val orders by viewModel.customerOrders.collectAsState()
    val consumption by viewModel.customerConsumption.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()

    var selectedFilter by remember { mutableStateOf<String>("all") }

    val filteredOrders = remember(orders, selectedFilter) {
        when (selectedFilter) {
            "completed" -> orders.filter { it.status == OrderStatus.COMPLETED }
            "active" -> orders.filter { it.status == OrderStatus.PENDING || it.status == OrderStatus.COOKING || it.status == OrderStatus.READY }
            "cancelled" -> orders.filter { it.status == OrderStatus.CANCELLED }
            else -> orders
        }
    }

    Dialog(
        onDismissRequest = { viewModel.closeOrderHistory() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("order_history_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = OrangePrimary.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ประวัติการสั่งซื้อ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (customerPhone.isNotBlank()) "เบอร์: $customerPhone" else "ลูกค้าทั่วไป (Guest)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.closeOrderHistory() },
                        modifier = Modifier.testTag("close_order_history_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "ปิด")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Consumption Statistics Card
                ConsumptionSummaryCard(consumption = consumption)

                Spacer(modifier = Modifier.height(12.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("ทั้งหมด (${orders.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_all_orders")
                    )
                    FilterChip(
                        selected = selectedFilter == "completed",
                        onClick = { selectedFilter = "completed" },
                        label = {
                            Text(
                                "สำเร็จ (${orders.count { it.status == OrderStatus.COMPLETED }})",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CucumberGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_completed_orders")
                    )
                    FilterChip(
                        selected = selectedFilter == "active",
                        onClick = { selectedFilter = "active" },
                        label = {
                            Text(
                                "กำลังดำเนินการ (${orders.count { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }})",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldenCrisp,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("filter_active_orders")
                    )
                    FilterChip(
                        selected = selectedFilter == "cancelled",
                        onClick = { selectedFilter = "cancelled" },
                        label = {
                            Text(
                                "ยกเลิก (${orders.count { it.status == OrderStatus.CANCELLED }})",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChiliRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_cancelled_orders")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Orders List
                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🍢", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ยังไม่มีประวัติการสั่งซื้อในหมวดนี้",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "เลือกเมนูลูกชิ้นทอดกรอบๆ สดใหม่ แล้วกดสั่งซื้อได้เลย!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            OrderHistoryCard(
                                order = order,
                                onReorder = { viewModel.reorderSpecificOrder(order) },
                                onTrack = { viewModel.viewOrderTracking(order.id) },
                                onPrintReceipt = { viewModel.openPrintingOrder(order) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsumptionSummaryCard(consumption: com.example.data.model.CustomerConsumptionSummary) {
    Surface(
        color = Color(0xFFFFF8E7),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE0B2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📊", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "สรุปการบริโภคสะสมของคุณ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OrangePrimary
                    )
                }
                if (consumption.totalSavings > 0) {
                    Surface(
                        color = CucumberGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ประหยัด ฿${String.format(Locale.US, "%.0f", consumption.totalSavings)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CucumberGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConsumptionMetric(
                    label = "ยอดบริโภคสะสม",
                    value = "฿${String.format(Locale.US, "%.0f", consumption.totalSpent)}",
                    valueColor = OrangePrimary,
                    isHighlight = true
                )
                ConsumptionMetric(
                    label = "จำนวนออเดอร์",
                    value = "${consumption.totalOrdersCount} ครั้ง",
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                ConsumptionMetric(
                    label = "ลูกชิ้นที่ทานไป",
                    value = "${consumption.totalUnitsConsumed} ไม้",
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ConsumptionMetric(
    label: String,
    value: String,
    valueColor: Color,
    isHighlight: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onReorder: () -> Unit,
    onTrack: () -> Unit,
    onPrintReceipt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_history_card_${order.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Order ID, Date, and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = order.timestamp.ifEmpty { "ไม่ระบุเวลา" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fulfillment & Address
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (order.fulfillment == FulfillmentType.DELIVERY) Icons.Default.DeliveryDining else Icons.Default.Storefront,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = OrangePrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (order.fulfillment == FulfillmentType.DELIVERY) {
                        "จัดส่ง: ${if (order.deliveryAddress.isNotBlank()) order.deliveryAddress else "ตามพิกัด GPS"}"
                    } else {
                        "รับเองที่หน้าร้าน"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            // Items breakdown
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• ${item.name} x${item.quantity}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "฿${String.format(Locale.US, "%.0f", item.price * item.quantity)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Options summary (Skewer, Sauce, Condiments)
            val condimentList = mutableListOf<String>()
            if (order.options.friedChili) condimentList.add("พริกทอด")
            if (order.options.cucumber) condimentList.add("แตงกวา")
            if (order.options.sauceCup) condimentList.add("ถ้วยน้ำจิ้ม")

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color(0xFFF9F9F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "🍢 ${order.options.skewerLabel.ifBlank { order.options.skewer.displayName }} • ${order.options.sauceChoice.displayName}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    if (condimentList.isNotEmpty()) {
                        Text(
                            text = "🥗 เครื่องเคียง: ${condimentList.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    if (order.comment.isNotBlank()) {
                        Text(
                            text = "💬 หมายเหตุ: \"${order.comment}\"",
                            fontSize = 11.sp,
                            color = OrangePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total Cost & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ยอดรวมสุทธิ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "฿${String.format(Locale.US, "%.0f", order.totalAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = OrangePrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Receipt button
                    OutlinedButton(
                        onClick = onPrintReceipt,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("receipt_btn_${order.id}")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "ใบเสร็จ", fontSize = 11.sp)
                    }

                    // Track if still active
                    if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                        Button(
                            onClick = onTrack,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenCrisp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("track_btn_${order.id}")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "ติดตาม", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 1-Click Reorder button
                    Button(
                        onClick = onReorder,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("reorder_btn_${order.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "สั่งซ้ำ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (bgColor, textColor, label) = when (status) {
        OrderStatus.PENDING -> Triple(Color(0xFFFFF3E0), OrangePrimary, "⏳ รอดำเนินการ")
        OrderStatus.COOKING -> Triple(Color(0xFFFFF8E1), GoldenCrisp, "🍳 กำลังทอด")
        OrderStatus.READY -> Triple(Color(0xFFE8F5E9), CucumberGreen, "🎉 พร้อมรับ/ส่ง")
        OrderStatus.COMPLETED -> Triple(Color(0xFFE8F5E9), CucumberGreen, "✅ สำเร็จแล้ว")
        OrderStatus.CANCELLED -> Triple(Color(0xFFFFEBEE), ChiliRed, "❌ ยกเลิก")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
