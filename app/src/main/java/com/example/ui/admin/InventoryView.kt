package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.Category
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
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MenuItem?>(null) }
    var deletingItem by remember { mutableStateOf<MenuItem?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryEmoji by remember { mutableStateOf("🍽️") }

    // Group exactly like the customer page: shop categories in configured order,
    // only categories that actually contain menu items.
    val groupedItems = remember(menuItems, categories) {
        val groups = mutableListOf<Pair<Category?, List<MenuItem>>>()
        val used = mutableSetOf<String>()
        categories
            .filter { it.id != "all" }
            .sortedBy { it.sort }
            .forEach { category ->
                val items = menuItems.filter { it.categoryId == category.id }
                used.add(category.id)
                if (items.isNotEmpty()) groups.add(category to items)
            }
        val orphans = menuItems.filter { it.categoryId !in used }
        if (orphans.isNotEmpty()) groups.add(null to orphans)
        groups
    }

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
                        onClick = { showCategoryDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "หมวดหมู่", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
            groupedItems.forEach { (category, items) ->
                item(key = "category_${category?.id ?: "uncategorized"}") {
                    InventoryCategoryHeader(category = category, items = items)
                }
                items(items, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        onStatusChange = { newStatus -> viewModel.updateItemStatus(item.id, newStatus) },
                        onStockChange = { newStock -> viewModel.updateItemStock(item.id, newStock) },
                        onUnlimitedChange = { unlimited -> viewModel.setItemUnlimitedStock(item.id, unlimited) },
                        onEdit = { editingItem = item },
                        onDelete = { deletingItem = item },
                        onMove = { dir -> viewModel.moveMenuItem(item.id, dir) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddMenuItemDialog(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onSave = { item ->
                    viewModel.addOrUpdateMenuItem(item)
                    showAddDialog = false
                    Toast.makeText(context, "เพิ่มเมนู ${item.name} แล้ว", Toast.LENGTH_SHORT).show()
                }
            )
        }

        editingItem?.let { item ->
            AddMenuItemDialog(
                categories = categories,
                existing = item,
                onDismiss = { editingItem = null },
                onSave = { updated ->
                    viewModel.addOrUpdateMenuItem(updated)
                    editingItem = null
                    Toast.makeText(context, "แก้ไขเมนู ${updated.name} แล้ว", Toast.LENGTH_SHORT).show()
                }
            )
        }

        deletingItem?.let { item ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { deletingItem = null },
                title = { Text("ลบเมนู?", fontWeight = FontWeight.Bold) },
                text = { Text("ต้องการลบเมนู “${item.name}” ออกจากระบบใช่หรือไม่? เมนูนี้จะไม่แสดงให้ลูกค้าเห็นอีก") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMenuItem(item.id)
                            Toast.makeText(context, "ลบเมนู ${item.name} แล้ว", Toast.LENGTH_SHORT).show()
                            deletingItem = null
                        }
                    ) {
                        Text("ลบเมนู", color = ChiliRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingItem = null }) { Text("ยกเลิก") }
                }
            )
        }

        if (showCategoryDialog) {
            CategoryManagerDialog(
                categories = categories,
                menuItems = menuItems,
                onAdd = { name, emoji -> viewModel.addCategory(name, emoji) },
                onRename = { id, name -> viewModel.renameCategory(id, name) },
                onDelete = { id -> viewModel.deleteCategory(id) },
                onDismiss = { showCategoryDialog = false }
            )
        }
    }
}

@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    menuItems: List<MenuItem>,
    onAdd: (String, String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("🍽️") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("จัดการหมวดหมู่", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.filter { it.id != "all" }.forEach { c ->
                    val count = menuItems.count { it.categoryId == c.id }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${c.iconEmoji} ${c.name}", modifier = Modifier.weight(1f))
                        Text("$count เมนู", fontSize = 10.sp, color = Color.Gray)
                        if (count == 0) {
                            TextButton(onClick = { onDelete(c.id) }) { Text("🗑️", color = ChiliRed) }
                        }
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newEmoji, onValueChange = { newEmoji = it }, modifier = Modifier.width(60.dp), singleLine = true)
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("ชื่อหมวดหมู่ใหม่") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAdd(newName.trim(), newEmoji.trim().ifBlank { "🍽️" })
                            newName = ""
                            newEmoji = "🍽️"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("เพิ่มหมวดหมู่") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("เสร็จสิ้น") }
        }
    )
}

