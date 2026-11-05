package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemStatus
import com.example.data.model.MenuItem
import com.example.ui.MainViewModel
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.GoldenCrisp
import com.example.ui.theme.OrangePrimary
import java.util.Locale

@Composable
fun InventoryView(viewModel: MainViewModel) {
    val menuItems by viewModel.menuItems.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // One-Click Stock Reset Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📦 คลังสินค้า & สต็อกอัจฉริยะ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "สต็อกตัดอัตโนมัติเมื่อลูกค้าสั่งซื้อ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "เพิ่มเมนู", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.oneClickStockReset()
                            Toast.makeText(context, "รีเซ็ตสต็อกเป็นค่าเริ่มต้นประจำวันเรียบร้อย! 🔄", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("one_click_stock_reset_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "One-Click Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(menuItems, key = { it.id }) { item ->
                InventoryItemCard(
                    item = item,
                    onStatusChange = { newStatus -> viewModel.updateItemStatus(item.id, newStatus) },
                    onStockChange = { newStock -> viewModel.updateItemStock(item.id, newStock) }
                )
            }
        }

        if (showAddDialog) {
            AddMenuItemDialog(
                onDismiss = { showAddDialog = false },
                onSave = { item ->
                    viewModel.addOrUpdateMenuItem(item)
                    showAddDialog = false
                    Toast.makeText(context, "เพิ่มเมนู ${item.name} แล้ว", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun InventoryItemCard(
    item: MenuItem,
    onStatusChange: (ItemStatus) -> Unit,
    onStockChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "ราคาขาย ฿${String.format(Locale.US, "%.0f", item.price)} (ต้นทุนตรง ฿${String.format(Locale.US, "%.1f", item.directCost)})",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Current stock stepper
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onStockChange(item.stockQty - 5) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFF0F0F0), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "ลด 5", modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = "${item.stockQty}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (item.stockQty == 0) ChiliRed else Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { onStockChange(item.stockQty + 5) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFFF3E0), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "เพิ่ม 5", tint = OrangePrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // 3-way Status Segmented Switch: 🟢 ขายปกติ / 🟡 สินค้าหมด / 🔴 ซ่อนเมนู
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadgeButton(
                    title = "🟢 ขายปกติ",
                    isSelected = item.status == ItemStatus.ACTIVE,
                    onClick = { onStatusChange(ItemStatus.ACTIVE) },
                    selectedColor = CucumberGreen,
                    modifier = Modifier.weight(1f)
                )

                StatusBadgeButton(
                    title = "🟡 สินค้าหมด",
                    isSelected = item.status == ItemStatus.OUT_OF_STOCK,
                    onClick = { onStatusChange(ItemStatus.OUT_OF_STOCK) },
                    selectedColor = GoldenCrisp,
                    modifier = Modifier.weight(1f)
                )

                StatusBadgeButton(
                    title = "🔴 ซ่อนเมนู",
                    isSelected = item.status == ItemStatus.HIDDEN,
                    onClick = { onStatusChange(ItemStatus.HIDDEN) },
                    selectedColor = ChiliRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AddMenuItemDialog(
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    var id by remember { mutableStateOf("item_${System.currentTimeMillis()}") }
    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("cat_1") }
    var price by remember { mutableStateOf("") }
    var directCost by remember { mutableStateOf("") }
    var stockQty by remember { mutableStateOf("30") }
    var defaultResetQty by remember { mutableStateOf("30") }
    var emoji by remember { mutableStateOf("🍢") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มเมนูใหม่", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ชื่อเมนู") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = categoryId, onValueChange = { categoryId = it }, label = { Text("หมวดหมู่ (cat_1, cat_2, ...)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("ราคาขาย (บาท)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = directCost, onValueChange = { directCost = it }, label = { Text("ต้นทุนตรง (บาท)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stockQty, onValueChange = { stockQty = it }, label = { Text("สต็อกเริ่มต้น") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = defaultResetQty, onValueChange = { defaultResetQty = it }, label = { Text("สต็อก reset รายวัน") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("อีโมจิ") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("คำอธิบาย") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = price.toDoubleOrNull() ?: 0.0
                val c = directCost.toDoubleOrNull() ?: 0.0
                val s = stockQty.toIntOrNull() ?: 0
                val r = defaultResetQty.toIntOrNull() ?: s
                if (name.isBlank()) return@TextButton
                onSave(
                    MenuItem(
                        id = id,
                        name = name.trim(),
                        categoryId = categoryId.trim().ifBlank { "cat_1" },
                        price = p,
                        directCost = c,
                        status = ItemStatus.ACTIVE,
                        stockQty = s,
                        defaultResetQty = r,
                        isBestseller = false,
                        emoji = emoji.ifBlank { "🍢" },
                        description = description.trim()
                    )
                )
            }) { Text("บันทึก") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ยกเลิก") }
        }
    )
}

@Composable
fun StatusBadgeButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f) else Color(0xFFF7F7F7),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, selectedColor) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) selectedColor else Color.Gray
            )
        }
    }
}
