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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionView(viewModel: MainViewModel) {
    val shopInfo by viewModel.shopInfo.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    var deliveryFee by remember(shopInfo.deliveryFee) { mutableStateOf(shopInfo.deliveryFee.toInt().toString()) }
    var minDiscount by remember(shopInfo.minOrderForDiscount) { mutableStateOf(shopInfo.minOrderForDiscount.toInt().toString()) }
    var discountAmount by remember(shopInfo.discountAmount) { mutableStateOf(shopInfo.discountAmount.toInt().toString()) }
    var freeDelivery by remember(shopInfo.minOrderForFreeDelivery) { mutableStateOf(shopInfo.minOrderForFreeDelivery.toInt().toString()) }
    var stamps by remember(shopInfo.stampsRequiredForReward) { mutableStateOf(shopInfo.stampsRequiredForReward.toString()) }
    var rewardDesc by remember(shopInfo.rewardDescription) { mutableStateOf(shopInfo.rewardDescription) }
    var rewardItemId by remember(shopInfo.rewardItemId) { mutableStateOf(shopInfo.rewardItemId) }
    var showPromo by remember(shopInfo.showPromoBanner) { mutableStateOf(shopInfo.showPromoBanner) }
    var rewardExpanded by remember { mutableStateOf(false) }
    val rewardItem = menuItems.find { it.id == rewardItemId }

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

                OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("ค่าส่ง (บาท)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minDiscount, onValueChange = { minDiscount = it }, label = { Text("ซื้อครบ (บาท) จึงลด") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = discountAmount, onValueChange = { discountAmount = it }, label = { Text("ส่วนลด (บาท)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = freeDelivery, onValueChange = { freeDelivery = it }, label = { Text("ส่งฟรีเมื่อครบ (บาท)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stamps, onValueChange = { stamps = it }, label = { Text("แต้มแลกรางวัล") }, modifier = Modifier.fillMaxWidth())
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

                Button(
                    onClick = {
                        val updated = shopInfo.copy(
                            deliveryFee = deliveryFee.toDoubleOrNull() ?: shopInfo.deliveryFee,
                            minOrderForDiscount = minDiscount.toDoubleOrNull() ?: shopInfo.minOrderForDiscount,
                            discountAmount = discountAmount.toDoubleOrNull() ?: shopInfo.discountAmount,
                            minOrderForFreeDelivery = freeDelivery.toDoubleOrNull() ?: shopInfo.minOrderForFreeDelivery,
                            stampsRequiredForReward = stamps.toIntOrNull() ?: shopInfo.stampsRequiredForReward,
                            rewardDescription = rewardDesc,
                            rewardItemId = rewardItemId,
                            showPromoBanner = showPromo
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