@Composable
fun InventoryCategoryHeader(category: Category?, items: List<MenuItem>) {
    val limitedTotal = items.filter { !it.isUnlimitedStock }.sumOf { it.stockQty }
    val unlimitedCount = items.count { it.isUnlimitedStock }
    val totalText = when {
        items.isEmpty() -> ""
        unlimitedCount == items.size -> "${items.size} รายการ • ไม่จำกัด ∞"
        unlimitedCount > 0 -> "${items.size} รายการ • คงเหลือ $limitedTotal ชิ้น • ไม่จำกัด $unlimitedCount รายการ"
        else -> "${items.size} รายการ • คงเหลือ $limitedTotal ชิ้น"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "${category?.iconEmoji ?: "📦"} ${category?.name ?: "ไม่ระบุหมวดหมู่"}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = totalText,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InventoryItemCard(
    item: MenuItem,
    onStatusChange: (ItemStatus) -> Unit,
    onStockChange: (Int) -> Unit,
    onUnlimitedChange: (Boolean) -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onMove: (Int) -> Unit = {}
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(20.dp)) {
                            Text("↑", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onMove(1) }, modifier = Modifier.size(20.dp)) {
                            Text("↓", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.components.MenuItemThumbnail(
                        imageUrl = item.imageUrl,
                        emoji = item.emoji,
                        contentDescription = item.name,
                        size = 44.dp,
                        cornerRadius = 10.dp,
                        emojiFontSize = 22
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "ราคาขาย ฿${String.format(Locale.US, "%.0f", item.price)} / ${item.saleUnit} (ต้นทุนตรง ฿${String.format(Locale.US, "%.1f", item.directCost)})",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Current stock stepper (hidden when unlimited)
                if (item.isUnlimitedStock) {
                    Text(
                        text = "∞ ไม่จำกัด",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = CucumberGreen
                    )
                } else {
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

            Spacer(modifier = Modifier.height(8.dp))

            // Stock mode: unlimited (default) or limited with visible remaining count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = item.isUnlimitedStock,
                        onCheckedChange = { onUnlimitedChange(it) }
                    )
                    Text(
                        text = if (item.isUnlimitedStock) "สต็อกไม่จำกัด (ค่าเริ่มต้น)" else "จำกัดสต็อก — แสดงคงเหลือให้ลูกค้า",
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(8.dp)) {
                        Text("✏️ แก้ไข", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(8.dp)) {
                        Text("🗑️ ลบ", fontSize = 12.sp, color = ChiliRed)
                    }
                }
            }
        }
    }
}

