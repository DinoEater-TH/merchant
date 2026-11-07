package com.example.data.repository

import android.content.Context
import com.example.data.model.Category
import com.example.data.model.HiddenCostLog
import com.example.data.model.ItemStatus
import com.example.data.model.MemberUser
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.ShopInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BreakEvenSummary(
    val totalDirectCostThisMonth: Double,
    val totalHiddenCostThisMonth: Double,
    val totalRevenueThisMonth: Double,
    val totalOrdersCount: Int,
    val totalUnitsSold: Int,
    val averageSellingPricePerUnit: Double,
    val averageDirectCostPerUnit: Double,
    val averageContributionMarginPerUnit: Double,
    val breakEvenUnitsNeeded: Int,
    val progressToBreakEvenPercent: Float,
    val gasTankRemainingPercent: Int,
    val estimatedGasDaysRemaining: Int,
    val smartAdvices: List<String>
)

class MeatballRepository(context: Context? = null) {

    private val preferences = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val stateAdapter = if (context != null) {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter(PersistedState::class.java)
    } else {
        null
    }

    private val _shopInfo = MutableStateFlow(ShopInfo())
    val shopInfo: StateFlow<ShopInfo> = _shopInfo.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _users = MutableStateFlow<Map<String, MemberUser>>(emptyMap())
    val users: StateFlow<Map<String, MemberUser>> = _users.asStateFlow()

    private val _hiddenCosts = MutableStateFlow<List<HiddenCostLog>>(emptyList())
    val hiddenCosts: StateFlow<List<HiddenCostLog>> = _hiddenCosts.asStateFlow()

    private val _currentActiveOrderId = MutableStateFlow<String?>(null)
    val currentActiveOrderId: StateFlow<String?> = _currentActiveOrderId.asStateFlow()

    private val _currentCustomerPhone = MutableStateFlow<String>("")
    val currentCustomerPhone: StateFlow<String> = _currentCustomerPhone.asStateFlow()

    private val _adminPin = MutableStateFlow(DEFAULT_ADMIN_PIN)
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    @Volatile
    var isApplyingRemote: Boolean = false

    init {
        if (!restoreState()) {
            loadInitialData()
            persistState()
        }
    }

