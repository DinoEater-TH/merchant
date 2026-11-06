package com.example.ui.customer

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AddonOptions
import com.example.data.model.FulfillmentType
import com.example.data.model.SauceChoice
import com.example.data.model.SkewerOption
import com.example.ui.MainViewModel
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.OrangePrimary
import com.example.util.LocationHelper
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddonsDialog(viewModel: MainViewModel) {
    val isOpen by viewModel.isAddonModalOpen.collectAsState()
    if (!isOpen) return

    val currentAddons by viewModel.addonOptions.collectAsState()
    val fulfillment by viewModel.fulfillmentType.collectAsState()
    val deliveryAddress by viewModel.deliveryAddress.collectAsState()
    val gpsCoordinates by viewModel.gpsCoordinates.collectAsState()
    val distanceKm by viewModel.deliveryDistanceKm.collectAsState()
    val distanceLoading by viewModel.deliveryDistanceLoading.collectAsState()
    val distanceError by viewModel.deliveryDistanceError.collectAsState()
    val cartSummary by viewModel.cartSummary.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()

    var tempAddons by remember(currentAddons) { mutableStateOf(currentAddons) }
    var tempFulfillment by remember(fulfillment) { mutableStateOf(fulfillment) }
    var tempAddress by remember(deliveryAddress) { mutableStateOf(deliveryAddress) }
    var tempComment by remember(currentAddons.comment) { mutableStateOf(currentAddons.comment) }
    var gpsCaptured by remember(gpsCoordinates) { mutableStateOf(gpsCoordinates.isNotBlank()) }
    var isLocating by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(isOpen, tempFulfillment, gpsCoordinates, shopInfo.shopLat, shopInfo.shopLng) {
        if (isOpen && tempFulfillment == FulfillmentType.DELIVERY && gpsCoordinates.isNotBlank()) {
            viewModel.refreshDeliveryQuote()
        }
    }

    fun startLocationCapture() {
        isLocating = true
        LocationHelper.requestCurrentLocation(context) { coordinates ->
            isLocating = false
            if (coordinates != null) {
                viewModel.setGpsCoordinates(coordinates)
                gpsCaptured = true
            } else {
                Toast.makeText(
                    context,
                    "ไม่สามารถอ่านพิกัด GPS ได้ กรุณาเปิด GPS แล้วลองอีกครั้ง",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLocationCapture()
        } else {
            Toast.makeText(
                context,
                "ต้องอนุญาตสิทธิ์เข้าถึงตำแหน่งเพื่อแนบพิกัดจัดส่ง",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Dialog(
        onDismissRequest = { viewModel.closeAddonModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("addons_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🍢 ตัวเลือกการรับสินค้าและเครื่องเคียง",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "ปรับแต่งความอร่อยตามใจคุณ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.closeAddonModal() }) {
                        Icon(Icons.Default.Close, contentDescription = "ปิด")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Fulfillment Mode: Delivery vs Pickup
                Text(
                    text = "1. รูปแบบการรับสินค้า",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Delivery button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tempFulfillment = FulfillmentType.DELIVERY },
                        shape = RoundedCornerShape(12.dp),
                        color = if (tempFulfillment == FulfillmentType.DELIVERY) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (tempFulfillment == FulfillmentType.DELIVERY) OrangePrimary else Color(0xFFE0E0E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🛵", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "จัดส่งตามที่อยู่",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (tempFulfillment == FulfillmentType.DELIVERY) OrangePrimary else Color.Unspecified
                            )
                        }
                    }

                    // Pickup button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tempFulfillment = FulfillmentType.PICKUP },
                        shape = RoundedCornerShape(12.dp),
                        color = if (tempFulfillment == FulfillmentType.PICKUP) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (tempFulfillment == FulfillmentType.PICKUP) OrangePrimary else Color(0xFFE0E0E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🏪", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "มารับเองหน้าร้าน",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (tempFulfillment == FulfillmentType.PICKUP) OrangePrimary else Color.Unspecified
                            )
                        }
                    }
                }

                if (tempFulfillment == FulfillmentType.DELIVERY) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { tempAddress = it },
                        label = { Text("ที่อยู่จัดส่ง (บ้านเลขที่ / ซอย / จุดสังเกต)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                startLocationCapture()
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        enabled = !isLocating,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isLocating -> "กำลังค้นหาพิกัด GPS..."
                                gpsCaptured -> "พิกัด GPS ปัจจุบัน: $gpsCoordinates (แนบแล้ว ✅)"
                                else -> "📍 กดเพื่อส่งพิกัด GPS ปัจจุบัน"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = when {
                            distanceError != null -> Color(0xFFFFEBEE)
                            distanceKm != null -> Color(0xFFE8F5E9)
                            else -> Color(0xFFF5F5F5)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            when {
                                distanceLoading -> Text("กำลังคำนวณระยะทางตามเส้นทาง...", fontSize = 12.sp)
                                distanceError != null -> Text(
                                    text = "⚠️ $distanceError",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Medium
                                )
                                distanceKm != null -> {
                                    val maxKm = shopInfo.maxDeliveryDistanceKm
                                    val overLimit = distanceKm!! > maxKm
                                    Text(
                                        text = "ระยะทางตามเส้นทาง ${String.format(Locale.US, "%.1f", distanceKm)} กม. • สูงสุด ${String.format(Locale.US, "%.0f", maxKm)} กม.",
                                        fontSize = 12.sp,
                                        color = if (overLimit) Color(0xFFC62828) else Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (overLimit) {
                                        Text(
                                            text = "เกินระยะทางให้บริการ ไม่สามารถสั่งอาหารได้",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC62828)
                                        )
                                    }
                                }
                                else -> Text("แนบพิกัด GPS เพื่อคำนวณระยะทางและค่าส่ง", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "📍 ร้านอยู่ที่: ${shopInfo.address}", fontSize = 12.sp)
                            if (shopInfo.showShopLocation && shopInfo.shopLat != null && shopInfo.shopLng != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = {
                                        val lat = shopInfo.shopLat
                                        val lng = shopInfo.shopLng
                                        val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(${shopInfo.name})")
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text("🧭 เปิดแผนที่นำทางไปร้าน", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Skewer Option
                Text(
                    text = "2. รูปแบบการใส่ไม้",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                SkewerOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempAddons = tempAddons.copy(skewer = option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tempAddons.skewer == option,
                            onClick = { tempAddons = tempAddons.copy(skewer = option) },
                            colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${option.icon} ${option.displayName}", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // 3. Free Condiments (Checkboxes)
                Text(
                    text = "3. เครื่องเคียงและถ้วยน้ำจิ้ม (ฟรี)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic addon options from admin config
                val addonItems = remember(shopInfo.addonConfig) {
                    parseAddonItems(shopInfo.addonConfig)
                }
                val selectedAddons = remember { mutableStateListOf<String>() }
                // initialize selected from tempAddons.selectedAddons
                androidx.compose.runtime.LaunchedEffect(addonItems, tempAddons) {
                    selectedAddons.clear()
                    selectedAddons.addAll(tempAddons.selectedAddons)
                    // default legacy addons
                    if (tempAddons.friedChili && !selectedAddons.contains("chili")) selectedAddons.add("chili")
                    if (tempAddons.cucumber && !selectedAddons.contains("cucumber")) selectedAddons.add("cucumber")
                    if (tempAddons.sauceCup && !selectedAddons.contains("sauceCup")) selectedAddons.add("sauceCup")
                }
                addonItems.filter { it.enabled }.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedAddons.contains(item.id),
                            onCheckedChange = { checked ->
                                if (checked) selectedAddons.add(item.id) else selectedAddons.remove(item.id)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = OrangePrimary)
                        )
                        Text(text = item.name, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // 4. Sauce Choice
                Text(
                    text = "4. เลือกน้ำจิ้มสูตรเด็ด",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                SauceChoice.values().forEach { sauce ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempAddons = tempAddons.copy(sauceChoice = sauce) }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tempAddons.sauceChoice == sauce,
                            onClick = { tempAddons = tempAddons.copy(sauceChoice = sauce) },
                            colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = sauce.displayName, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // 5. Comment / Note
                Text(
                    text = "5. หมายเหตุเพิ่มเติมถึงร้าน (ถ้ามี)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = tempComment,
                    onValueChange = { tempComment = it },
                    placeholder = { Text("เช่น 'ขอทอดกรอบๆ', 'แยกน้ำจิ้ม', 'ไม่เอาน้ำมัน'") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )

                // Quick chips for comment
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ขอทอดกรอบๆ", "แยกน้ำจิ้ม", "ไม่เผ็ดมาก").forEach { tag ->
                        Surface(
                            modifier = Modifier.clickable {
                                tempComment = if (tempComment.isEmpty()) tag else "$tempComment, $tag"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0F0F0)
                        ) {
                            Text(
                                text = "+ $tag",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // Calculation breakdown
                val isDelivery = tempFulfillment == FulfillmentType.DELIVERY
                val freeDeliveryReached = cartSummary.subtotal >= shopInfo.minOrderForFreeDelivery
                val quotedDistance = distanceKm
                val perKmEnabled = shopInfo.promoDeliveryPerKmEnabled
                val previewDeliveryFee = if (isDelivery && !freeDeliveryReached && quotedDistance != null) {
                    if (perKmEnabled && shopInfo.deliveryFeePerKm > 0.0) ceil(quotedDistance * shopInfo.deliveryFeePerKm) else shopInfo.deliveryFee
                } else 0.0
                val deliveryBlocked = isDelivery && (
                    quotedDistance == null ||
                        quotedDistance > shopInfo.maxDeliveryDistanceKm ||
                        distanceLoading
                    )
                val previewTotal = (cartSummary.subtotal - cartSummary.discount + previewDeliveryFee).coerceAtLeast(0.0)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "ยอดรวมสินค้า (${cartSummary.totalItemsCount} รายการ):", fontSize = 13.sp)
                        Text(text = "฿${String.format(Locale.US, "%.0f", cartSummary.subtotal)}", fontSize = 13.sp)
                    }

                    if (cartSummary.discount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "ส่วนลดโปรโมชั่น (ซื้อครบ 50.-):", fontSize = 13.sp, color = CucumberGreen)
                            Text(text = "-฿${String.format(Locale.US, "%.0f", cartSummary.discount)}", fontSize = 13.sp, color = CucumberGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isDelivery) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (quotedDistance != null && !freeDeliveryReached) {
                                    "ค่าจัดส่ง (${String.format(Locale.US, "%.1f", quotedDistance)} กม. × ฿${String.format(Locale.US, "%.0f", shopInfo.deliveryFeePerKm)}/กม.):"
                                } else "ค่าจัดส่ง:",
                                fontSize = 13.sp
                            )
                            when {
                                freeDeliveryReached -> Text("ฟรี (ซื้อครบ ${String.format(Locale.US, "%.0f", shopInfo.minOrderForFreeDelivery)}.-) 🎉", fontSize = 13.sp, color = CucumberGreen, fontWeight = FontWeight.Bold)
                                distanceLoading -> Text("กำลังคำนวณ...", fontSize = 13.sp, color = Color.Gray)
                                quotedDistance == null -> Text("รอพิกัด GPS", fontSize = 13.sp, color = Color(0xFFC62828))
                                else -> Text("฿${String.format(Locale.US, "%.0f", previewDeliveryFee)}", fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color(0xFFDDDDDD))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ยอดสุทธิที่ต้องชำระ:", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(
                            text = "฿${String.format(Locale.US, "%.0f", previewTotal)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = OrangePrimary
                        )
                    }
                }

                if (deliveryBlocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = distanceError ?: "กรุณาแนบพิกัด GPS จุดส่งก่อนสั่งอาหาร (ไม่เกิน ${String.format(Locale.US, "%.0f", shopInfo.maxDeliveryDistanceKm)} กม.)",
                        fontSize = 12.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val shopInfo by viewModel.shopInfo.collectAsState()
                val showScan = shopInfo.showScanPayment

                // Cash payment
                Button(
                    onClick = {
                        val finalOptions = tempAddons.copy(comment = tempComment, selectedAddons = selectedAddons.toList())
                        viewModel.setAddonOptions(finalOptions)
                        viewModel.setFulfillment(tempFulfillment)
                        if (tempFulfillment == FulfillmentType.DELIVERY) {
                            viewModel.setDeliveryAddress(tempAddress)
                        }
                        viewModel.confirmCashOrder()
                    },
                    enabled = !deliveryBlocked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        disabledContainerColor = Color(0xFFBDBDBD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(text = "💵 เงินสด", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showScan) {
// Action buttons
                Button(
                    onClick = {
                        val finalOptions = tempAddons.copy(comment = tempComment, selectedAddons = selectedAddons.toList())
                        viewModel.setAddonOptions(finalOptions)
                        viewModel.setFulfillment(tempFulfillment)
                        if (tempFulfillment == FulfillmentType.DELIVERY) {
                            viewModel.setDeliveryAddress(tempAddress)
                        }
                        viewModel.openPaymentModal()
                    },
                    enabled = !deliveryBlocked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary,
                        disabledContainerColor = Color(0xFFBDBDBD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_addons_proceed_button")
                ) {
                    Text(text = "ไปที่หน้าชำระเงิน (PromptPay QR) >", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                }
            }
        }
    }
}
data class AddonItem(val id: String, val name: String, val enabled: Boolean = true)

fun parseAddonItems(config: String): List<AddonItem> {
    if (config.isBlank()) return listOf(
        AddonItem("chili", "???????", true),
        AddonItem("cucumber", "??????", true),
        AddonItem("sauceCup", "???????????", true)
    )
    return try {
        val arr = JSONArray(config)
        val list = mutableListOf<AddonItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(AddonItem(o.optString("id", "addon_$i"), o.optString("name", "????????"), o.optBoolean("enabled", true)))
        }
        if (list.isEmpty()) listOf(AddonItem("chili", "???????", true), AddonItem("cucumber", "??????", true), AddonItem("sauceCup", "???????????", true)) else list
    } catch (e: Exception) {
        listOf(AddonItem("chili", "???????", true), AddonItem("cucumber", "??????", true), AddonItem("sauceCup", "???????????", true))
    }
}
