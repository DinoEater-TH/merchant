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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.example.data.model.ShopInfo
import com.example.data.model.CustomPromo
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionView(viewModel: MainViewModel) {
    val shopInfo by viewModel.shopInfo.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val customPromos by viewModel.customPromos.collectAsState()
    // Keep the form stable while the admin is typing. Remote values (e.g. from the web
    // admin) are adopted only when the form has no unsaved edits.
    var syncedShopInfo by remember { mutableStateOf(shopInfo) }
    var deliveryFee by remember(syncedShopInfo.deliveryFee) { mutableStateOf(formatSettingNumber(syncedShopInfo.deliveryFee)) }
    var promoDeliveryFee by remember(syncedShopInfo.promoDeliveryFeeEnabled) { mutableStateOf(syncedShopInfo.promoDeliveryFeeEnabled) }
    var deliveryFeePerKm by remember(syncedShopInfo.deliveryFeePerKm) {
        mutableStateOf(if (syncedShopInfo.deliveryFeePerKm == 0.0) "" else formatSettingNumber(syncedShopInfo.deliveryFeePerKm))
    }
    var maxDeliveryDistanceKm by remember(syncedShopInfo.maxDeliveryDistanceKm) {
        mutableStateOf(formatSettingNumber(syncedShopInfo.maxDeliveryDistanceKm))
    }
    var minDiscount by remember(syncedShopInfo.minOrderForDiscount) { mutableStateOf(formatSettingNumber(syncedShopInfo.minOrderForDiscount)) }
    var discountAmount by remember(syncedShopInfo.discountAmount) { mutableStateOf(formatSettingNumber(syncedShopInfo.discountAmount)) }
    var freeDelivery by remember(syncedShopInfo.minOrderForFreeDelivery) { mutableStateOf(formatSettingNumber(syncedShopInfo.minOrderForFreeDelivery)) }
    var stamps by remember(syncedShopInfo.stampsRequiredForReward) { mutableStateOf(syncedShopInfo.stampsRequiredForReward.toString()) }
    var spendPerStamp by remember(syncedShopInfo.spendPerStamp) { mutableStateOf(formatSettingNumber(syncedShopInfo.spendPerStamp)) }
    var rewardDesc by remember(syncedShopInfo.rewardDescription) { mutableStateOf(syncedShopInfo.rewardDescription) }
    var rewardItemId by remember(syncedShopInfo.rewardItemId) { mutableStateOf(syncedShopInfo.rewardItemId) }
    var showPromo by remember(syncedShopInfo.showPromoBanner) { mutableStateOf(syncedShopInfo.showPromoBanner) }
    var promoDiscount by remember(syncedShopInfo.promoDiscountEnabled) { mutableStateOf(syncedShopInfo.promoDiscountEnabled) }
    var promoDeliveryPerKm by remember(syncedShopInfo.promoDeliveryPerKmEnabled) { mutableStateOf(syncedShopInfo.promoDeliveryPerKmEnabled) }
    var promoFreeDelivery by remember(syncedShopInfo.promoFreeDeliveryEnabled) { mutableStateOf(syncedShopInfo.promoFreeDeliveryEnabled) }
    var promoReward by remember(syncedShopInfo.promoRewardEnabled) { mutableStateOf(syncedShopInfo.promoRewardEnabled) }
    var rewardExpanded by remember { mutableStateOf(false) }
    var newPromoName by remember { mutableStateOf("") }
    var newPromoType by remember { mutableStateOf("percentOff") }
    var newPromoMinQty by remember { mutableStateOf("1") }
    var newPromoValue by remember { mutableStateOf("10") }
    var newPromoBuyQty by remember { mutableStateOf("3") }
    var newPromoGetQty by remember { mutableStateOf("1") }
    var newPromoBuyItem by remember { mutableStateOf("") }
    var newPromoGetItem by remember { mutableStateOf("") }
    val rewardItem = menuItems.find { it.id == rewardItemId }

    // Current values of the edit form, used to detect unsaved edits.
    val localShopInfo = shopInfo.copy(
        deliveryFee = deliveryFee.toDoubleOrNull() ?: shopInfo.deliveryFee,
        deliveryFeePerKm = deliveryFeePerKm.toDoubleOrNull() ?: 0.0,
        maxDeliveryDistanceKm = maxDeliveryDistanceKm.toDoubleOrNull() ?: 20.0,
        minOrderForDiscount = minDiscount.toDoubleOrNull() ?: shopInfo.minOrderForDiscount,
        discountAmount = discountAmount.toDoubleOrNull() ?: shopInfo.discountAmount,
        minOrderForFreeDelivery = freeDelivery.toDoubleOrNull() ?: shopInfo.minOrderForFreeDelivery,
        stampsRequiredForReward = stamps.toIntOrNull() ?: shopInfo.stampsRequiredForReward,
        rewardDescription = rewardDesc,
        rewardItemId = rewardItemId,
        showPromoBanner = showPromo,
        promoDeliveryFeeEnabled = promoDeliveryFee,
        promoDiscountEnabled = promoDiscount,
        promoDeliveryPerKmEnabled = promoDeliveryPerKm,
        promoFreeDeliveryEnabled = promoFreeDelivery,
        promoRewardEnabled = promoReward,
        spendPerStamp = spendPerStamp.toDoubleOrNull() ?: shopInfo.spendPerStamp
    )
    // Adopt changes coming from the other device (web) only when this form has no
    // unsaved edits, so typing is never overwritten mid-edit.
    LaunchedEffect(shopInfo) {
        if (localShopInfo == shopInfo || localShopInfo == syncedShopInfo) {
            syncedShopInfo = shopInfo
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = OrangePrimary)
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text("โปรโมชั่น & แต้มสะสม", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("จัดกลุ่มโปรโมชั่นเพื่อให้ลูกค้าเห็นชัดเจน", fontSize = 11.sp, color = Color.Gray)

                OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("ค่าส่งพื้นฐาน (บาท) — ใช้เมื่อยังไม่ตั้งราคาต่อกิโล") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = deliveryFeePerKm,
                    onValueChange = { deliveryFeePerKm = it },
                    label = { Text("ค่าส่งต่อกิโล (บาท/กม.) — วัดจากเส้นทางจริง") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxDeliveryDistanceKm,
                    onValueChange = { maxDeliveryDistanceKm = it },
                    label = { Text("ระยะทางสูงสุดที่จัดส่งได้ (กม.)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "ระบบจะคำนวณระยะทางตามถนนจากพิกัดร้านไปถึงจุดส่งของลูกค้า แล้วคิดค่าส่ง = ระยะทาง (กม.) × ราคาต่อกิโล (ปัดขึ้นเป็นบาท). ถ้าเกินระยะทางสูงสุด ลูกค้าจะสั่งแบบจัดส่งไม่ได้ ต้องเลือกรับเองที่ร้าน",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                OutlinedTextField(value = minDiscount, onValueChange = { minDiscount = it }, label = { Text("ซื้อครบ (บาท) จึงลด") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = discountAmount, onValueChange = { discountAmount = it }, label = { Text("ส่วนลด (บาท)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = freeDelivery, onValueChange = { freeDelivery = it }, label = { Text("ส่งฟรีเมื่อครบ (บาท)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stamps, onValueChange = { stamps = it }, label = { Text("สะสม stamp ครบ (ดวง) เพื่อแลกเมนูรางวัล") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = spendPerStamp, onValueChange = { spendPerStamp = it }, label = { Text("ซื้อเท่าไหร่ (บาท) ได้ 1 แต้ม") }, supportingText = { Text("ยอดไม่ถึงเกณฑ์ = ไม่ได้แต้ม • ใส่ 0 = 1 แต้มต่อออเดอร์", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rewardDesc, onValueChange = { rewardDesc = it }, label = { Text("คำอธิบายรางวัล") }, modifier = Modifier.fillMaxWidth())

                Text("รายการฟรีที่แอดมินเลือก (เมื่อสะสมครบ)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(expanded = rewardExpanded, onExpandedChange = { rewardExpanded = !rewardExpanded }) {
                    OutlinedTextField(
                        value = rewardItem?.let { "${it.emoji} ${it.name} (฿${it.price.toInt()})" } ?: "เลือกรายการฟรี",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rewardExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = rewardExpanded, onDismissRequest = { rewardExpanded = false }) {
                        menuItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.emoji} ${item.name} (฿${item.price.toInt()})") },
                                onClick = { rewardItemId = item.id; rewardExpanded = false }
                            )
                        }
                    }
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = showPromo, onCheckedChange = { showPromo = it })
                    Text("แสดงป้ายโปรโมชั่นให้ลูกค้า", fontSize = 13.sp)
                }
                Text("เปิด/ปิดโปรโมชั่นแต่ละรายการ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = promoDeliveryFee, onCheckedChange = { promoDeliveryFee = it })
                    Text("ค่าส่งพื้นฐาน", fontSize = 13.sp)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = promoDiscount, onCheckedChange = { promoDiscount = it })
                    Text("ส่วนลดเมื่อครบยอด", fontSize = 13.sp)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = promoDeliveryPerKm, onCheckedChange = { promoDeliveryPerKm = it })
                    Text("คิดค่าส่งตามระยะทาง (บาท/กม.)", fontSize = 13.sp)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = promoFreeDelivery, onCheckedChange = { promoFreeDelivery = it })
                    Text("ส่งฟรีเมื่อครบยอด", fontSize = 13.sp)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = promoReward, onCheckedChange = { promoReward = it })
                    Text("แต้มแลกฟรี 1 รายการ", fontSize = 13.sp)
                }

                // ---- Custom Promotions ----
                Text("🎁 โปรโมชั่นพิเศษ (กำหนดเอง)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                customPromos.forEach { promo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = promo.enabled, onCheckedChange = { viewModel.toggleCustomPromo(promo.id, it) })
                            Text(promo.name, fontSize = 13.sp)
                        }
                        TextButton(onClick = { viewModel.deleteCustomPromo(promo.id) }) {
                            Text("🗑️", color = Color.Red)
                        }
                    }
                }
                if (customPromos.isEmpty()) {
                    Text("ยังไม่มีโปรโมชั่นพิเศษ", fontSize = 12.sp, color = Color.Gray)
                }
                // Add form
                var promoName by remember { mutableStateOf("") }
                var promoType by remember { mutableStateOf("percentOff") }
                var promoMinQty by remember { mutableStateOf("1") }
                var promoValue by remember { mutableStateOf("10") }
                OutlinedTextField(value = promoName, onValueChange = { promoName = it }, label = { Text("ชื่อโปรโมชั่น") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = promoType, onValueChange = { promoType = it }, label = { Text("ประเภท (buyXgetY/percentOff/fixedOff)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = promoMinQty, onValueChange = { promoMinQty = it }, label = { Text("ซื้อครบ") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = promoValue, onValueChange = { promoValue = it }, label = { Text("ค่า") }, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        if (promoName.isNotBlank()) {
                            val promo = CustomPromo(
                                id = "promo_" + System.currentTimeMillis(),
                                name = promoName.trim(),
                                type = promoType.trim(),
                                enabled = true,
                                minQty = promoMinQty.toIntOrNull() ?: 1,
                                discountPercent = if (promoType == "percentOff") (promoValue.toDoubleOrNull() ?: 10.0) else 0.0,
                                discountAmount = if (promoType == "fixedOff") (promoValue.toDoubleOrNull() ?: 10.0) else 0.0,
                                buyQty = 3,
                                getQty = 1
                            )
                            viewModel.addCustomPromo(promo)
                            promoName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("เพิ่มโปรโมชั่น", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val updated = shopInfo.copy(
                            deliveryFee = deliveryFee.toDoubleOrNull() ?: shopInfo.deliveryFee,
                            deliveryFeePerKm = deliveryFeePerKm.toDoubleOrNull() ?: 0.0,
                            maxDeliveryDistanceKm = maxDeliveryDistanceKm.toDoubleOrNull() ?: 20.0,
                            minOrderForDiscount = minDiscount.toDoubleOrNull() ?: shopInfo.minOrderForDiscount,
                            discountAmount = discountAmount.toDoubleOrNull() ?: shopInfo.discountAmount,
                            minOrderForFreeDelivery = freeDelivery.toDoubleOrNull() ?: shopInfo.minOrderForFreeDelivery,
                            stampsRequiredForReward = stamps.toIntOrNull() ?: shopInfo.stampsRequiredForReward,
                            rewardDescription = rewardDesc,
                            rewardItemId = rewardItemId,
                            showPromoBanner = showPromo,
                            promoDeliveryFeeEnabled = promoDeliveryFee,
                            promoDiscountEnabled = promoDiscount,
                            promoDeliveryPerKmEnabled = promoDeliveryPerKm,
                            promoFreeDeliveryEnabled = promoFreeDelivery,
                            promoRewardEnabled = promoReward,
                            spendPerStamp = spendPerStamp.toDoubleOrNull() ?: shopInfo.spendPerStamp
                        )
                        viewModel.updateShopInfo(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("บันทึกโปรโมชั่น", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