    private fun restoreState(): Boolean {
        val adapter = stateAdapter ?: return false
        val saved = preferences?.getString(KEY_STATE, null) ?: return false
        return try {
            val state = adapter.fromJson(saved) ?: return false
            _shopInfo.value = state.shopInfo
            _categories.value = state.categories.ifEmpty { defaultCategories() }
            _menuItems.value = state.menuItems
            _orders.value = state.orders
            _users.value = state.users
            _hiddenCosts.value = state.hiddenCosts
            _currentCustomerPhone.value = state.currentCustomerPhone
            _currentActiveOrderId.value = state.currentActiveOrderId
            _adminPin.value = state.adminPin.ifBlank { DEFAULT_ADMIN_PIN }
            _menuItems.value.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun persistState() {
        val adapter = stateAdapter ?: return
        val editor = preferences?.edit() ?: return
        val state = PersistedState(
            shopInfo = _shopInfo.value,
            categories = _categories.value,
            menuItems = _menuItems.value,
            orders = _orders.value,
            users = _users.value,
            hiddenCosts = _hiddenCosts.value,
            currentCustomerPhone = _currentCustomerPhone.value,
            currentActiveOrderId = _currentActiveOrderId.value,
            adminPin = _adminPin.value
        )
        editor.putString(KEY_STATE, adapter.toJson(state)).apply()
    }

    fun applyRemoteState(state: PersistedState) {
        isApplyingRemote = true
        try {
            _shopInfo.value = state.shopInfo
            _categories.value = state.categories.ifEmpty { defaultCategories() }
            _menuItems.value = state.menuItems
            _orders.value = state.orders
            _users.value = state.users
            _hiddenCosts.value = state.hiddenCosts
            persistState()
        } finally {
            isApplyingRemote = false
        }
    }

    data class PersistedState(
        val shopInfo: ShopInfo = ShopInfo(),
        val categories: List<Category> = emptyList(),
        val menuItems: List<MenuItem> = emptyList(),
        val orders: List<Order> = emptyList(),
        val users: Map<String, MemberUser> = emptyMap(),
        val hiddenCosts: List<HiddenCostLog> = emptyList(),
        val currentCustomerPhone: String = "",
        val currentActiveOrderId: String? = null,
        val adminPin: String = DEFAULT_ADMIN_PIN
    )

    companion object {
        private const val PREFS_NAME = "meatball_shop_state"
        private const val KEY_STATE = "state_json"
        private const val DEFAULT_ADMIN_PIN = "09286840"
    }

    private fun defaultCategories(): List<Category> = listOf(
        Category(id = "all", name = "ทั้งหมด", sort = 0, iconEmoji = "🥢"),
        Category(id = "cat_1", name = "ลูกชิ้นทอด", sort = 1, iconEmoji = "🍢"),
        Category(id = "cat_2", name = "ลูกชิ้นปิ้ง/นึ่ง", sort = 2, iconEmoji = "🍡"),
        Category(id = "cat_3", name = "ชุดสุดคุ้ม Combo", sort = 3, iconEmoji = "⭐"),
        Category(id = "cat_4", name = "เครื่องดื่ม & เคียง", sort = 4, iconEmoji = "🥤")
    )

    private fun loadInitialData() {
        // Start empty: the shop's real data is loaded from Firebase / persisted state.
        _categories.value = defaultCategories()
        _menuItems.value = emptyList()
        _orders.value = emptyList()
        _users.value = emptyMap()
        _hiddenCosts.value = emptyList()
        _currentActiveOrderId.value = null
        _currentCustomerPhone.value = ""
    }

    fun setCustomerPhone(phone: String, name: String = "") {
        val clean = phone.trim()
        _currentCustomerPhone.value = clean
        if (clean.isNotEmpty()) {
            val existing = _users.value[clean]
            if (existing == null) {
                val newUser = MemberUser(phoneNumber = clean, name = name.trim(), stamps = 1, totalOrders = 0)
                _users.value = _users.value + (clean to newUser)
            } else if (name.trim().isNotEmpty() && existing.name.isBlank()) {
                _users.value = _users.value + (clean to existing.copy(name = name.trim()))
            }
        }
        persistState()
    }

    fun addOrder(newOrder: Order) {
        val currentOrders = _orders.value.toMutableList()
        currentOrders.add(0, newOrder)
        _orders.value = currentOrders
        _currentActiveOrderId.value = newOrder.id

        // Auto deduct stock (only when the item tracks limited stock)
        val currentMenu = _menuItems.value.toMutableList()
        newOrder.items.forEach { orderItem ->
            val index = currentMenu.indexOfFirst { it.id == orderItem.itemId }
            if (index != -1) {
                val item = currentMenu[index]
                if (!item.isUnlimitedStock) {
                    val updatedStock = (item.stockQty - orderItem.quantity).coerceAtLeast(0)
                    val newStatus = if (updatedStock == 0) ItemStatus.OUT_OF_STOCK else item.status
                    currentMenu[index] = item.copy(stockQty = updatedStock, status = newStatus)
                }
            }
        }
        _menuItems.value = currentMenu

        // Update member stamps if phone provided
        if (newOrder.customerPhone.isNotEmpty()) {
            val phone = newOrder.customerPhone
            val existing = _users.value[phone] ?: MemberUser(phoneNumber = phone)
            val updatedUser = existing.copy(
                stamps = (existing.stamps + 1),
                totalOrders = existing.totalOrders + 1,
                lastOrder = newOrder
            )
            _users.value = _users.value + (phone to updatedUser)
        }
        persistState()
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        // Cancelling an order returns its limited-stock items to inventory.
        if (newStatus == OrderStatus.CANCELLED) {
            val order = _orders.value.firstOrNull { it.id == orderId }
            val wasActive = order != null && order.status != OrderStatus.CANCELLED && order.status != OrderStatus.COMPLETED
            if (order != null && wasActive) restoreStockForItems(order.items)
        }
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) {
                order.copy(status = newStatus)
            } else order
        }
        persistState()
    }

