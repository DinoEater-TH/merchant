package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.MainViewModel
import com.example.ui.customer.SubItem
import com.example.ui.customer.parseSubItems
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

/**
 * Pastel group palette so ไม้ 5 / ไม้ 10 / เกี๊ยวทอด are easy to tell apart at a glance.
 * Order follows the shop's own category order, so each group keeps a stable colour.
 */
private data class PastelStyle(val container: Color, val border: Color, val chip: Color)

private val PASTEL_STYLES = listOf(
    PastelStyle(Color(0xFFFFF1E6), Color(0xFFF2C9A6), Color(0xFFFFDFC4)), // peach
    PastelStyle(Color(0xFFE8F6EC), Color(0xFFB4DBC2), Color(0xFFD3EDDB)), // mint
    PastelStyle(Color(0xFFE9F1FB), Color(0xFFB5CBE9), Color(0xFFD6E5F8)), // sky
    PastelStyle(Color(0xFFF3ECFA), Color(0xFFCBB9E6), Color(0xFFE4D8F5)), // lavender
    PastelStyle(Color(0xFFFFF9E0), Color(0xFFE7D79C), Color(0xFFFBF0BE)), // lemon
    PastelStyle(Color(0xFFFDECF1), Color(0xFFEFC0CE), Color(0xFFFAD8E2))  // pink
)

private val NEUTRAL_STYLE = PastelStyle(
    container = Color(0xFFF7F7F7),
    border = Color(0xFFDDDDDD),
    chip = Color(0xFFECECEC)
)

