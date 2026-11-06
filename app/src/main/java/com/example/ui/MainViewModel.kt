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
import com.example.data.repository.FirebaseSyncManager
import com.example.data.repository.MeatballRepository
import com.example.util.SoundHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MeatballRepository(application)
    val firebaseSync = FirebaseSyncManager(application, repository).apply { start() }

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

    private val _adminSelectedTab = MutableStateFlow(1)
    val adminSelectedTab: StateFlow<Int> = _adminSelectedTab.asStateFlow()

    private val _adminOrderFilter = MutableStateFlow<OrderStatus?>(null)
    val adminOrderFilter: StateFlow<OrderStatus?> = _adminOrderFilter.asStateFlow()

    // Customer State
    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart: StateFlow<Map<String, Int>> = _cart.asStateFlow()

    private val _customerPhone = MutableStateFlow("0812345678")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _isPhoneInputDialogOpen = MutableStateFlow(false)
    val isPhoneInputDialogOpen: StateFlow<Boolean> = _isPhoneInputDialogOpen.asStateFlow()

    private val _fulfillmentType = MutableStateFlow(FulfillmentType.DELIVERY)
    val fulfillmentType: StateFlow<FulfillmentType> = _fulfillmentType.asStateFlow()

    private val _deliveryAddress = MutableStateFlow("บ้านเลขที่ 12/3 ซอยมิตรภาพ 14")
    val deliveryAddress: StateFlow<String> = _deliveryAddress.asStateFlow()

    private val _gpsCoordinates = MutableStateFlow("")
    val gpsCoordinates: StateFlow<String> = _gpsCoordinates.asStateFlow()

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
        _fulfillmentType
    ) { cartMap: Map<String, Int>, items: List<MenuItem>, shop: ShopInfo, fulfillment: FulfillmentType ->
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

        // Auto Promotions: Buy >= 50 -> -5 baht
        val discount = if (subtotal >= shop.minOrderForDiscount) shop.discountAmount else 0.0

        // Free delivery if >= 120, else shop deliveryFee (if delivery)
        val deliveryFee = if (fulfillment == FulfillmentType.DELIVERY) {
            if (subtotal >= shop.minOrderForFreeDelivery) 0.0 else shop.deliveryFee
        } else 0.0

        val total = (subtotal - discount + deliveryFee).coerceAtLeast(0.0)

        CartCalculation(
            subtotal = subtotal,
            discount = discount,
            deliveryFee = deliveryFee,
            totalAmount = total,
            totalItemsCount = totalItems,
            orderItems = orderItems
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
        if (item.status == ItemStatus.OUT_OF_STOCK || item.stockQty <= 0) return
        val current = _cart.value.toMutableMap()
        val currentQty = current[item.id] ?: 0
        if (currentQty < item.stockQty) {
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

    fun setCustomerPhone(phone: String) {
        _customerPhone.value = phone
        repository.setCustomerPhone(phone)
    }

    fun setFulfillment(type: FulfillmentType) {
        _fulfillmentType.value = type
    }

    fun setDeliveryAddress(addr: String) {
        _deliveryAddress.value = addr
    }

    fun setGpsCoordinates(coordinates: String) {
        _gpsCoordinates.value = coordinates
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
            items = calculation.orderItems,
            options = _addonOptions.value,
            comment = _addonOptions.value.comment,
            subtotal = calculation.subtotal,
            discount = calculation.discount,
            deliveryFee = calculation.deliveryFee,
            totalAmount = calculation.totalAmount,
            status = OrderStatus.PENDING,
            timestamp = timeStr,
            slipAttached = _isSlipAttached.value,
            slipImageUri = _slipImageUri.value
        )

        repository.addOrder(newOrder)
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
    }

    fun setAdminOrderFilter(filter: OrderStatus?) {
        _adminOrderFilter.value = filter
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        repository.updateOrderStatus(orderId, newStatus)
        if (newStatus == OrderStatus.READY) {
            SoundHelper.playOrderReadyChime(getApplication())
        }
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

    fun moveMenuItem(itemId: String, direction: Int) {
        repository.moveMenuItem(itemId, direction)
    }

    fun oneClickStockReset() {
        repository.oneClickStockReset()
        SoundHelper.playStampSuccessSound(getApplication())
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
            _isPinDialogOpen.value = false
            return true
        }
        return false
    }

    fun exitAdminMode() {
        _isAdminMode.value = false
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
}

data class CartCalculation(
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalItemsCount: Int = 0,
    val orderItems: List<OrderItem> = emptyList()
)