    fun removeOrderItem(orderId: String, itemId: String) {
        val order = _orders.value.firstOrNull { it.id == orderId } ?: return
        val removed = order.items.filter { it.itemId == itemId }
        if (removed.isEmpty()) return
        if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
            restoreStockForItems(removed)
        }
        val shop = _shopInfo.value
        _orders.value = _orders.value.map { current ->
            if (current.id != orderId) return@map current
            val remaining = current.items.filterNot { it.itemId == itemId }
            val subtotal = remaining.sumOf { it.price * it.quantity }
            val discount = if (shop.promoDiscountEnabled && subtotal >= shop.minOrderForDiscount) shop.discountAmount else 0.0
            val total = (subtotal - discount + current.deliveryFee).coerceAtLeast(0.0)
            current.copy(
                items = remaining,
                subtotal = subtotal,
                discount = discount,
                totalAmount = total,
                status = if (remaining.isEmpty()) OrderStatus.CANCELLED else current.status
            )
        }
        persistState()
    }

    fun deleteOrder(orderId: String) {
        _orders.value = _orders.value.filterNot { it.id == orderId }
        persistState()
    }

    private fun restoreStockForItems(items: List<OrderItem>) {
        val menu = _menuItems.value.toMutableList()
        items.forEach { orderItem ->
            val index = menu.indexOfFirst { it.id == orderItem.itemId }
            if (index != -1) {
                val item = menu[index]
                if (!item.isUnlimitedStock) {
                    val restored = item.stockQty + orderItem.quantity
                    menu[index] = item.copy(
                        stockQty = restored,
                        status = if (restored > 0 && item.status == ItemStatus.OUT_OF_STOCK) ItemStatus.ACTIVE else item.status
                    )
                }
            }
        }
        _menuItems.value = menu
    }

    fun updateItemStatus(itemId: String, newStatus: ItemStatus) {
        _menuItems.value = _menuItems.value.map { item ->
            if (item.id == itemId) item.copy(status = newStatus) else item
        }
        persistState()
    }

    fun updateItemStock(itemId: String, newStock: Int) {
        _menuItems.value = _menuItems.value.map { item ->
            if (item.id == itemId) {
                val clamped = newStock.coerceAtLeast(0)
                val status = if (clamped == 0) ItemStatus.OUT_OF_STOCK else {
                    if (item.status == ItemStatus.OUT_OF_STOCK) ItemStatus.ACTIVE else item.status
                }
                item.copy(stockQty = clamped, status = status)
            } else item
        }
        persistState()
    }

    fun oneClickStockReset() {
        _menuItems.value = _menuItems.value.map { item ->
            if (item.isUnlimitedStock) {
                item.copy(status = if (item.status == ItemStatus.OUT_OF_STOCK) ItemStatus.ACTIVE else item.status)
            } else {
                item.copy(
                    stockQty = item.defaultResetQty,
                    status = if (item.defaultResetQty > 0 && item.status == ItemStatus.OUT_OF_STOCK) ItemStatus.ACTIVE else item.status
                )
            }
        }
        persistState()
    }

    fun moveMenuItem(itemId: String, direction: Int) {
        val items = _menuItems.value.toMutableList()
        val targetIndex = items.indexOfFirst { it.id == itemId }
        if (targetIndex == -1) return
        val targetItem = items[targetIndex]
        val sameCategory = items.filter { it.categoryId == targetItem.categoryId }.sortedBy { it.sortOrder }
        val pos = sameCategory.indexOfFirst { it.id == itemId }
        val newPos = pos + direction
        if (newPos < 0 || newPos >= sameCategory.size) return
        val otherItem = sameCategory[newPos]
        _menuItems.value = items.map { item ->
            when (item.id) {
                targetItem.id -> item.copy(sortOrder = otherItem.sortOrder)
                otherItem.id -> item.copy(sortOrder = targetItem.sortOrder)
                else -> item
            }
        }
        persistState()
    }