@Composable
fun AddMenuItemDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit,
    existing: MenuItem? = null
) {
    var id by remember { mutableStateOf(existing?.id ?: "item_${System.currentTimeMillis()}") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val selectableCategories = remember(categories) {
        categories.filter { it.id != "all" }.sortedBy { it.sort }
    }
    var categoryId by remember { mutableStateOf(existing?.categoryId ?: selectableCategories.firstOrNull()?.id ?: "cat_1") }
    var price by remember { mutableStateOf(existing?.price?.toString() ?: "") }
    var directCost by remember { mutableStateOf(existing?.directCost?.toString() ?: "") }
    var isUnlimitedStock by remember { mutableStateOf(existing?.isUnlimitedStock ?: true) }
    var saleUnit by remember { mutableStateOf(existing?.saleUnit ?: "ไม้") }
    var stockQty by remember { mutableStateOf(existing?.stockQty?.toString() ?: "30") }
    var defaultResetQty by remember { mutableStateOf(existing?.defaultResetQty?.toString() ?: "30") }
    var emoji by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var imageUrl by remember { mutableStateOf(existing?.imageUrl) }
    var subItems by remember { mutableStateOf(existing?.subItems ?: "") }
    val subItemList = remember { mutableStateListOf<String>() }
    // init subItemList from subItems JSON
    androidx.compose.runtime.LaunchedEffect(subItems) {
        subItemList.clear()
        try {
            val arr = org.json.JSONArray(subItems)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optBoolean("enabled", true)) subItemList.add(o.optString("name", ""))
            }
        } catch (e: Exception) { }
    }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = com.example.ui.components.encodeUriToDataUrl(context, uri, maxDim = 600, quality = 75)
            Toast.makeText(context, "เลือกรูปเมนูแล้ว", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "แก้ไขเมนู" else "เพิ่มเมนูใหม่", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ชื่อเมนู") }, modifier = Modifier.fillMaxWidth())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    com.example.ui.components.MenuItemThumbnail(
                        imageUrl = imageUrl,
                        emoji = emoji,
                        contentDescription = name,
                        size = 56.dp,
                        cornerRadius = 12.dp,
                        emojiFontSize = 26
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (imageUrl.isNullOrBlank()) "📷 เลือกรูปอาหาร" else "📷 เปลี่ยนรูปอาหาร", fontSize = 12.sp)
                        }
                    }
                }
                Text("หมวดหมู่", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectableCategories.forEach { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("ราคาขาย (บาท)") }, modifier = Modifier.fillMaxWidth())
                Text("ขายเป็น (แสดงให้ลูกค้า เช่น ฿10 / ไม้)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ไม้", "ชุด").forEach { unit ->
                        FilterChip(
                            selected = saleUnit == unit,
                            onClick = { saleUnit = unit },
                            label = { Text(if (unit == "ไม้") "ขายเป็นไม้" else "ขายเป็นชุด", fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(value = directCost, onValueChange = { directCost = it }, label = { Text("ต้นทุนตรง (บาท)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = isUnlimitedStock, onCheckedChange = { isUnlimitedStock = it })
                    Text("สต็อกไม่จำกัด (ค่าเริ่มต้น — ไม่แสดงคงเหลือให้ลูกค้า)", fontSize = 12.sp)
                }
                if (!isUnlimitedStock) {
                    OutlinedTextField(value = stockQty, onValueChange = { stockQty = it }, label = { Text("สต็อกคงเหลือที่จะแสดงให้ลูกค้า") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = defaultResetQty, onValueChange = { defaultResetQty = it }, label = { Text("สต็อก reset รายวัน") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("คำอธิบาย") }, modifier = Modifier.fillMaxWidth())

                // Sub-items manager (เมนูรอง)
                Text("เมนูรอง (ให้ลูกค้าเลือก)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                subItemList.forEachIndexed { idx, name ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { subItemList[idx] = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        TextButton(onClick = { subItemList.removeAt(idx) }) {
                            Text("ลบ", color = ChiliRed, fontSize = 11.sp)
                        }
                    }
                }
                var newSubItem by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubItem,
                        onValueChange = { newSubItem = it },
                        label = { Text("เพิ่มเมนูรอง") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    TextButton(onClick = {
                        if (newSubItem.isNotBlank()) {
                            subItemList.add(newSubItem.trim())
                            newSubItem = ""
                        }
                    }) {
                        Text("เพิ่ม", fontSize = 11.sp)
                    }
                }
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
                        status = existing?.status ?: ItemStatus.ACTIVE,
                        stockQty = s,
                        isUnlimitedStock = isUnlimitedStock,
                        saleUnit = saleUnit,
                        defaultResetQty = r,
                        isBestseller = existing?.isBestseller ?: false,
                        emoji = emoji.ifBlank { "🍢" },
                        description = description.trim(),
                        imageUrl = imageUrl,
                        subItems = org.json.JSONArray().apply { subItemList.forEach { put(org.json.JSONObject().apply { put("id", "sub_${System.currentTimeMillis()}"); put("name", it); put("enabled", true) }) } }.toString(),
                        sortOrder = existing?.sortOrder ?: 0,
                        packagePrice = existing?.packagePrice ?: 0.0,
                        piecesPerPackage = existing?.piecesPerPackage ?: 0
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
