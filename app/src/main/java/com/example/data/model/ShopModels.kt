package com.example.data.model

enum class LogoType {
    PRESET,
    CUSTOM_IMAGE,
    EMOJI
}

enum class StampIconType {
    PRESET,
    CUSTOM_IMAGE,
    EMOJI
}

data class ShopInfo(
    val name: String = "ร้านลูกชิ้นนายเอ็ด",
    val slogan: String = "ลูกชิ้นทอดในตำนาน น้ำจิ้มมะขามรสเด็ด กรอบนอกนุ่มใน",
    val promptPayNumber: String = "084-611-2419",
    val promptPayName: String = "นายเอ็ด ลูกชิ้นทอด",
    val address: String = "ตลาดโต้รุ่ง ถ.มิตรภาพ อ.เมือง จ.ขอนแก่น",
    val openingHours: String = "10:00 - 20:30 น. (เปิดทุกวัน)",
    val isOpen: Boolean = true,
    val deliveryFee: Double = 15.0,
    val minOrderForDiscount: Double = 50.0,
    val discountAmount: Double = 5.0,
    val minOrderForFreeDelivery: Double = 120.0,
    val stampsRequiredForReward: Int = 10,
    val rewardDescription: String = "ฟรี! ชุดลูกชิ้นรวมมิตร 1 ชุด",
val addonConfig: String = "", // JSON: {"friedChili":true,"cucumber":true,"sauceCup":true}
    // Shop Logo Customization
    val logoType: LogoType = LogoType.PRESET,
    val logoUri: String? = null,
    val logoPresetId: String = "preset_logo_retro",
    val logoEmoji: String = "🍢",
    // Stamp Icon Customization
    val stampType: StampIconType = StampIconType.PRESET,
    val stampIconUri: String? = null,
    val stampPresetId: String = "preset_stamp_badge",
    val stampEmoji: String = "🍢"
)

data class Category(
    val id: String,
    val name: String,
    val sort: Int,
    val iconEmoji: String = "🍢"
)

enum class ItemStatus {
    ACTIVE,         // 🟢 ขายปกติ
    OUT_OF_STOCK,   // 🟡 สินค้าหมด
    HIDDEN          // 🔴 ซ่อนเมนู
}

data class MenuItem(
    val id: String,
    val name: String,
    val categoryId: String,
    val price: Double,
    val directCost: Double, // Cost per unit (ลูกชิ้น + น้ำจิ้ม + ไม้)
    val status: ItemStatus = ItemStatus.ACTIVE,
    val stockQty: Int = 50,
    val defaultResetQty: Int = 50,
    val isBestseller: Boolean = false,
    val emoji: String = "🍢",
    val description: String = "",
val imageUrl: String? = null,
val sortOrder: Int = 0
)

data class CartItem(
    val item: MenuItem,
    val quantity: Int
)

enum class SkewerOption(val displayName: String, val icon: String) {
    SKEWERED("เสียบไม้", "🍢"),
    IN_BAG("ใส่ถุง (ไม่เสียบไม้)", "🛍️"),
    SEPARATE("รับไม้เสียบแยก", "🥢")
}

enum class SauceChoice(val displayName: String) {
    TAMARIND("น้ำจิ้มมะขามโบราณ (สูตรเด็ด)"),
    SEAFOOD("น้ำจิ้มซีฟู้ดมะนาวสดแซ่บ"),
    MIXED("ผสม 2 อย่าง (เด็ดสุด แนะนำ!)"),
    SEPARATE_CUP("แยกถ้วยน้ำจิ้มต่างหาก"),
    NO_SAUCE("ไม่รับน้ำจิ้ม")
}

data class AddonOptions(
    val skewer: SkewerOption = SkewerOption.SKEWERED,
    val friedChili: Boolean = true,
    val cucumber: Boolean = true,
    val sauceCup: Boolean = true,
    val sauceChoice: SauceChoice = SauceChoice.MIXED,
    val comment: String = ""
)

enum class FulfillmentType(val title: String) {
    DELIVERY("จัดส่งตามที่อยู่"),
    PICKUP("มารับเองหน้าร้าน")
}

enum class OrderStatus(val title: String, val stepIndex: Int) {
    PENDING("รอดำเนินการ", 0),
    COOKING("กำลังทำทอดร้อนๆ", 1),
    READY("เสร็จแล้วพร้อมรับ/ส่ง", 2),
    COMPLETED("สำเร็จแล้ว", 3),
    CANCELLED("ยกเลิก", -1)
}

data class OrderItem(
    val itemId: String,
    val name: String,
    val price: Double,
    val directCost: Double,
    val quantity: Int
)

data class Order(
    val id: String,
    val customerPhone: String = "",
    val isGuest: Boolean = true,
    val fulfillment: FulfillmentType = FulfillmentType.DELIVERY,
    val deliveryAddress: String = "",
    val gpsCoordinates: String = "",
    val items: List<OrderItem> = emptyList(),
    val options: AddonOptions = AddonOptions(),
    val comment: String = "",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val timestamp: String = "",
    val slipAttached: Boolean = true,
    val slipImageUri: String? = null
)

data class MemberUser(
    val phoneNumber: String,
    val name: String = "",
    val stamps: Int = 0,
    val totalOrders: Int = 0,
    val lastOrder: Order? = null
)

data class HiddenCostLog(
    val id: String,
    val date: String,
    val category: String, // "utilities", "supplies", "oil_gas", "rent"
    val itemName: String,
    val amount: Double,
    val notes: String = ""
)

data class CustomerConsumptionSummary(
    val totalSpent: Double = 0.0,
    val totalOrdersCount: Int = 0,
    val completedOrdersCount: Int = 0,
    val totalUnitsConsumed: Int = 0,
    val totalSavings: Double = 0.0
)