fun addOrUpdateMenuItem(item: MenuItem) {
        val current = _menuItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item
        } else {
            current.add(item)
        }
        _menuItems.value = current
        persistState()
    }

    fun deleteMenuItem(itemId: String) {
        _menuItems.value = _menuItems.value.filterNot { it.id == itemId }
        persistState()
    }

    fun setItemUnlimitedStock(itemId: String, unlimited: Boolean) {
        _menuItems.value = _menuItems.value.map { item ->
            if (item.id == itemId) {
                item.copy(
                    isUnlimitedStock = unlimited,
                    status = if (unlimited) {
                        if (item.status == ItemStatus.OUT_OF_STOCK) ItemStatus.ACTIVE else item.status
                    } else item.status
                )
            } else item
        }
        persistState()
    }

    fun addMemberStamp(phone: String, count: Int = 1) {
        val clean = phone.trim()
        if (clean.isEmpty()) return
        val existing = _users.value[clean] ?: MemberUser(phoneNumber = clean)
        val updated = existing.copy(stamps = existing.stamps + count)
        _users.value = _users.value + (clean to updated)
        persistState()
    }

    fun redeemReward(phone: String): Boolean {
        if (!_shopInfo.value.promoRewardEnabled) return false
        val clean = phone.trim()
        val existing = _users.value[clean] ?: return false
        val cost = _shopInfo.value.stampsRequiredForReward
        if (existing.stamps >= cost) {
            val updated = existing.copy(stamps = existing.stamps - cost)
            _users.value = _users.value + (clean to updated)
            persistState()
            return true
        }
        return false
    }

    fun addHiddenCost(itemName: String, category: String, amount: Double, notes: String = "") {
        val newId = "cost_${System.currentTimeMillis()}"
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val log = HiddenCostLog(newId, date, category, itemName, amount, notes)
        _hiddenCosts.value = listOf(log) + _hiddenCosts.value
        persistState()
    }

    fun setCurrentActiveOrderId(orderId: String) {
        _currentActiveOrderId.value = orderId
        persistState()
    }

    fun updateShopInfo(info: ShopInfo) {
        _shopInfo.value = info
        persistState()
    }

    fun setAdminPin(pin: String) {
        if (pin.length in 4..8 && pin.all { it.isDigit() }) {
            _adminPin.value = pin
            persistState()
        }
    }

    fun calculateBreakEvenSummary(): BreakEvenSummary {
        val ordersList = _orders.value.filter { it.status != OrderStatus.CANCELLED }
        var totalRevenue = 0.0
        var totalDirectCost = 0.0
        var totalUnits = 0

        ordersList.forEach { order ->
            totalRevenue += order.totalAmount
            order.items.forEach { item ->
                totalUnits += item.quantity
                totalDirectCost += (item.directCost * item.quantity)
            }
        }

        val totalHidden = _hiddenCosts.value.sumOf { it.amount }

        val avgSellingPrice = if (totalUnits > 0) (totalRevenue / totalUnits) else 10.0
        val avgDirectCost = if (totalUnits > 0) (totalDirectCost / totalUnits) else 5.2
        val contributionMargin = (avgSellingPrice - avgDirectCost).coerceAtLeast(1.0)

        val breakEvenUnits = if (contributionMargin > 0) {
            kotlin.math.ceil(totalHidden / contributionMargin).toInt()
        } else 0

        val progress = if (breakEvenUnits > 0) {
            (totalUnits.toFloat() / breakEvenUnits.toFloat()).coerceIn(0f, 1f)
        } else 1f

        // Gas status estimation (15 kg tank ~ 300 orders)
        val ordersUsingCurrentTank = (ordersList.size * 3) % 300
        val gasRemaining = (100 - (ordersUsingCurrentTank * 100 / 300)).coerceIn(10, 100)
        val daysRemaining = (gasRemaining * 15) / 100

        val advices = mutableListOf<String>()
        if (avgSellingPrice < 12.0) {
            advices.add("💡 แนะนำจัดชุด Combo Set 6 ไม้ (55 บาท) เพื่อเพิ่มยอดใช้จ่ายเฉลี่ยต่อออเดอร์ (Ticket Size)")
        }
        if (gasRemaining <= 25) {
            advices.add("⚠️ ถังแก๊สใกล้หมด (~$gasRemaining%) แนะนำสั่งเติมแก๊สสำรองไว้ล่วงหน้า")
        }
        advices.add("🔥 ลูกชิ้นเอ็นเนื้อ และ ไส้กรอกแดง ให้ Margin ดีที่สุด ควรวางตำแหน่งเด่นบนสุด")
        advices.add("✨ ยอดขายสะสมแต้มช่วยดึงดูดลูกค้าประจำสั่งซ้ำเพิ่มขึ้นกว่า 35%")

        return BreakEvenSummary(
            totalDirectCostThisMonth = totalDirectCost,
            totalHiddenCostThisMonth = totalHidden,
            totalRevenueThisMonth = totalRevenue,
            totalOrdersCount = ordersList.size,
            totalUnitsSold = totalUnits,
            averageSellingPricePerUnit = avgSellingPrice,
            averageDirectCostPerUnit = avgDirectCost,
            averageContributionMarginPerUnit = contributionMargin,
            breakEvenUnitsNeeded = breakEvenUnits,
            progressToBreakEvenPercent = progress,
            gasTankRemainingPercent = gasRemaining,
            estimatedGasDaysRemaining = daysRemaining,
            smartAdvices = advices
        )
    }
}
