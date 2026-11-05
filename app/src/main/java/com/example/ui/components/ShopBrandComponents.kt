package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LogoType
import com.example.data.model.ShopInfo
import com.example.data.model.StampIconType
import com.example.ui.theme.OrangePrimary

data class LogoPreset(
    val id: String,
    val name: String,
    val drawableResId: Int,
    val description: String
)

data class StampPreset(
    val id: String,
    val name: String,
    val drawableResId: Int,
    val description: String
)

object ShopBrandPresets {
    val logoPresets = listOf(
        LogoPreset(
            id = "preset_logo_retro",
            name = "ตราสัญลักษณ์กระทะไฟเด็ด",
            drawableResId = R.drawable.shop_logo_retro_1793805033528,
            description = "โลโก้วินเทจไฟลุก ลูกชิ้นทอด & เกี๊ยวกรอบ"
        ),
        LogoPreset(
            id = "preset_logo_plate",
            name = "จานลูกชิ้นราดน้ำจิ้มมะขาม",
            drawableResId = R.drawable.meatball_app_icon_1793783376640,
            description = "ภาพลูกชิ้นทอดสีทองพองกรอบราดน้ำจิ้มเยิ้มๆ"
        ),
        LogoPreset(
            id = "preset_logo_mascot",
            name = "เหรียญมาสคอตน้องลูกชิ้นยิ้ม",
            drawableResId = R.drawable.stamp_badge_meatball_1793805017769,
            description = "ตราสัญลักษณ์การ์ตูนน่ารัก Crispy Bites"
        )
    )

    val stampPresets = listOf(
        StampPreset(
            id = "preset_stamp_badge",
            name = "เหรียญแสตมป์น้องลูกชิ้น",
            drawableResId = R.drawable.stamp_badge_meatball_1793805017769,
            description = "เหรียญแสตมป์สะสมแต้มพรีเมียมสีทอง"
        ),
        StampPreset(
            id = "preset_stamp_retro",
            name = "ตราไฟกระทะเดือด",
            drawableResId = R.drawable.shop_logo_retro_1793805033528,
            description = "แสตมป์ไฟลุกสำหรับแฟนพันธุ์แท้ลูกชิ้นทอด"
        ),
        StampPreset(
            id = "preset_stamp_plate",
            name = "ลูกชิ้นเสียบไม้ชุบน้ำจิ้ม",
            drawableResId = R.drawable.meatball_app_icon_1793783376640,
            description = "ตราลูกชิ้นทอด 3 ลูกราดน้ำจิ้มฉ่ำ"
        )
    )

    val emojiPresets = listOf("🍢", "🍡", "🥟", "🌭", "⭐", "👑", "🔥", "🌶️", "🎯", "🏆", "❤️", "😋")
}

@Composable
fun ShopLogoView(
    shopInfo: ShopInfo,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    fallbackTextSize: Int = 18
) {
    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = Color(0xFFFFF3E0)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            when (shopInfo.logoType) {
                LogoType.PRESET -> {
                    val preset = ShopBrandPresets.logoPresets.find { it.id == shopInfo.logoPresetId }
                        ?: ShopBrandPresets.logoPresets.first()
                    Image(
                        painter = painterResource(id = preset.drawableResId),
                        contentDescription = shopInfo.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                LogoType.CUSTOM_IMAGE -> {
                    if (!shopInfo.logoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = shopInfo.logoUri,
                            contentDescription = shopInfo.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = shopInfo.logoEmoji, fontSize = fallbackTextSize.sp)
                    }
                }
                LogoType.EMOJI -> {
                    Text(text = shopInfo.logoEmoji, fontSize = fallbackTextSize.sp)
                }
            }
        }
    }
}

@Composable
fun StampIconView(
    shopInfo: ShopInfo,
    isStamped: Boolean,
    stepNumber: Int,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = if (isStamped) OrangePrimary else Color(0xFFEEEEEE),
        border = if (isStamped) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isStamped) {
                when (shopInfo.stampType) {
                    StampIconType.PRESET -> {
                        val preset = ShopBrandPresets.stampPresets.find { it.id == shopInfo.stampPresetId }
                            ?: ShopBrandPresets.stampPresets.first()
                        Image(
                            painter = painterResource(id = preset.drawableResId),
                            contentDescription = "แสตมป์สะสมแต้ม",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    StampIconType.CUSTOM_IMAGE -> {
                        if (!shopInfo.stampIconUri.isNullOrBlank()) {
                            AsyncImage(
                                model = shopInfo.stampIconUri,
                                contentDescription = "แสตมป์สะสมแต้ม",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = shopInfo.stampEmoji, fontSize = (size.value * 0.48f).sp)
                        }
                    }
                    StampIconType.EMOJI -> {
                        Text(text = shopInfo.stampEmoji, fontSize = (size.value * 0.48f).sp)
                    }
                }
            } else {
                Text(
                    text = "$stepNumber",
                    fontSize = (size.value * 0.38f).sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
