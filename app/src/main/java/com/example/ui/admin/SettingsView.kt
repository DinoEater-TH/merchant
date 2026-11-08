package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.LogoType
import com.example.data.model.ShopInfo
import com.example.data.model.StampIconType
import com.example.ui.MainViewModel
import com.example.ui.components.ShopBrandPresets
import com.example.ui.components.ShopLogoView
import com.example.ui.components.StampIconView
import com.example.ui.theme.GoldenCrisp
import com.example.ui.theme.OrangePrimary
import com.example.util.LocationHelper

@Composable
fun SettingsView(viewModel: MainViewModel) {
    val shopInfo by viewModel.shopInfo.collectAsState()
    val context = LocalContext.current

    // Keep the form stable while the admin is typing. Remote values (e.g. from the web
    // admin) are adopted only when the form has no unsaved edits.
    var syncedShopInfo by remember { mutableStateOf(shopInfo) }

    // Brand Logo & Stamp States
    var logoType by remember(syncedShopInfo.logoType) { mutableStateOf(syncedShopInfo.logoType) }
    var logoPresetId by remember(syncedShopInfo.logoPresetId) { mutableStateOf(syncedShopInfo.logoPresetId) }
    var logoUri by remember(syncedShopInfo.logoUri) { mutableStateOf(syncedShopInfo.logoUri ?: "") }
    var logoEmoji by remember(syncedShopInfo.logoEmoji) { mutableStateOf(syncedShopInfo.logoEmoji) }

    var stampType by remember(syncedShopInfo.stampType) { mutableStateOf(syncedShopInfo.stampType) }
    var stampPresetId by remember(syncedShopInfo.stampPresetId) { mutableStateOf(syncedShopInfo.stampPresetId) }
    var stampIconUri by remember(syncedShopInfo.stampIconUri) { mutableStateOf(syncedShopInfo.stampIconUri ?: "") }
    var stampEmoji by remember(syncedShopInfo.stampEmoji) { mutableStateOf(syncedShopInfo.stampEmoji) }

    // General Shop info States
    var name by remember(syncedShopInfo.name) { mutableStateOf(syncedShopInfo.name) }
    var slogan by remember(syncedShopInfo.slogan) { mutableStateOf(syncedShopInfo.slogan) }
    var address by remember(syncedShopInfo.address) { mutableStateOf(syncedShopInfo.address) }
    var promptPayNum by remember(syncedShopInfo.promptPayNumber) { mutableStateOf(syncedShopInfo.promptPayNumber) }
    var promptPayName by remember(syncedShopInfo.promptPayName) { mutableStateOf(syncedShopInfo.promptPayName) }
    var promptPayQrUri by remember(syncedShopInfo.promptPayQrUri) { mutableStateOf(syncedShopInfo.promptPayQrUri ?: "") }
    var showScanPayment by remember(syncedShopInfo.showScanPayment) { mutableStateOf(syncedShopInfo.showScanPayment) }
    var deliveryFeeText by remember(syncedShopInfo.deliveryFee) { mutableStateOf(formatSettingNumber(syncedShopInfo.deliveryFee)) }
    var shopLatText by remember(syncedShopInfo.shopLat) { mutableStateOf(syncedShopInfo.shopLat?.toString() ?: "") }
    var shopLngText by remember(syncedShopInfo.shopLng) { mutableStateOf(syncedShopInfo.shopLng?.toString() ?: "") }
    var showShopLocation by remember(syncedShopInfo.showShopLocation) { mutableStateOf(syncedShopInfo.showShopLocation) }
    var deliveryGpsEnabled by remember(syncedShopInfo.deliveryGpsEnabled) { mutableStateOf(syncedShopInfo.deliveryGpsEnabled) }
    var isLocatingShop by remember { mutableStateOf(false) }
    // Start empty: never pre-fill the field with the current secret PIN
    var pinText by remember { mutableStateOf("") }

    var isWebExportDialogOpen by remember { mutableStateOf(false) }

    // Current values of the edit form, used to detect unsaved edits.
    val localShopInfo = shopInfo.copy(
        name = name,
        slogan = slogan,
        address = address,
        promptPayNumber = promptPayNum,
        promptPayName = promptPayName,
        promptPayQrUri = promptPayQrUri.ifBlank { null },
        showScanPayment = showScanPayment,
        deliveryFee = deliveryFeeText.toDoubleOrNull() ?: shopInfo.deliveryFee,
        shopLat = shopLatText.toDoubleOrNull(),
        shopLng = shopLngText.toDoubleOrNull(),
        showShopLocation = showShopLocation,
        deliveryGpsEnabled = deliveryGpsEnabled,
        logoType = logoType,
        logoPresetId = logoPresetId,
        logoUri = logoUri.ifBlank { null },
        logoEmoji = logoEmoji,
        stampType = stampType,
        stampPresetId = stampPresetId,
        stampIconUri = stampIconUri.ifBlank { null },
        stampEmoji = stampEmoji
    )
    // Adopt changes coming from the other device (web) only when this form has no
    // unsaved edits, so typing is never overwritten mid-edit.
    LaunchedEffect(shopInfo) {
        if (localShopInfo == shopInfo || localShopInfo == syncedShopInfo) {
            syncedShopInfo = shopInfo
        }
    }

    fun captureShopLocation() {
        isLocatingShop = true
        LocationHelper.requestCurrentLocation(context) { coordinates ->
            isLocatingShop = false
            if (coordinates != null) {
                val parts = coordinates.split(",").map { it.trim() }
                if (parts.size >= 2) {
                    shopLatText = parts[0]
                    shopLngText = parts[1]
                    Toast.makeText(context, "ตั้งพิกัดร้านเป็น $coordinates แล้ว", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "อ่านพิกัด GPS ไม่ได้ กรุณาเปิด GPS แล้วลองอีกครั้ง", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val shopLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) captureShopLocation()
        else Toast.makeText(context, "ต้องอนุญาตตำแหน่งเพื่อตั้งพิกัดร้าน", Toast.LENGTH_SHORT).show()
    }

    // Modern Android Photo Picker for Shop Logo (Zero Permission)
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            logoUri = com.example.ui.components.encodeUriToDataUrl(context, uri, maxDim = 300, quality = 85) ?: uri.toString()
            logoType = LogoType.CUSTOM_IMAGE
            Toast.makeText(context, "เลือกรูปโลโก้เรียบร้อย", Toast.LENGTH_SHORT).show()
        }
    }

    // Modern Android Photo Picker for Stamp Icon (Zero Permission)
    val stampPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            stampIconUri = com.example.ui.components.encodeUriToDataUrl(context, uri, maxDim = 200, quality = 85) ?: uri.toString()
            stampType = StampIconType.CUSTOM_IMAGE
            Toast.makeText(context, "เลือกรูปไอคอนแสตมป์เรียบร้อย", Toast.LENGTH_SHORT).show()
        }
    }

    // Modern Android Photo Picker for PromptPay QR Code (Zero Permission)
    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            promptPayQrUri = com.example.ui.components.encodeUriToDataUrl(context, uri, maxDim = 600, quality = 85) ?: uri.toString()
            Toast.makeText(context, "เลือกรูป QR Code เรียบร้อย", Toast.LENGTH_SHORT).show()
        }
    }

    // Temporary preview ShopInfo object reflecting pending edits
    val previewShopInfo = remember(
        shopInfo, name, logoType, logoPresetId, logoUri, logoEmoji,
        stampType, stampPresetId, stampIconUri, stampEmoji
    ) {
        shopInfo.copy(
            name = name,
            logoType = logoType,
            logoPresetId = logoPresetId,
            logoUri = logoUri.ifBlank { null },
            logoEmoji = logoEmoji,
            stampType = stampType,
            stampPresetId = stampPresetId,
            stampIconUri = stampIconUri.ifBlank { null },
            stampEmoji = stampEmoji
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Brand Preview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldenCrisp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_preview_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👀", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ตัวอย่างการแสดงผลหน้าร้านจริง",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = OrangePrimary
                            )
                        }
                        Surface(
                            color = OrangePrimary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Live Preview",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logo & Shop Name preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        ShopLogoView(
                            shopInfo = previewShopInfo,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = previewShopInfo.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = previewShopInfo.slogan,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stamp Card preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "ตัวอย่างบัตรสะสมแต้ม 10 แต้ม:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "สะสมแล้ว 6/10", fontSize = 11.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 1..10) {
                                StampIconView(
                                    shopInfo = previewShopInfo,
                                    isStamped = i <= 6,
                                    stepNumber = i,
                                    size = 28.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Shop Logo Settings Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shop_logo_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "1. ตั้งค่ารูปโลโก้ร้าน (Shop Logo)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Type selection chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = logoType == LogoType.PRESET,
                            onClick = { logoType = LogoType.PRESET },
                            label = { Text("🎨 รูปสำเร็จรูป", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("logo_preset_chip")
                        )
                        FilterChip(
                            selected = logoType == LogoType.CUSTOM_IMAGE,
                            onClick = { logoType = LogoType.CUSTOM_IMAGE },
                            label = { Text("🖼️ อัปโหลดรูปภาพ", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("logo_custom_chip")
                        )
                        FilterChip(
                            selected = logoType == LogoType.EMOJI,
                            onClick = { logoType = LogoType.EMOJI },
                            label = { Text("😀 อิโมจิ", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("logo_emoji_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (logoType) {
                        LogoType.PRESET -> {
                            Text(text = "เลือกโลโก้สำเร็จรูปความละเอียดสูง:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ShopBrandPresets.logoPresets.forEach { preset ->
                                    val isSelected = logoPresetId == preset.id
                                    Surface(
                                        color = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFFAFAFA),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) OrangePrimary else Color(0xFFE0E0E0)
                                        ),
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable { logoPresetId = preset.id }
                                            .testTag("logo_preset_${preset.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Image(
                                                painter = painterResource(id = preset.drawableResId),
                                                contentDescription = preset.name,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = preset.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = preset.description,
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        LogoType.CUSTOM_IMAGE -> {
                            Text(text = "เลือกภาพโลโก้ของคุณจากเครื่อง หรือระบุที่อยู่ภาพ (URL/URI):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        logoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_pick_logo_image")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("เลือกรูปจากเครื่อง")
                                }

                                if (logoUri.isNotBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary)
                                    ) {
                                        com.example.ui.components.UniversalImage(
                                            uriOrDataUrl = logoUri,
                                            contentDescription = "Logo Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = logoUri,
                                onValueChange = { logoUri = it },
                                label = { Text("หรือใส่ URL / ที่อยู่ไฟล์รูปภาพโลโก้") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        LogoType.EMOJI -> {
                            Text(text = "เลือกหรือพิมพ์อิโมจิโลโก้ร้าน:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShopBrandPresets.emojiPresets.forEach { emoji ->
                                    Surface(
                                        shape = CircleShape,
                                        color = if (logoEmoji == emoji) OrangePrimary else Color(0xFFF5F5F5),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable { logoEmoji = emoji }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = logoEmoji,
                                onValueChange = { logoEmoji = it },
                                label = { Text("อิโมจิโลโก้ (เช่น 🍢)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 3. Stamp Icon Settings Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stamp_icon_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldenCrisp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "2. ตั้งค่ารูป Icon Stamp สะสมแต้ม", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Type selection chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = stampType == StampIconType.PRESET,
                            onClick = { stampType = StampIconType.PRESET },
                            label = { Text("🎨 ตราประทับสำเร็จรูป", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("stamp_preset_chip")
                        )
                        FilterChip(
                            selected = stampType == StampIconType.CUSTOM_IMAGE,
                            onClick = { stampType = StampIconType.CUSTOM_IMAGE },
                            label = { Text("🖼️ อัปโหลดรูปแสตมป์", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("stamp_custom_chip")
                        )
                        FilterChip(
                            selected = stampType == StampIconType.EMOJI,
                            onClick = { stampType = StampIconType.EMOJI },
                            label = { Text("⭐ ไอคอนอิโมจิ", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("stamp_emoji_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (stampType) {
                        StampIconType.PRESET -> {
                            Text(text = "เลือกตราประทับสะสมแต้มลายพิเศษ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ShopBrandPresets.stampPresets.forEach { preset ->
                                    val isSelected = stampPresetId == preset.id
                                    Surface(
                                        color = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFFAFAFA),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) OrangePrimary else Color(0xFFE0E0E0)
                                        ),
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable { stampPresetId = preset.id }
                                            .testTag("stamp_preset_${preset.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Image(
                                                painter = painterResource(id = preset.drawableResId),
                                                contentDescription = preset.name,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = preset.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = preset.description,
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        StampIconType.CUSTOM_IMAGE -> {
                            Text(text = "เลือกภาพตราแสตมป์ของคุณจากเครื่อง:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        stampPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_pick_stamp_image")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("เลือกรูปแสตมป์จากเครื่อง")
                                }

                                if (stampIconUri.isNotBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(40.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary)
                                    ) {
                                        com.example.ui.components.UniversalImage(
                                            uriOrDataUrl = stampIconUri,
                                            contentDescription = "Stamp Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = stampIconUri,
                                onValueChange = { stampIconUri = it },
                                label = { Text("หรือใส่ URL / ที่อยู่ไฟล์รูปภาพแสตมป์") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StampIconType.EMOJI -> {
                            Text(text = "เลือกหรือพิมพ์ไอคอนแสตมป์สะสมแต้ม:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShopBrandPresets.emojiPresets.forEach { emoji ->
                                    Surface(
                                        shape = CircleShape,
                                        color = if (stampEmoji == emoji) OrangePrimary else Color(0xFFF5F5F5),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable { stampEmoji = emoji }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = stampEmoji,
                                onValueChange = { stampEmoji = it },
                                label = { Text("ไอคอนแสตมป์ (เช่น 🍢, ⭐, 🥟)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Brand Settings Button
                    Button(
                        onClick = {
                            val updated = shopInfo.copy(
                                logoType = logoType,
                                logoPresetId = logoPresetId,
                                logoUri = logoUri.ifBlank { null },
                                logoEmoji = logoEmoji,
                                stampType = stampType,
                                stampPresetId = stampPresetId,
                                stampIconUri = stampIconUri.ifBlank { null },
                                stampEmoji = stampEmoji
                            )
                            viewModel.updateShopInfo(updated)
                            Toast.makeText(context, "บันทึกรูปโลโก้และรูปไอคอนแสตมป์เรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_brand_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("บันทึกการตั้งค่าโลโก้ร้านและไอคอนแสตมป์")
                    }
                }
            }
        }

        // 4. Shop General info card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "3. ข้อมูลร้านค้าและการชำระเงิน", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("ชื่อร้านค้า") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = slogan,
                        onValueChange = { slogan = it },
                        label = { Text("สโลแกนร้าน (แสดงใต้ชื่อร้านให้ลูกค้า)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("ที่อยู่ร้านค้า") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = promptPayNum,
                        onValueChange = { promptPayNum = it },
                        label = { Text("เบอร์พร้อมเพย์ (PromptPay)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = promptPayName,
                        onValueChange = { promptPayName = it },
                        label = { Text("ชื่อบัญชีพร้อมเพย์") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showScanPayment, onCheckedChange = { showScanPayment = it })
                        Text("แสดงปุ่มสแกน PromptPay ให้ลูกค้า", fontSize = 13.sp)
                    }

                    Text(
                        "อัปโหลดรูป QR Code ของร้าน (ถ้าไม่ใส่ ระบบจะสร้าง QR จากเลขพร้อมเพย์ให้อัตโนมัติ)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                qrPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_pick_qr_image")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("เลือกรูป QR จากเครื่อง")
                        }

                        if (promptPayQrUri.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(56.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary)
                            ) {
                                com.example.ui.components.UniversalImage(
                                    uriOrDataUrl = promptPayQrUri,
                                    contentDescription = "QR Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    if (promptPayQrUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { promptPayQrUri = "" }) {
                            Text("ลบรูป QR", color = Color.Red, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deliveryFeeText,
                        onValueChange = { deliveryFeeText = it },
                        label = { Text("ค่าจัดส่งปกติ (บาท)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("📍 พิกัดร้าน (ใช้คำนวณค่าส่งตามเส้นทางและนำทาง)", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shopLatText,
                            onValueChange = { shopLatText = it },
                            label = { Text("ละติจูด (Lat)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shopLngText,
                            onValueChange = { shopLngText = it },
                            label = { Text("ลองติจูด (Lng)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) captureShopLocation()
                            else shopLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        enabled = !isLocatingShop,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLocatingShop) "กำลังอ่านพิกัด GPS..." else "📍 ใช้ตำแหน่งปัจจุบันของเครื่องนี้เป็นพิกัดร้าน")
                    }

                    val shopLatValue = shopLatText.toDoubleOrNull()
                    val shopLngValue = shopLngText.toDoubleOrNull()
                    if (shopLatValue != null && shopLngValue != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse("https://www.openstreetmap.org/?mlat=$shopLatValue&mlon=$shopLngValue#map=16/$shopLatValue/$shopLngValue")
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🗺️ เปิดแผนที่ตรวจพิกัดร้าน (OpenStreetMap)")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showShopLocation, onCheckedChange = { showShopLocation = it })
                        Text("แสดงพิกัดและปุ่มนำทางไปร้านให้ลูกค้าเห็น", fontSize = 13.sp)
                    }
                    Text(
                        "ปิดไว้ = ลูกค้าจะไม่เห็นปุ่มนำทางมาที่ร้าน แต่ระบบยังใช้พิกัดคำนวณค่าส่งได้",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deliveryGpsEnabled, onCheckedChange = { deliveryGpsEnabled = it })
                        Text("ให้ลูกค้าแนบพิกัด GPS (ปิด = ใช้ค่าส่งคงที่)", fontSize = 13.sp)
                    }
                    Text(
                        "ปิดไว้ = ปุ่ม GPS/แผนที่/ระยะทางจะถูกซ่อน และปุ่มชำระเงินทำงานได้เลยเมื่อเลือกจัดส่ง",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val fee = deliveryFeeText.toDoubleOrNull() ?: 15.0
                            val updated = shopInfo.copy(
                                name = name,
                                slogan = slogan,
                                address = address,
                                promptPayNumber = promptPayNum,
                                promptPayName = promptPayName,
                                promptPayQrUri = promptPayQrUri.ifBlank { null },
                                showScanPayment = showScanPayment,
                                deliveryFee = fee,
                                shopLat = shopLatText.toDoubleOrNull(),
                                shopLng = shopLngText.toDoubleOrNull(),
                                showShopLocation = showShopLocation,
        deliveryGpsEnabled = deliveryGpsEnabled,
                                logoType = logoType,
                                logoPresetId = logoPresetId,
                                logoUri = logoUri.ifBlank { null },
                                logoEmoji = logoEmoji,
                                stampType = stampType,
                                stampPresetId = stampPresetId,
                                stampIconUri = stampIconUri.ifBlank { null },
                                stampEmoji = stampEmoji
                            )
                            viewModel.updateShopInfo(updated)
                            Toast.makeText(context, "บันทึกข้อมูลร้านค้าแล้ว", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("บันทึกข้อมูลร้านค้าและการชำระเงิน")
                    }
                }
            }
        }

        // 5. Security card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "4. ความปลอดภัย (PIN 4-8 หลัก)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pinText = it },
                        label = { Text("รหัส PIN ใหม่ (4-8 หลัก)") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (pinText.length in 4..8) {
                                viewModel.setAdminPin(pinText)
                                Toast.makeText(context, "เปลี่ยนรหัส PIN แล้ว", Toast.LENGTH_SHORT).show()
                                pinText = ""
                            } else {
                                Toast.makeText(context, "กรุณาใส่รหัส 4-8 หลัก", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("เปลี่ยนรหัส PIN")
                    }
                }
            }
        }

        // 6a. Customer links (copy to share)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔗 ลิงก์สำหรับลูกค้า (คัดลอกส่งให้ลูกค้า)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🌐 เว็บสั่งอาหาร: https://dinoeater-merchant.web.app/", fontSize = 12.sp)
                    Text("📱 ดาวน์โหลดแอป Android: https://github.com/DinoEater-TH/merchant/raw/main/docs/prototype/DinoEater-Merchant.apk", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("link", "https://dinoeater-merchant.web.app/")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "คัดลอกลิงก์เว็บแล้ว", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("คัดลอกลิงก์เว็บ")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("link", "https://github.com/DinoEater-TH/merchant/raw/main/docs/prototype/DinoEater-Merchant.apk")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "คัดลอกลิงก์ดาวน์โหลดแอปแล้ว", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("คัดลอกลิงก์ดาวน์โหลดแอป Android")
                    }
                }
            }
        }

        // 6. Web PWA Export card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "5. ส่งออกเว็บ Web PWA (GitHub Pages)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "ระบบได้สร้างไฟล์ index.html และ admin.html พร้อมใช้งานฟรี 100% บน GitHub Pages ตามสเปคงบ 0 บาท",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { isWebExportDialogOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("เปิดดูโค้ด index.html & admin.html")
                    }
                }
            }
        }
    }

    if (isWebExportDialogOpen) {
        WebPwaExportDialog(onDismiss = { isWebExportDialogOpen = false })
    }
}

@Composable
fun WebPwaExportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: index.html, 1: admin.html

    val indexHtmlSnippet = """
<!-- index.html (Customer UI - Single Page App) -->
<!DOCTYPE html>
<html lang="th">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ร้านลูกชิ้นนายเอ็ด - สั่งอาหารออนไลน์</title>
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-amber-50 text-gray-800">
  <!-- Categories, Best Sellers, Add-ons modal, PromptPay QR, and Status Tracker -->
  <div class="max-w-md mx-auto p-4">
    <h1 class="text-xl font-bold text-orange-600">🍢 ร้านลูกชิ้นนายเอ็ด</h1>
    <p class="text-xs text-gray-500">งบ 0 บาท พร้อมเพย์ & ติดตามออเดอร์สด</p>
  </div>
</body>
</html>
""".trimIndent()

    val adminHtmlSnippet = """
<!-- admin.html (Merchant Dashboard - PIN Lock & Analytics) -->
<!DOCTYPE html>
<html lang="th">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ร้านลูกชิ้นนายเอ็ด - จัดการร้านค้า</title>
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 text-gray-800">
  <!-- PIN Code, Kitchen Orders, POS Stamps, Inventory, and Break-even Analytics -->
</body>
</html>
""".trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🌐 เว็บไฟล์ HTML (GitHub Pages Ready)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) OrangePrimary else Color.LightGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("index.html")
                    }
                    Button(
                        onClick = { selectedTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) OrangePrimary else Color.LightGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("admin.html")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Text(
                        text = if (selectedTab == 0) indexHtmlSnippet else adminHtmlSnippet,
                        color = Color(0xFF81C784),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ปิด")
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val code = if (selectedTab == 0) indexHtmlSnippet else adminHtmlSnippet
                            clipboard.setPrimaryClip(ClipData.newPlainText("HTML", code))
                            Toast.makeText(context, "คัดลอกโค้ด HTML เรียบร้อย", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("คัดลอกโค้ด")
                    }
                }
            }
        }
    }
}