@Composable
fun PosMenuView(viewModel: MainViewModel) {
    val menuItems by viewModel.menuItems.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val cart = remember { mutableStateMapOf<String, Int>() }
    // itemId -> (sub-item name -> count). The cart quantity of such a menu is the sum.
    val subCounts = remember { mutableStateMapOf<String, Map<String, Int>>() }
    var selectedCategory by remember { mutableStateOf("all") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // Drag-to-reorder: the shop decides which menu sits on the upper/lower row.
    // The arrangement is saved into the shared menu order (same order on the customer web).
    val canonicalIds = remember(menuItems) {
        menuItems.sortedWith(compareBy { it.sortOrder }).map { it.id }
    }
    var workingIds by remember { mutableStateOf(canonicalIds) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var draggedDistance by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val dragScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(canonicalIds, draggingId) {
        if (draggingId == null) workingIds = canonicalIds
    }

    // Stable colour per category (shop order first, then any extra categories).
    val categoryOrder = remember(categoriesState, menuItems) {
        val ordered = categoriesState.filter { it.id != "all" }.sortedBy { it.sort }.map { it.id }
        ordered + menuItems.map { it.categoryId }
            .distinct()
            .filter { it.isNotBlank() && it != "all" && it !in ordered }
    }
    fun styleFor(categoryId: String): PastelStyle {
        val index = categoryOrder.indexOf(categoryId)
        return if (index >= 0) PASTEL_STYLES[index % PASTEL_STYLES.size] else NEUTRAL_STYLE
    }

    fun categoryName(id: String): String {
        val c = categoriesState.find { it.id == id }
        return c?.name?.takeIf { it.isNotBlank() } ?: id
    }
    val categories = remember(menuItems) {
        menuItems.map { it.categoryId }.distinct().filter { it.isNotBlank() && it != "all" }
    }
    val menuById = remember(menuItems) { menuItems.associateBy { it.id } }
    val filtered = remember(workingIds, menuById, selectedCategory) {
        workingIds.mapNotNull { menuById[it] }
            .filter { it.status.name != "HIDDEN" && (selectedCategory == "all" || it.categoryId == selectedCategory) }
    }

    // +1/-1 for one sub-item. The cart line always equals the sum of the sub counts.
    fun changeSubItem(itemId: String, name: String, delta: Int) {
        val next = subCounts[itemId].orEmpty().toMutableMap()
        val value = ((next[name] ?: 0) + delta).coerceAtLeast(0)
        if (value == 0) next.remove(name) else next[name] = value
        val total = next.values.sum()
        if (total <= 0) {
            subCounts.remove(itemId)
            cart.remove(itemId)
        } else {
            subCounts[itemId] = next.toMap()
            cart[itemId] = total
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Category chips, tinted with the same pastel as their group.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == "all",
                onClick = { selectedCategory = "all" },
                label = { Text("ทั้งหมด", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
            categories.forEach { cat ->
                val style = styleFor(cat)
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(categoryName(cat), fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = style.container,
                        selectedContainerColor = style.chip
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == cat,
                        borderColor = style.border,
                        selectedBorderColor = style.border
                    )
                )
            }
        }
        Text(
            text = "กดค้างที่เมนูแล้วลากขึ้น-ลงเพื่อสลับตำแหน่ง • บันทึกอัตโนมัติ",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, top = 3.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val startItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                    offset.y.toInt() in info.offset..(info.offset + info.size)
                                }
                                val key = startItem?.key as? String
                                if (key != null) {
                                    draggingId = key
                                    draggedDistance = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                val key = draggingId
                                if (key != null) {
                                    change.consume()
                                    draggedDistance += dragAmount.y
                                    val layout = listState.layoutInfo
                                    val current = layout.visibleItemsInfo.firstOrNull { it.key == key }
                                    if (current != null) {
                                        val start = current.offset + draggedDistance
                                        val end = start + current.size
                                        val middle = (start + end) / 2f
                                        // Swap with the card the finger is now over.
                                        val target = layout.visibleItemsInfo.firstOrNull { info ->
                                            info.key != key && middle.toInt() in info.offset..(info.offset + info.size)
                                        }
                                        if (target != null) {
                                            val targetId = target.key as? String
                                            val from = workingIds.indexOf(key)
                                            val to = if (targetId == null) -1 else workingIds.indexOf(targetId)
                                            if (from >= 0 && to >= 0 && from != to) {
                                                workingIds = workingIds.toMutableList().apply {
                                                    removeAt(from)
                                                    add(to, key)
                                                }
                                                draggedDistance += current.offset - target.offset
                                            }
                                        }
                                        // Keep long drags usable: scroll when near the edges.
                                        val edge = 80f
                                        val viewportEnd = layout.viewportEndOffset.toFloat()
                                        val push = when {
                                            start < edge -> (start - edge) / edge
                                            end > viewportEnd - edge -> (viewportEnd - edge - end) / edge
                                            else -> 0f
                                        }
                                        if (push != 0f) dragScope.launch { listState.scrollBy(push * 24f) }
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggingId != null) {
                                    val visibleNow = workingIds.filter { id ->
                                        val m = menuItems.firstOrNull { it.id == id }
                                        m != null && m.status.name != "HIDDEN" &&
                                            (selectedCategory == "all" || m.categoryId == selectedCategory)
                                    }
                                    if (visibleNow.isNotEmpty()) viewModel.reorderMenuItems(visibleNow)
                                }
                                draggingId = null
                                draggedDistance = 0f
                            },
                            onDragCancel = {
                                draggingId = null
                                draggedDistance = 0f
                                workingIds = canonicalIds
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { m ->
                    val style = styleFor(m.categoryId)
                    val subs = remember(m.id, m.subItems) {
                        parseSubItems(m.subItems).filter { it.enabled }
                    }
                    val isDragging = draggingId == m.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) draggedDistance else 0f },
                        colors = CardDefaults.cardColors(containerColor = style.container),
                        border = BorderStroke(1.dp, style.border)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = m.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "฿${m.price} / ${m.saleUnit}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (subs.isEmpty()) {
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

                            if (subs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val itemTotal = subCounts[m.id]?.values?.sum() ?: 0
                                Text(
                                    text = "เลือกเมนูรอง • รวม $itemTotal ${m.saleUnit}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                subs.forEach { sub: SubItem ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sub.name,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { changeSubItem(m.id, sub.name, -1) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "ลด ${sub.name}",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "${subCounts[m.id]?.get(sub.name) ?: 0}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { changeSubItem(m.id, sub.name, 1) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "เพิ่ม ${sub.name}",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
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
        val items = cart.toMap()
        val preview = viewModel.posOrderPreview(items, phone, subCounts.toMap())
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
                                if (oi.subItemSummary.isNotBlank()) {
                                    Text(
                                        text = oi.subItemSummary,
                                        fontSize = 11.sp,
                                        color = OrangePrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
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
                        if (items.isEmpty()) {
                            message = "กรุณาเลือกรายการก่อน"
                            return@Button
                        }
                        val orderId = viewModel.submitPosOrder(
                            cart = items,
                            phone = phone,
                            subCounts = subCounts.toMap()
                        )
                        if (orderId == null) {
                            message = "บันทึกไม่สำเร็จ: ไม่พบรายการเมนู"
                        } else {
                            message = "สร้างออเดอร์ $orderId สำเร็จ ฿${fmtBaht(preview?.totalAmount ?: 0.0)}"
                            cart.clear()
                            subCounts.clear()
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
    valueColor: Color = Color.Unspecified
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(text = label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 12.sp, color = valueColor)
    }
}

private fun fmtBaht(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)
