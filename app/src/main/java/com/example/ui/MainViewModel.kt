package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AddonOptions
import com.example.data.model.FulfillmentType
import com.example.data.model.ItemStatus
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.ShopInfo
import com.example.data.model.CustomPromo
import com.example.data.repository.FirebaseSyncManager
import com.example.data.repository.MeatballRepository
import com.example.util.DeliveryRouteHelper
import com.example.util.AppUpdater
import com.example.util.AppVisibility
import com.example.util.NewOrderMessagingService
import com.example.util.NotificationHelper
import com.example.util.OrderAlertService
import com.example.util.SoundHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MeatballRepository(application)
    val firebaseSync = FirebaseSyncManager(application, repository).apply { start() }
    val syncStatus: StateFlow<String> = firebaseSync.status

    val shopInfo = repository.shopInfo
    val categories = repository.categories
    val menuItems = repository.menuItems
    val orders = repository.orders
    val users = repository.users
    val hiddenCosts = repository.hiddenCosts
    val currentActiveOrderId = repository.currentActiveOrderId

    // Navigation & Mode
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _isPinDialogOpen = MutableStateFlow(false)
    val isPinDialogOpen: StateFlow<Boolean> = _isPinDialogOpen.asStateFlow()

    val adminPin: StateFlow<String> = repository.adminPin

    // In-app self update (sideloaded app, not Play Store)
    private val _updateInfo = MutableStateFlow<com.example.util.UpdateInfo?>(null)
    val updateInfo: StateFlow<com.example.util.UpdateInfo?> = _updateInfo.asStateFlow()
    private val _updateStatus = MutableStateFlow("")
    val updateStatus: StateFlow<String> = _updateStatus.asStateFlow()
    private val _updateProgress = MutableStateFlow(-1)
    val updateProgress: StateFlow<Int> = _updateProgress.asStateFlow()
    private val _updateDismissed = MutableStateFlow(false)
    val updateDismissed: StateFlow<Boolean> = _updateDismissed.asStateFlow()
    private var updateChecked = false

    // Incoming order alert (admin mode only)
    private val locallySubmittedOrderIds = mutableSetOf<String>()
    private var knownOrderIds: Set<String>? = null

    init {
        // Initialize sound settings from SharedPreferences
        SoundHelper.init(getApplication())
        // Auto-login if the admin has logged in before on this device (PIN remembered).
        if (repository.hasAdminLoggedIn()) {
            _isAdminMode.value = true
            knownOrderIds = orders.value.map { it.id }.toSet()
            registerAdminPushToken()
        }
        viewModelScope.launch {
            orders.collect { list ->
                if (!_isAdminMode.value) {
                    knownOrderIds = null
                    return@collect
                }
                val currentIds = list.map { it.id }.toSet()
                val known = knownOrderIds
                if (known == null) {
                    knownOrderIds = currentIds
                    return@collect
                }
                val incoming = list.filter { order ->
                    order.id !in known &&
                        order.status == OrderStatus.PENDING &&
                        !isPosOrder(order) &&
                        order.id !in locallySubmittedOrderIds &&
                        isRecentOrder(order)
                }
                knownOrderIds = currentIds
                if (incoming.isNotEmpty()) {
                    // Avoid double alerts: when the foreground service is alive
                    // and the app is in the background, the service alerts.
                    if (AppVisibility.isForeground || !OrderAlertService.isRunning) {
                        SoundHelper.playNewOrderAlert(getApplication())
                        val latest = incoming.lastOrNull()
                        NotificationHelper.showNewOrderNotification(
                            getApplication(),
                            latest?.id ?: "ออเดอร์ใหม่",
                            latest?.totalAmount
                        )
                    }
                }
            }
        }
    }

    private fun isPosOrder(order: Order): Boolean =
        order.comment.trim().uppercase(Locale.US).startsWith("POS")

    private fun isRecentOrder(order: Order, windowMinutes: Long = 30): Boolean {
        val timestamp = order.timestamp.trim()
        if (timestamp.isEmpty()) return true
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(timestamp)
                ?: return true
            System.currentTimeMillis() - parsed.time <= windowMinutes * 60_000L
        } catch (_: Exception) {
            true
        }
    }

    private val _adminSelectedTab = MutableStateFlow(1)
    val adminSelectedTab: StateFlow<Int> = _adminSelectedTab.asStateFlow()

    private val _adminOrderFilter = MutableStateFlow<OrderStatus?>(null)
    val adminOrderFilter: StateFlow<OrderStatus?> = _adminOrderFilter.asStateFlow()

    // Customer State
    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart: StateFlow<Map<String, Int>> = _cart.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _isPhoneInputDialogOpen = MutableStateFlow(false)
    val isPhoneInputDialogOpen: StateFlow<Boolean> = _isPhoneInputDialogOpen.asStateFlow()

    private val _fulfillmentType = MutableStateFlow(FulfillmentType.DELIVERY)
    val fulfillmentType: StateFlow<FulfillmentType> = _fulfillmentType.asStateFlow()

    private val _deliveryAddress = MutableStateFlow("บ้านเลขที่ 12/3 ซอยมิตรภาพ 14")
    val deliveryAddress: StateFlow<String> = _deliveryAddress.asStateFlow()

    private val _gpsCoordinates = MutableStateFlow("")
    val gpsCoordinates: StateFlow<String> = _gpsCoordinates.asStateFlow()

    private val _deliveryDistanceKm = MutableStateFlow<Double?>(null)
    val deliveryDistanceKm: StateFlow<Double?> = _deliveryDistanceKm.asStateFlow()

    private val _deliveryDistanceLoading = MutableStateFlow(false)
    val deliveryDistanceLoading: StateFlow<Boolean> = _deliveryDistanceLoading.asStateFlow()

    private val _deliveryDistanceError = MutableStateFlow<String?>(null)
    val deliveryDistanceError: StateFlow<String?> = _deliveryDistanceError.asStateFlow()

    private val _slipImageUri = MutableStateFlow<String?>(null)
    val slipImageUri: StateFlow<String?> = _slipImageUri.asStateFlow()

    private val _addonOptions = MutableStateFlow(AddonOptions())
    val addonOptions: StateFlow<AddonOptions> = _addonOptions.asStateFlow()

    // Dialog flags
    private val _isAddonModalOpen = MutableStateFlow(false)
    val isAddonModalOpen: StateFlow<Boolean> = _isAddonModalOpen.asStateFlow()

    private val _isPaymentModalOpen = MutableStateFlow(false)
    val isPaymentModalOpen: StateFlow<Boolean> = _isPaymentModalOpen.asStateFlow()

    private val _isSlipAttached = MutableStateFlow(false)
    val isSlipAttached: StateFlow<Boolean> = _isSlipAttached.asStateFlow()

    private val _isTrackerModalOpen = MutableStateFlow(false)
    val isTrackerModalOpen: StateFlow<Boolean> = _isTrackerModalOpen.asStateFlow()

    private val _isOrderHistoryDialogOpen = MutableStateFlow(false)
    val isOrderHistoryDialogOpen: StateFlow<Boolean> = _isOrderHistoryDialogOpen.asStateFlow()

    private val _printingOrder = MutableStateFlow<Order?>(null)
    val printingOrder: StateFlow<Order?> = _printingOrder.asStateFlow()

    // Customer-specific orders list
    val customerOrders: StateFlow<List<Order>> = combine(
        orders,
        _customerPhone
    ) { allOrders, phone ->
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty()) {
            allOrders.filter { it.isGuest || it.customerPhone.isEmpty() }
        } else {
            allOrders.filter { it.customerPhone == cleanPhone }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Customer consumption summary
    val customerConsumption: StateFlow<com.example.data.model.CustomerConsumptionSummary> = customerOrders.map { custOrders ->
        var totalSpent = 0.0
        var totalUnits = 0
        var totalSavings = 0.0
        var completedCount = 0
        var validOrdersCount = 0

        custOrders.forEach { order ->
            if (order.status != OrderStatus.CANCELLED) {
                totalSpent += order.totalAmount
                totalSavings += order.discount
                validOrdersCount++
                if (order.status == OrderStatus.COMPLETED) {
                    completedCount++
                }
                order.items.forEach { item ->
                    totalUnits += item.quantity
                }
            }
        }

        com.example.data.model.CustomerConsumptionSummary(
            totalSpent = totalSpent,
            totalOrdersCount = validOrdersCount,
            completedOrdersCount = completedCount,
            totalUnitsConsumed = totalUnits,
            totalSavings = totalSavings
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.data.model.CustomerConsumptionSummary()
    )

    // Computed Cart Totals
    val cartSummary = combine(
        _cart,
        menuItems,
        shopInfo,
        _fulfillmentType,
        _deliveryDistanceKm
    ) { cartMap: Map<String, Int>, items: List<MenuItem>, shop: ShopInfo, fulfillment: FulfillmentType, distanceKm: Double? ->
        var subtotal = 0.0
        var totalItems = 0
        val orderItems = mutableListOf<OrderItem>()

        cartMap.forEach { (itemId, qty) ->
            if (qty > 0) {
                val item = items.find { it.id == itemId }
                if (item != null) {
                    val lineTotal = item.price * qty
                    subtotal += lineTotal
                    totalItems += qty
                    orderItems.add(
                        OrderItem(
                            itemId = item.id,
                            name = item.name,
                            price = item.price,
                            directCost = item.directCost,
                            quantity = qty
                        )
                    )
                }
            }
        }

        // Auto promotions (admin can enable/disable each one)
        var discount = if (shop.promoDiscountEnabled && subtotal >= shop.minOrderForDiscount) shop.discountAmount else 0.0
        // Custom promotions (percentOff / fixedOff)
        val totalItemsCount = totalItems
        customPromos.value.filter { it.enabled }.forEach { promo ->
            if (promo.type == "percentOff" && totalItemsCount >= promo.minQty) {
                discount += (subtotal * promo.discountPercent / 100.0)
            } else if (promo.type == "fixedOff" && totalItemsCount >= promo.minQty) {
                discount += promo.discountAmount
            }
        }

        // Delivery fee is calculated from the real driving distance at the admin's per-km rate.
        val freeDelivery = shop.promoFreeDeliveryEnabled && subtotal >= shop.minOrderForFreeDelivery
        val deliveryFee = if (fulfillment == FulfillmentType.DELIVERY) {
            if (!shop.deliveryGpsEnabled) {
                when {
                    freeDelivery -> 0.0
                    !shop.promoDeliveryFeeEnabled -> 0.0
                    else -> shop.deliveryFee
                }
            } else {
                when {
                    freeDelivery -> 0.0
                    distanceKm == null -> 0.0
                    shop.promoDeliveryPerKmEnabled && shop.deliveryFeePerKm > 0.0 -> ceil(distanceKm * shop.deliveryFeePerKm)
                    !shop.promoDeliveryFeeEnabled -> 0.0
                    else -> shop.deliveryFee
                }
            }
        } else 0.0

        val isDeliveryBlocked = if (!shop.deliveryGpsEnabled) {
            false
        } else {
            fulfillment == FulfillmentType.DELIVERY &&
                (distanceKm == null || distanceKm > shop.maxDeliveryDistanceKm)
        }

        val total = (subtotal - discount + deliveryFee).coerceAtLeast(0.0)

        CartCalculation(
            subtotal = subtotal,
            discount = discount,
            deliveryFee = deliveryFee,
            totalAmount = total,
            totalItemsCount = totalItems,
            orderItems = orderItems,
            distanceKm = distanceKm,
            isDeliveryBlocked = isDeliveryBlocked,
            maxDeliveryDistanceKm = shop.maxDeliveryDistanceKm
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CartCalculation()
    )

    fun selectCategory(catId: String) {
        _selectedCategoryId.value = catId
    }

    fun addToCart(item: MenuItem) {
        if (item.status == ItemStatus.OUT_OF_STOCK) return
        if (!item.isUnlimitedStock && item.stockQty <= 0) return
        val current = _cart.value.toMutableMap()
        val currentQty = current[item.id] ?: 0
        if (item.isUnlimitedStock || currentQty < item.stockQty) {
            current[item.id] = currentQty + 1
            _cart.value = current
        }
    }

    fun removeFromCart(item: MenuItem) {
        val current = _cart.value.toMutableMap()
        val currentQty = current[item.id] ?: 0
        if (currentQty > 1) {
            current[item.id] = currentQty - 1
        } else {
            current.remove(item.id)
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun setCustomerPhone(phone: String, name: String = "") {
        _customerPhone.value = phone
        repository.setCustomerPhone(phone, name)
    }

    fun addOrUpdateMember(phone: String, name: String = "") {
        repository.addOrUpdateMember(phone, name)
    }

    fun deleteMember(phone: String) {
        repository.deleteMember(phone)
    }

    fun setFulfillment(type: FulfillmentType) {
        _fulfillmentType.value = type
        if (type == FulfillmentType.DELIVERY) refreshDeliveryQuote()
    }

    fun setDeliveryAddress(addr: String) {
        _deliveryAddress.value = addr
    }

    fun setGpsCoordinates(coordinates: String) {
        _gpsCoordinates.value = coordinates
        refreshDeliveryQuote()
    }

    /**
     * Ask OSRM for the driving distance from the shop to the attached GPS point.
     * The result drives both the delivery fee and the 20 km service limit.
     */
    fun refreshDeliveryQuote() {
        val shop = shopInfo.value
        // GPS disabled by admin: no quote is needed, the flat fee is used.
        if (!shop.deliveryGpsEnabled) {
            _deliveryDistanceKm.value = null
            _deliveryDistanceLoading.value = false
            _deliveryDistanceError.value = null
            return
        }
        val destination = DeliveryRouteHelper.parseCoordinate(_gpsCoordinates.value)
        val shopCoordinate = if (shop.shopLat != null && shop.shopLng != null) {
            DeliveryRouteHelper.Coordinate(shop.shopLat, shop.shopLng)
        } else null

        if (destination == null || shopCoordinate == null) {
            _deliveryDistanceKm.value = null
            _deliveryDistanceLoading.value = false
            _deliveryDistanceError.value = when {
                destination == null -> "กรุณาแนบพิกัด GPS จุดส่งเพื่อคำนวณค่าส่ง"
                else -> "ร้านยังไม่ได้ตั้งพิกัด กรุณาให้แอดมินตั้งค่าพิกัดร้านก่อน"
            }
            return
        }

        _deliveryDistanceLoading.value = true
        _deliveryDistanceError.value = null
        viewModelScope.launch {
            val distance = DeliveryRouteHelper.drivingDistanceKm(shopCoordinate, destination)
            _deliveryDistanceLoading.value = false
            if (distance == null) {
                _deliveryDistanceKm.value = null
                _deliveryDistanceError.value = "ไม่สามารถคำนวณเส้นทางได้ กรุณาลองใหม่อีกครั้ง"
            } else {
                _deliveryDistanceKm.value = distance
                _deliveryDistanceError.value = if (distance > shop.maxDeliveryDistanceKm) {
                    "ระยะทาง ${String.format(Locale.US, "%.1f", distance)} กม. เกิน ${String.format(Locale.US, "%.0f", shop.maxDeliveryDistanceKm)} กม. ไม่สามารถจัดส่งได้"
                } else null
            }
        }
    }

    fun clearDeliveryQuote() {
        _deliveryDistanceKm.value = null
        _deliveryDistanceError.value = null
        _deliveryDistanceLoading.value = false
    }

    fun setSlipImage(uri: String?) {
        _slipImageUri.value = uri
        _isSlipAttached.value = uri != null
    }

    fun setAddonOptions(options: AddonOptions) {
        _addonOptions.value = options
    }

    fun openAddonModal() {
        _isAddonModalOpen.value = true
    }

    fun closeAddonModal() {
        _isAddonModalOpen.value = false
    }

    fun openPaymentModal() {
        _isAddonModalOpen.value = false
        _isPaymentModalOpen.value = true
    }

    fun confirmCashOrder() {
        _isSlipAttached.value = false
        _slipImageUri.value = null
        confirmOrderAndSubmit()
    }

    fun closePaymentModal() {
        _isPaymentModalOpen.value = false
    }

    fun toggleSlipAttached(attached: Boolean) {
        _isSlipAttached.value = attached
    }

    fun confirmOrderAndSubmit() {
        val calculation = cartSummary.value
        if (calculation.orderItems.isEmpty()) return
        if (_fulfillmentType.value == FulfillmentType.DELIVERY && calculation.isDeliveryBlocked) {
            if (_deliveryDistanceError.value == null) refreshDeliveryQuote()
            return
        }

        val orderNum = (100..999).random()
        val orderId = "ORD-$orderNum"
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val isGuest = _customerPhone.value.trim().isEmpty()

        val newOrder = Order(
            id = orderId,
            customerPhone = _customerPhone.value.trim(),
            isGuest = isGuest,
            fulfillment = _fulfillmentType.value,
            deliveryAddress = if (_fulfillmentType.value == FulfillmentType.DELIVERY) _deliveryAddress.value else "",
            gpsCoordinates = if (_fulfillmentType.value == FulfillmentType.DELIVERY) _gpsCoordinates.value else "",
            items = calculation.orderItems.map { oi ->
                val subJson = _addonOptions.value.selectedSubItemsJson
                val subs = try {
                    if (subJson.isNotBlank()) {
                        val o = org.json.JSONObject(subJson)
                        if (o.has(oi.itemId)) o.getJSONArray(oi.itemId).let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        }.joinToString(",") else ""
                    } else ""
                } catch (e: Exception) { "" }
                oi.copy(selectedSubItems = subs)
            },
            options = _addonOptions.value,
            comment = _addonOptions.value.comment,
            subtotal = calculation.subtotal,
            discount = calculation.discount,
            deliveryFee = calculation.deliveryFee,
            totalAmount = calculation.totalAmount,
            status = OrderStatus.PENDING,
            timestamp = timeStr,
            distanceKm = calculation.distanceKm,
            slipAttached = _isSlipAttached.value,
            slipImageUri = _slipImageUri.value
        )

        repository.addOrder(newOrder)
        locallySubmittedOrderIds.add(newOrder.id)
        clearCart()
        _isPaymentModalOpen.value = false
        _isTrackerModalOpen.value = true
        _slipImageUri.value = null
        _isSlipAttached.value = false

        // Play alert sound for new order
        SoundHelper.playNewOrderAlert(getApplication())
    }

    fun reorderLastOrder() {
        val phone = _customerPhone.value.trim()
        val user = users.value[phone]
        val lastOrder = user?.lastOrder ?: orders.value.firstOrNull { it.customerPhone == phone }
        if (lastOrder != null) {
            val newCart = mutableMapOf<String, Int>()
            lastOrder.items.forEach { orderItem ->
                newCart[orderItem.itemId] = orderItem.quantity
            }
            _cart.value = newCart
            _addonOptions.value = lastOrder.options
            _fulfillmentType.value = lastOrder.fulfillment
            _deliveryAddress.value = lastOrder.deliveryAddress
            _isAddonModalOpen.value = true
        }
    }

    fun openTrackerModal() {
        _isTrackerModalOpen.value = true
    }

    fun closeTrackerModal() {
        _isTrackerModalOpen.value = false
    }

    fun openOrderHistory() {
        _isOrderHistoryDialogOpen.value = true
    }

    fun closeOrderHistory() {
        _isOrderHistoryDialogOpen.value = false
    }

    fun viewOrderTracking(orderId: String) {
        repository.setCurrentActiveOrderId(orderId)
        _isOrderHistoryDialogOpen.value = false
        _isTrackerModalOpen.value = true
    }

    fun reorderSpecificOrder(order: Order) {
        val newCart = mutableMapOf<String, Int>()
        order.items.forEach { orderItem ->
            newCart[orderItem.itemId] = orderItem.quantity
        }
        _cart.value = newCart
        _addonOptions.value = order.options
        _fulfillmentType.value = order.fulfillment
        if (order.deliveryAddress.isNotEmpty()) {
            _deliveryAddress.value = order.deliveryAddress
        }
        _isOrderHistoryDialogOpen.value = false
        _isAddonModalOpen.value = true
    }

    // Admin Controls
    fun setAdminTab(tab: Int) {
        _adminSelectedTab.value = tab
        // Stop new-order sound when admin opens the orders tab
        if (tab == 0) SoundHelper.stopAlert()
    }

    fun stopNewOrderSound() {
        SoundHelper.stopAlert()
    }

    fun setAdminOrderFilter(filter: OrderStatus?) {
        _adminOrderFilter.value = filter
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        repository.updateOrderStatus(orderId, newStatus)
        // Stop new-order alert sound once admin acts on an order
        SoundHelper.stopAlert()
        if (newStatus == OrderStatus.READY) {
            SoundHelper.playOrderReadyChime(getApplication())
        }
    }

    fun removeOrderItem(orderId: String, itemId: String) {
        repository.removeOrderItem(orderId, itemId)
    }

    fun retrySync() {
        firebaseSync.retry()
    }

    /**
     * Creates a completed POS (walk-in) order: deducts stock, awards member
     * stamps and syncs to Firebase. Returns the new order id, or null when the
     * cart is empty / items are no longer available.
     */
    fun submitPosOrder(cart: Map<String, Int>, phone: String): String? {
        val items = cart.mapNotNull { (id, qty) ->
            if (qty <= 0) null
            else menuItems.value.find { it.id == id }?.let { m ->
                OrderItem(
                    itemId = m.id,
                    name = m.name,
                    price = m.price,
                    directCost = m.directCost,
                    quantity = qty
                )
            }
        }
        if (items.isEmpty()) return null

        val subtotal = items.sumOf { it.price * it.quantity }
        val discount = if (shopInfo.value.promoDiscountEnabled && subtotal >= shopInfo.value.minOrderForDiscount) {
            shopInfo.value.discountAmount
        } else {
            0.0
        }
        val total = (subtotal - discount).coerceAtLeast(0.0)

        var orderId = "ORD-" + (100..999).random()
        var guard = 0
        while (orders.value.any { it.id == orderId } && guard < 50) {
            orderId = "ORD-" + (100..999).random()
            guard++
        }

        val order = Order(
            id = orderId,
            customerPhone = phone.trim(),
            isGuest = phone.isBlank(),
            fulfillment = FulfillmentType.PICKUP,
            items = items,
            comment = "POS (เงินสด)",
            subtotal = subtotal,
            discount = discount,
            deliveryFee = 0.0,
            totalAmount = total,
            status = OrderStatus.COMPLETED,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()),
            slipAttached = false,
            slipImageUri = null
        )
        repository.addOrder(order)
        return orderId
    }

    fun deleteOrder(orderId: String) {
        repository.deleteOrder(orderId)
        SoundHelper.stopAlert()
    }

    fun updateItemStatus(itemId: String, status: ItemStatus) {
        repository.updateItemStatus(itemId, status)
    }

    fun updateItemStock(itemId: String, stock: Int) {
        repository.updateItemStock(itemId, stock)
    }

    fun addOrUpdateMenuItem(item: MenuItem) {
        repository.addOrUpdateMenuItem(item)
    }

    fun deleteMenuItem(itemId: String) {
        repository.deleteMenuItem(itemId)
    }

    fun setItemUnlimitedStock(itemId: String, unlimited: Boolean) {
        repository.setItemUnlimitedStock(itemId, unlimited)
    }

    fun moveMenuItem(itemId: String, direction: Int) {
        repository.moveMenuItem(itemId, direction)
    }

    fun addCategory(name: String, emoji: String) {
        repository.addCategory(name, emoji)
    }

    fun renameCategory(id: String, name: String) {
        repository.renameCategory(id, name)
    }

    fun deleteCategory(id: String) {
        repository.deleteCategory(id)
    }

    val customPromos: StateFlow<List<CustomPromo>> = repository.customPromos

    fun addCustomPromo(promo: CustomPromo) {
        repository.addCustomPromo(promo)
    }

    fun toggleCustomPromo(id: String, enabled: Boolean) {
        repository.toggleCustomPromo(id, enabled)
    }

    fun deleteCustomPromo(id: String) {
        repository.deleteCustomPromo(id)
    }

    fun oneClickStockReset() {
        repository.oneClickStockReset()
        SoundHelper.playStampSuccessSound(getApplication())
    }

    fun registerMember(phone: String) {
        repository.addMemberStamp(phone, 0)
    }

    fun addMemberStamp(phone: String, count: Int = 1) {
        repository.addMemberStamp(phone, count)
        SoundHelper.playStampSuccessSound(getApplication())
    }

    fun redeemReward(phone: String): Boolean {
        val success = repository.redeemReward(phone)
        if (success) {
            SoundHelper.playStampSuccessSound(getApplication())
        }
        return success
    }

    fun addHiddenCost(name: String, cat: String, amount: Double, note: String) {
        repository.addHiddenCost(name, cat, amount, note)
    }

    fun updateShopInfo(info: ShopInfo) {
        repository.updateShopInfo(info)
    }

    fun openPinDialog() {
        _isPinDialogOpen.value = true
    }

    fun closePinDialog() {
        _isPinDialogOpen.value = false
    }

    fun verifyPin(entered: String): Boolean {
        if (entered == adminPin.value) {
            _isAdminMode.value = true
            repository.setAdminLoggedIn(true)
            // Seed existing orders so only genuinely new ones trigger the alert.
            knownOrderIds = orders.value.map { it.id }.toSet()
            registerAdminPushToken()
            _isPinDialogOpen.value = false
            return true
        }
        return false
    }

    /**
     * Registers this device as an admin notification target so new orders can be
     * pushed even when the app is closed (via the Cloud Function trigger).
     */
    fun registerAdminPushToken() {
        val app = getApplication<android.app.Application>()
        NotificationHelper.ensureChannel(app)
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNotBlank()) NewOrderMessagingService.registerToken(app, token)
            }
    }

    fun exitAdminMode() {
        _isAdminMode.value = false
        repository.setAdminLoggedIn(false)
        SoundHelper.stopAlert()
        OrderAlertService.stop(getApplication())
    }

    fun setAdminPin(newPin: String) {
        repository.setAdminPin(newPin)
    }

    fun openPrintingOrder(order: Order) {
        _printingOrder.value = order
    }

    fun closePrintingOrder() {
        _printingOrder.value = null
    }

    fun setPhoneInputDialogOpen(open: Boolean) {
        _isPhoneInputDialogOpen.value = open
    }

    // ---------- In-app self update ----------

    /** Checks the published version.json. Silent failures never disturb the shop. */
    fun checkForUpdate(silent: Boolean = true) {
        if (!silent) _updateStatus.value = "กำลังตรวจหาอัปเดต..."
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            AppUpdater.check(app)
                .onSuccess { info ->
                    _updateInfo.value = info
                    if (info != null) {
                        _updateStatus.value = "มีเวอร์ชันใหม่ v${info.versionName}"
                    } else {
                        _updateStatus.value =
                            "เป็นเวอร์ชันล่าสุดแล้ว (v${AppUpdater.installedVersionName(app)})"
                    }
                }
                .onFailure { e ->
                    _updateStatus.value =
                        if (silent) "" else "ตรวจสอบไม่สำเร็จ: ${e.message ?: "ไม่ทราบสาเหตุ"}"
                }
            updateChecked = true
        }
    }

    /** Called once when the admin screen opens. */
    fun checkForUpdateOnLaunch() {
        if (updateChecked) return
        updateChecked = true
        checkForUpdate(silent = true)
    }

    fun dismissUpdate() {
        _updateDismissed.value = true
    }

    /** Downloads the published APK and opens the system installer. */
    fun downloadAndInstallUpdate() {
        val info = _updateInfo.value ?: return
        val app = getApplication<android.app.Application>()
        viewModelScope.launch {
            _updateProgress.value = 0
            _updateStatus.value = "กำลังดาวน์โหลด v${info.versionName}..."
            try {
                val file = AppUpdater.download(app, info) { p -> _updateProgress.value = p }
                _updateProgress.value = 100
                _updateStatus.value = installMessage(AppUpdater.install(app, file, info.apkUrl))
            } catch (e: Exception) {
                _updateProgress.value = -1
                _updateStatus.value = "ดาวน์โหลดไม่สำเร็จ: ${e.message ?: "ไม่ทราบสาเหตุ"}"
            }
        }
    }

    private fun installMessage(result: com.example.util.InstallResult): String = when (result) {
        com.example.util.InstallResult.INSTALLER_OPENED ->
            "เปิดหน้าติดตั้งแล้ว กรุณากด \"ติดตั้ง\""
        com.example.util.InstallResult.NEED_PERMISSION ->
            "กรุณาอนุญาต \"ติดตั้งแอปจากแหล่งอื่น\" ให้แอปนี้ แล้วกด \"ติดตั้งอีกครั้ง\""
        com.example.util.InstallResult.BROWSER_FALLBACK ->
            "เปิดหน้าดาวน์โหลดในเบราว์เซอร์แล้ว กรุณาดาวน์โหลดและติดตั้ง"
    }

    /** Re-opens the installer with the already-downloaded APK (no re-download). */
    fun retryUpdateInstall() {
        val info = _updateInfo.value ?: return
        val app = getApplication<android.app.Application>()
        val cached = java.io.File(app.cacheDir, "updates/update.apk")
        if (cached.exists() && cached.length() > 1_000_000L) {
            _updateStatus.value = installMessage(AppUpdater.install(app, cached, info.apkUrl))
        } else {
            _updateProgress.value = -1
            downloadAndInstallUpdate()
        }
    }
}

data class CartCalculation(
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalItemsCount: Int = 0,
    val orderItems: List<OrderItem> = emptyList(),
    val distanceKm: Double? = null,
    val isDeliveryBlocked: Boolean = false,
    val maxDeliveryDistanceKm: Double = 20.0
)
