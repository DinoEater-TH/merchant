package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.OrangePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun parseOrderTimestamp(value: String): Long? {
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(value)?.time
    } catch (_: Exception) {
        null
    }
}

private fun startOfToday(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun startOfDaysAgo(days: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startOfToday()
    calendar.add(Calendar.DAY_OF_YEAR, -days)
    return calendar.timeInMillis
}

private fun isPosOrder(order: Order): Boolean =
    order.comment.trim().uppercase(Locale.US).startsWith("POS")

@Composable
fun SalesHistoryView(viewModel: MainViewModel) {
    val orders by viewModel.orders.collectAsState()
    val users by viewModel.users.collectAsState()

    var period by remember { mutableStateOf("all") }
    var channel by remember { mutableStateOf("all") }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var query by remember { mutableStateOf("") }

    val filteredOrders = remember(orders, period, channel, statusFilter, query) {
        val todayStart = startOfToday()
        val weekStart = startOfDaysAgo(6)
        val monthStart = startOfDaysAgo(29)
        val q = query.trim().lowercase(Locale.US)
        orders
            .filter { order ->
                val time = parseOrderTimestamp(order.timestamp)
                when (period) {
                    "today" -> time != null && time >= todayStart
                    "7d" -> time != null && time >= weekStart
                    "month" -> time != null && time >= monthStart
                    else -> true
                }
            }
            .filter { order ->
                when (channel) {
                    "pos" -> isPosOrder(order)
                    "online" -> !isPosOrder(order)
                    else -> true
                }
            }
            .filter { order -> statusFilter == null || order.status == statusFilter }
            .filter { order ->
                if (q.isEmpty()) return@filter true
                val memberName = users[order.customerPhone]?.name.orEmpty()
                val itemNames = order.items.joinToString(" ") { it.name }
                listOf(order.id, order.customerPhone, memberName, itemNames)
                    .joinToString(" ")
                    .lowercase(Locale.US)
                    .contains(q)
            }
            .sortedByDescending { it.timestamp }
    }

    val validOrders = filteredOrders.filter { it.status != OrderStatus.CANCELLED }
    val revenue = validOrders.sumOf { it.totalAmount }
    val todayRevenue = validOrders
        .filter { (parseOrderTimestamp(it.timestamp) ?: 0L) >= startOfToday() }
        .sumOf { it.totalAmount }
    val profit = validOrders.sumOf { order ->
        order.items.sumOf { (it.price - it.directCost) * it.quantity } - order.discount
    }
    val average = if (validOrders.isEmpty()) 0.0 else revenue / validOrders.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🧾 ประวัติการขาย",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "ยอดขาย กำไร และรายการบิลทั้งหมดของร้าน",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SalesSummaryCard(
                        label = "ยอดขายรวม",
                        value = "฿${formatMoney(revenue)}",
                        sub = "${validOrders.size} บิล",
                        color = OrangePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    SalesSummaryCard(
                        label = "กำไรขั้นต้น",
                        value = "฿${formatMoney(profit)}",
                        sub = "หลังหักต้นทุน+ส่วนลด",
                        color = CucumberGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SalesSummaryCard(
                        label = "ยอดขายวันนี้",
                        value = "฿${formatMoney(todayRevenue)}",
                        sub = "เฉพาะวันนี้",
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f)
                    )
                    SalesSummaryCard(
                        label = "เฉลี่ยต่อบิล",
                        value = "฿${formatMoney(average)}",
                        sub = "${orders.count { it.status == OrderStatus.CANCELLED }} บิลยกเลิก",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("ค้นหาเลขออเดอร์ / เบอร์โทร / เมนู", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("sales_search")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SalesFilterChip("ทั้งหมด", period == "all") { period = "all" }
            SalesFilterChip("วันนี้", period == "today") { period = "today" }
            SalesFilterChip("7 วัน", period == "7d") { period = "7d" }
            SalesFilterChip("30 วัน", period == "month") { period = "month" }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SalesFilterChip("ทุกช่องทาง", channel == "all") { channel = "all" }
            SalesFilterChip("หน้าร้าน (POS)", channel == "pos") { channel = "pos" }
            SalesFilterChip("ออนไลน์", channel == "online") { channel = "online" }
            SalesFilterChip("สำเร็จ", statusFilter == OrderStatus.COMPLETED) {
                statusFilter = if (statusFilter == OrderStatus.COMPLETED) null else OrderStatus.COMPLETED
            }
            SalesFilterChip("ยกเลิก", statusFilter == OrderStatus.CANCELLED) {
                statusFilter = if (statusFilter == OrderStatus.CANCELLED) null else OrderStatus.CANCELLED
            }
        }

        if (filteredOrders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ไม่พบประวัติการขายตามเงื่อนไข",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ลองล้างตัวกรองหรือเปลี่ยนช่วงวันที่",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "${filteredOrders.size} รายการ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(filteredOrders, key = { it.id }) { order ->
                    SaleHistoryCard(
                        order = order,
                        memberName = users[order.customerPhone]?.name.orEmpty(),
                        onPrint = { viewModel.openPrintingOrder(order) },
                        onCancel = { viewModel.updateOrderStatus(order.id, OrderStatus.CANCELLED) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesSummaryCard(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SalesFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFFF3E0),
            selectedLabelColor = OrangePrimary
        )
    )
}

@Composable
private fun SaleHistoryCard(
    order: Order,
    memberName: String,
    onPrint: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (order.status) {
        OrderStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        OrderStatus.COOKING -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        OrderStatus.READY -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        OrderStatus.COMPLETED -> Color(0xFFF3F4F6) to Color(0xFF4B5563)
        OrderStatus.CANCELLED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sales_order_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.id,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = OrangePrimary
                )
                Surface(color = statusColor.first, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = order.status.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor.second,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${order.timestamp.ifBlank { "-" }} • ${order.customerPhone.ifBlank { "ลูกค้าทั่วไป" }}" +
                    if (memberName.isNotBlank()) " ($memberName)" else "",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isPosOrder(order)) "ช่องทาง: หน้าร้าน (POS)" else "ช่องทาง: ลูกค้าสั่งออนไลน์",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = order.items.joinToString(", ") { "${it.name} x${it.quantity}" }.ifBlank { "-" },
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "รวม ฿${formatMoney(order.subtotal)} • ลด ฿${formatMoney(order.discount)}" +
                        if (order.deliveryFee > 0) " • ส่ง ฿${formatMoney(order.deliveryFee)}" else "",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "฿${formatMoney(order.totalAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (order.comment.isNotBlank()) {
                Text(
                    text = "หมายเหตุ: ${order.comment}",
                    fontSize = 10.sp,
                    color = OrangePrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("พิมพ์ใบเสร็จ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (order.status != OrderStatus.CANCELLED) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFB91C1C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ยกเลิกบิล", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatMoney(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value)
