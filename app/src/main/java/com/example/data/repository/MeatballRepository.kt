package com.example.data.repository

import android.content.Context
import com.example.data.model.AddonOptions
import com.example.data.model.Category
import com.example.data.model.FulfillmentType
import com.example.data.model.HiddenCostLog
import com.example.data.model.ItemStatus
import com.example.data.model.MemberUser
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.SauceChoice
import com.example.data.model.ShopInfo
import com.example.data.model.SkewerOption
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
        _categories.value = defaultCategories()

        val initialMenu = listOf(
            MenuItem(
                id = "item_01",
                name = "ลูกชิ้นเอ็นเนื้อ",
                categoryId = "cat_1",
                price = 10.0,
                directCost = 5.5,
                status = ItemStatus.ACTIVE,
                stockQty = 50,
                defaultResetQty = 50,
                isBestseller = true,
                emoji = "🍢",
                description = "เอ็นเนื้อแท้ กรุบกรอบ ทอดพองกำลังดี ไม้ละ 10.-"
            ),
            MenuItem(
                id = "item_02",
                name = "ลูกชิ้นหมูล้วนแท้",
                categoryId = "cat_1",
                price = 10.0,
                directCost = 5.0,
                status = ItemStatus.ACTIVE,
                stockQty = 50,
                defaultResetQty = 50,
                isBestseller = true,
                emoji = "🍡",
                description = "หมูบดเนียนนุ่ม ไม่ผสมแป้งเยอะ ทอดไม่อมน้ำมัน"
            ),
            MenuItem(
                id = "item_03",
                name = "ไส้กรอกแดงในตำนาน",
                categoryId = "cat_1",
                price = 10.0,
                directCost = 4.8,
                status = ItemStatus.ACTIVE,
                stockQty = 60,
                defaultResetQty = 60,
                isBestseller = true,
                emoji = "🌭",
                description = "ไส้กรอกแดงวัยเด็ก ทอดกรอบบั้งสวย น้ำจิ้มซึมเข้าเนื้อ"
            ),
            MenuItem(
                id = "item_04",
                name = "ลูกชิ้นปลากรายเยาวราช",
                categoryId = "cat_2",
                price = 12.0,
                directCost = 6.2,
                status = ItemStatus.ACTIVE,
                stockQty = 40,
                defaultResetQty = 40,
                isBestseller = false,
                emoji = "🍥",
                description = "เนื้อปลากรายนวดฟู เด้งสู้ฟัน ราดน้ำจิ้มซีฟู้ดเข้ากันมาก"
            ),
            MenuItem(
                id = "item_05",
                name = "ลูกชิ้นกุ้งทอดทรงเครื่อง",
                categoryId = "cat_1",
                price = 15.0,
                directCost = 7.5,
                status = ItemStatus.ACTIVE,
                stockQty = 35,
                defaultResetQty = 35,
                isBestseller = false,
                emoji = "🦐",
                description = "เนื้อกุ้งหอมละมุน ทอดสีเหลืองทองหอมฟุ้ง"
            ),
            MenuItem(
                id = "item_06",
                name = "เกี๊ยวกรอบทอดไส้หมู",
                categoryId = "cat_1",
                price = 10.0,
                directCost = 4.5,
                status = ItemStatus.ACTIVE,
                stockQty = 45,
                defaultResetQty = 45,
                isBestseller = true,
                emoji = "🥟",
                description = "แผ่นเกี๊ยวทอดกรอบสะท้านฟัน จิ้มน้ำจิ้มมะขามฟินเว่อร์"
            ),
            MenuItem(
                id = "item_07",
                name = "ชุดคอมโบรวมมิตร 6 ไม้",
                categoryId = "cat_3",
                price = 55.0,
                directCost = 28.0,
                status = ItemStatus.ACTIVE,
                stockQty = 25,
                defaultResetQty = 25,
                isBestseller = true,
                emoji = "⭐",
                description = "เอ็นเนื้อ 2, หมูล้วน 2, ไส้กรอกแดง 1, เกี๊ยวกรอบ 1 (ประหยัด 5.-)"
            ),
            MenuItem(
                id = "item_08",
                name = "ชุดปาร์ตี้อิ่มจุก 12 ไม้",
                categoryId = "cat_3",
                price = 99.0,
                directCost = 52.0,
                status = ItemStatus.ACTIVE,
                stockQty = 15,
                defaultResetQty = 15,
                isBestseller = false,
                emoji = "🎉",
                description = "รวมลูกชิ้นครบทุกแบบ 12 ไม้ ผักสดและน้ำจิ้มจุใจ (ประหยัด 21.-)"
            ),
            MenuItem(
                id = "item_09",
                name = "ชาดำเย็นโบราณ / ชามะนาว",
                categoryId = "cat_4",
                price = 20.0,
                directCost = 6.0,
                status = ItemStatus.ACTIVE,
                stockQty = 30,
                defaultResetQty = 30,
                isBestseller = false,
                emoji = "🧋",
                description = "ชาต้มสดหอมกรุ่น เย็นชื่นใจ ดับกระหาย"
            ),
            MenuItem(
                id = "item_10",
                name = "โค้กเย็นฉ่ำ (กระป๋อง)",
                categoryId = "cat_4",
                price = 15.0,
                directCost = 9.0,
                status = ItemStatus.ACTIVE,
                stockQty = 40,
                defaultResetQty = 40,
                isBestseller = false,
                emoji = "🥤",
                description = "โค้กแช่เย็นเจี๊ยบ ดื่มคู่ลูกชิ้นทอดแก้เลี่ยน"
            )
        )
        _menuItems.value = initialMenu.mapIndexed { index, item -> item.copy(sortOrder = index) }

        val sampleOrder = Order(
            id = "ORD-101",
            customerPhone = "0812345678",
            isGuest = false,
            fulfillment = FulfillmentType.DELIVERY,
            deliveryAddress = "บ้านเลขที่ 12/3 ซอยมิตรภาพ 14",
            gpsCoordinates = "16.4421, 102.8358",
            items = listOf(
                OrderItem(
                    itemId = "item_01",
                    name = "ลูกชิ้นเอ็นเนื้อ",
                    price = 10.0,
                    directCost = 5.5,
                    quantity = 5
                )
            ),
            options = AddonOptions(
                skewer = SkewerOption.SKEWERED,
                friedChili = true,
                cucumber = true,
                sauceCup = true,
                sauceChoice = SauceChoice.MIXED,
                comment = "ขอทอดกรอบๆ แยกน้ำจิ้ม"
            ),
            comment = "ขอทอดกรอบๆ แยกน้ำจิ้ม",
            subtotal = 50.0,
            discount = 5.0,
            deliveryFee = 0.0,
            totalAmount = 45.0,
            status = OrderStatus.PENDING,
            timestamp = "2026-11-04 12:00",
            slipAttached = true
        )

        val historyOrder1 = Order(
            id = "ORD-098",
            customerPhone = "0812345678",
            isGuest = false,
            fulfillment = FulfillmentType.DELIVERY,
            deliveryAddress = "บ้านเลขที่ 12/3 ซอยมิตรภาพ 14",
            gpsCoordinates = "16.4421, 102.8358",
            items = listOf(
                OrderItem(itemId = "item_02", name = "ลูกชิ้นหมูล้วนแท้", price = 10.0, directCost = 5.0, quantity = 3),
                OrderItem(itemId = "item_03", name = "ไส้กรอกแดงในตำนาน", price = 10.0, directCost = 4.8, quantity = 3)
            ),
            options = AddonOptions(
                skewer = SkewerOption.SKEWERED,
                friedChili = true,
                cucumber = true,
                sauceCup = true,
                sauceChoice = SauceChoice.TAMARIND,
                comment = "พริกทอดเยอะๆ ครับ"
            ),
            comment = "พริกทอดเยอะๆ ครับ",
            subtotal = 60.0,
            discount = 5.0,
            deliveryFee = 0.0,
            totalAmount = 55.0,
            status = OrderStatus.COMPLETED,
            timestamp = "2026-11-03 18:45",
            slipAttached = true
        )

        val historyOrder2 = Order(
            id = "ORD-089",
            customerPhone = "0812345678",
            isGuest = false,
            fulfillment = FulfillmentType.PICKUP,
            deliveryAddress = "",
            gpsCoordinates = "",
            items = listOf(
                OrderItem(itemId = "item_07", name = "ชุดคอมโบรวมมิตร 6 ไม้", price = 55.0, directCost = 28.0, quantity = 1),
                OrderItem(itemId = "item_09", name = "ชาดำเย็นโบราณ / ชามะนาว", price = 20.0, directCost = 6.0, quantity = 1)
            ),
            options = AddonOptions(
                skewer = SkewerOption.IN_BAG,
                friedChili = true,
                cucumber = true,
                sauceCup = false,
                sauceChoice = SauceChoice.MIXED
            ),
            subtotal = 75.0,
            discount = 5.0,
            deliveryFee = 0.0,
            totalAmount = 70.0,
            status = OrderStatus.COMPLETED,
            timestamp = "2026-11-01 12:30",
            slipAttached = true
        )

        val historyOrder3 = Order(
            id = "ORD-075",
            customerPhone = "0812345678",
            isGuest = false,
            fulfillment = FulfillmentType.DELIVERY,
            deliveryAddress = "บ้านเลขที่ 12/3 ซอยมิตรภาพ 14",
            gpsCoordinates = "16.4421, 102.8358",
            items = listOf(
                OrderItem(itemId = "item_06", name = "เกี๊ยวกรอบทอดไส้หมู", price = 10.0, directCost = 4.5, quantity = 3),
                OrderItem(itemId = "item_01", name = "ลูกชิ้นเอ็นเนื้อ", price = 10.0, directCost = 5.5, quantity = 2)
            ),
            options = AddonOptions(
                skewer = SkewerOption.SKEWERED,
                friedChili = false,
                cucumber = true,
                sauceCup = true,
                sauceChoice = SauceChoice.SEAFOOD
            ),
            subtotal = 50.0,
            discount = 5.0,
            deliveryFee = 0.0,
            totalAmount = 45.0,
            status = OrderStatus.COMPLETED,
            timestamp = "2026-10-29 19:15",
            slipAttached = true
        )

        val historyOrder4 = Order(
            id = "ORD-063",
            customerPhone = "0812345678",
            isGuest = false,
            fulfillment = FulfillmentType.DELIVERY,
            deliveryAddress = "บ้านเลขที่ 12/3 ซอยมิตรภาพ 14",
            gpsCoordinates = "16.4421, 102.8358",
            items = listOf(
                OrderItem(itemId = "item_04", name = "ลูกชิ้นปลากรายเยาวราช", price = 12.0, directCost = 6.2, quantity = 2),
                OrderItem(itemId = "item_10", name = "โค้กเย็นฉ่ำ (กระป๋อง)", price = 15.0, directCost = 9.0, quantity = 1)
            ),
            options = AddonOptions(
                skewer = SkewerOption.SEPARATE,
                friedChili = false,
                cucumber = false,
                sauceCup = true,
                sauceChoice = SauceChoice.TAMARIND
            ),
            subtotal = 39.0,
            discount = 0.0,
            deliveryFee = 15.0,
            totalAmount = 54.0,
            status = OrderStatus.CANCELLED,
            timestamp = "2026-10-24 16:00",
            slipAttached = false
        )

        val initialUser = MemberUser(
            phoneNumber = "0812345678",
            name = "ลูกค้าประจำ",
            stamps = 7,
            totalOrders = 5,
            lastOrder = sampleOrder
        )
        _users.value = mapOf("0812345678" to initialUser)

        _orders.value = listOf(sampleOrder, historyOrder1, historyOrder2, historyOrder3, historyOrder4)
        _currentActiveOrderId.value = sampleOrder.id
        _currentCustomerPhone.value = "0812345678"

        val initialCosts = listOf(
            HiddenCostLog(
                id = "cost_1",
                date = "2026-11-04",
                category = "utilities",
                itemName = "เติมแก๊สหุงต้ม 15 กก.",
                amount = 390.0,
                notes = "ถังใหม่ ใช้ทอดได้ ~300 กระทะ"
            ),
            HiddenCostLog(
                id = "cost_2",
                date = "2026-11-02",
                category = "oil_gas",
                itemName = "น้ำมันปาล์มทอด 2 ปี๊บ",
                amount = 540.0,
                notes = "เปลี่ยนน้ำมันใหม่ใสสะอาด"
            ),
            HiddenCostLog(
                id = "cost_3",
                date = "2026-11-01",
                category = "supplies",
                itemName = "ถุงหิ้ว + ถ้วยน้ำจิ้ม + ไม้เสียบ",
                amount = 220.0,
                notes = "วัสดุสิ้นเปลืองประจำเดือน"
            ),
            HiddenCostLog(
                id = "cost_4",
                date = "2026-11-03",
                category = "supplies",
                itemName = "พริกสดทอด + แตงกวา 10 กก.",
                amount = 180.0,
                notes = "ผักเคียงสดกรอบ"
            )
        )
        _hiddenCosts.value = initialCosts
    }

    fun setCustomerPhone(phone: String) {
        val clean = phone.trim()
        _currentCustomerPhone.value = clean
        if (clean.isNotEmpty() && !_users.value.containsKey(clean)) {
            val newUser = MemberUser(phoneNumber = clean, stamps = 1, totalOrders = 0)
            _users.value = _users.value + (clean to newUser)
        }
        persistState()
    }

    fun addOrder(newOrder: Order) {
        val currentOrders = _orders.value.toMutableList()
        currentOrders.add(0, newOrder)
        _orders.value = currentOrders
        _currentActiveOrderId.value = newOrder.id

        // Auto deduct stock
        val currentMenu = _menuItems.value.toMutableList()
        newOrder.items.forEach { orderItem ->
            val index = currentMenu.indexOfFirst { it.id == orderItem.itemId }
            if (index != -1) {
                val item = currentMenu[index]
                val updatedStock = (item.stockQty - orderItem.quantity).coerceAtLeast(0)
                val newStatus = if (updatedStock == 0) ItemStatus.OUT_OF_STOCK else item.status
                currentMenu[index] = item.copy(stockQty = updatedStock, status = newStatus)
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
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) {
                order.copy(status = newStatus)
            } else order
        }
        persistState()
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
            item.copy(
                stockQty = item.defaultResetQty,
                status = if (item.defaultResetQty > 0 && item.status == ItemStatus.OUT_OF_STOCK) ItemStatus.ACTIVE else item.status
            )
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

    fun addMemberStamp(phone: String, count: Int = 1) {
        val clean = phone.trim()
        if (clean.isEmpty()) return
        val existing = _users.value[clean] ?: MemberUser(phoneNumber = clean)
        val updated = existing.copy(stamps = existing.stamps + count)
        _users.value = _users.value + (clean to updated)
        persistState()
    }

    fun redeemReward(phone: String): Boolean {
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
