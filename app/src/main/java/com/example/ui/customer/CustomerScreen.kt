package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerConsumptionSummary
import com.example.data.model.ItemStatus
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.ShopInfo
import com.example.ui.MainViewModel
import com.example.ui.components.ShopLogoView
import com.example.ui.components.StampIconView
import com.example.ui.theme.ChiliRed
import com.example.ui.theme.CucumberGreen
import com.example.ui.theme.GoldenCrisp
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: MainViewModel) {
    val shopInfo by viewModel.shopInfo.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val cartSummary by viewModel.cartSummary.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val users by viewModel.users.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val customerOrders by viewModel.customerOrders.collectAsState()
    val customerConsumption by viewModel.customerConsumption.collectAsState()
    val activeOrderId by viewModel.currentActiveOrderId.collectAsState()
    val isPhoneInputDialogOpen by viewModel.isPhoneInputDialogOpen.collectAsState()

    val currentUser = users[customerPhone.trim()]
    val activeOrder = orders.find { it.id == activeOrderId && it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }

    val filteredItems = remember(menuItems, selectedCategory) {
        val base = if (selectedCategory == "all") {
            menuItems.filter { it.status != ItemStatus.HIDDEN }
        } else {
            menuItems.filter { it.categoryId == selectedCategory && it.status != ItemStatus.HIDDEN }
        }
        base.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }

    // Category tabs must match the shop's real menu categories (same order as admin/web)
    val visibleCategories = remember(categories, menuItems) {
        val usedCategoryIds = menuItems
            .filter { it.status != ItemStatus.HIDDEN }
            .map { it.categoryId }
            .toSet()
        categories
            .sortedBy { it.sort }
            .filter { it.id == "all" || it.id in usedCategoryIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShopLogoView(
                            shopInfo = shopInfo,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("top_app_bar_logo")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = shopInfo.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CucumberGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (shopInfo.isOpen) "● เปิดบริการ" else "● ปิดบริการ",
                                        color = if (shopInfo.isOpen) CucumberGreen else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = shopInfo.address,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.openPinDialog() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("admin_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "เข้าสู่ระบบร้านค้า",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "จัดการร้าน", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Floating Cart summary bar
            AnimatedVisibility(
                visible = cartSummary.totalItemsCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = OrangePrimary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${cartSummary.totalItemsCount}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ยอดรวม ฿${String.format(Locale.US, "%.0f", cartSummary.totalAmount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = OrangePrimary
                                )
                            }
                            if (cartSummary.discount > 0) {
                                Text(
                                    text = "ประหยัด ฿${String.format(Locale.US, "%.0f", cartSummary.discount)} จากโปรโมชั่น!",
                                    fontSize = 11.sp,
                                    color = CucumberGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.openAddonModal() },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("checkout_button")
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "สั่งซื้อเลย", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = if (cartSummary.totalItemsCount > 0) 100.dp else 24.dp)
        ) {
            // 1. Member Stamps & Loyalty Banner & Order History Section (hidden for guests)
            if (customerPhone.isNotBlank()) {
                item {
                    MemberLoyaltyCard(
                        shopInfo = shopInfo,
                        customerPhone = customerPhone,
                        memberUser = currentUser,
                        customerOrders = customerOrders,
                        consumption = customerConsumption,
                        onPhoneChangeClick = { viewModel.setPhoneInputDialogOpen(true) },
                        onReorderClick = { viewModel.reorderLastOrder() },
                        onOpenOrderHistoryClick = { viewModel.openOrderHistory() },
                        onReorderSpecificOrder = { viewModel.reorderSpecificOrder(it) },
                        onTrackOrder = { viewModel.viewOrderTracking(it) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { viewModel.setPhoneInputDialogOpen(true) }
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "เข้าสู่ระบบ/สมัครสมาชิกเพื่อสะสมแต้ม",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OrangePrimary
                        )
                    }
                }
            }

            // 2. Active Order Tracker Alert (if order is active)
            if (activeOrder != null) {
                item {
                    ActiveOrderBanner(
                        order = activeOrder,
                        onClick = { viewModel.openTrackerModal() }
                    )
                }
            }

            // 3. Shop Promotion Highlights
            if (shopInfo.showPromoBanner) {
                item {
                    PromotionTicker(shopInfo = shopInfo)
                }
            }

            // 4. Horizontal Categories Scroll
            item {
                CategoryHorizontalBar(
                    categories = visibleCategories,
                    selectedCategoryId = selectedCategory,
                    onSelect = { viewModel.selectCategory(it) }
                )
            }

            // 5. Menu Items List
            items(filteredItems, key = { it.id }) { item ->
                val quantityInCart = cart[item.id] ?: 0
                MenuItemCard(
                    item = item,
                    quantity = quantityInCart,
                    onAdd = { viewModel.addToCart(item) },
                    onRemove = { viewModel.removeFromCart(item) }
                )
            }

            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ไม่พบรายการอาหารในหมวดนี้",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Phone number input dialog
    if (isPhoneInputDialogOpen) {
        CustomerPhoneDialog(
            currentPhone = customerPhone,
            currentName = (currentUser?.name ?: ""),
            onDismiss = { viewModel.setPhoneInputDialogOpen(false) },
            onConfirm = { phone, name ->
                viewModel.setCustomerPhone(phone, name)
                viewModel.setPhoneInputDialogOpen(false)
            }
        )
    }
}

@Composable
fun MemberLoyaltyCard(
    shopInfo: ShopInfo,
    customerPhone: String,
    memberUser: com.example.data.model.MemberUser?,
    customerOrders: List<Order>,
    consumption: com.example.data.model.CustomerConsumptionSummary,
    onPhoneChangeClick: () -> Unit,
    onReorderClick: () -> Unit,
    onOpenOrderHistoryClick: () -> Unit,
    onReorderSpecificOrder: (Order) -> Unit,
    onTrackOrder: (String) -> Unit
) {
    val stamps = memberUser?.stamps ?: 0
    val maxStamps = 10
    val isMember = customerPhone.isNotBlank()
    var isHistoryExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("user_profile_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User Profile Header with Shop/Member Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShopLogoView(
                        shopInfo = shopInfo,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("member_card_logo")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isMember) "สมาชิก: $customerPhone" else "สั่งแบบทั่วไป (Guest)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (isMember && consumption.totalOrdersCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = GoldenCrisp.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "VIP ⭐",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isMember) "สะสมแต้มแลกฟรี + ดูประวัติการบริโภค" else "ใส่เบอร์เพื่อสะสมแต้มและบันทึกประวัติสั่งซื้อ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onPhoneChangeClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isMember) "เปลี่ยนเบอร์" else "ใส่เบอร์โทร",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Previous Consumption Stats Row
            Surface(
                color = Color(0xFFFFF9F0),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE0B2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "ยอดบริโภคสะสม", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "฿${String.format(Locale.US, "%.0f", consumption.totalSpent)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = OrangePrimary
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFFFCC80)))
                    Column {
                        Text(text = "สั่งทั้งหมด", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${consumption.totalOrdersCount} ครั้ง",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFFFCC80)))
                    Column {
                        Text(text = "ทานไปแล้ว", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${consumption.totalUnitsConsumed} ไม้",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isMember) {
                Spacer(modifier = Modifier.height(12.dp))
                // Stamps Row (10 circles)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..maxStamps) {
                        val isStamped = i <= stamps
                        StampIconView(
                            shopInfo = shopInfo,
                            isStamped = isStamped,
                            stepNumber = i,
                            size = 28.dp,
                            modifier = Modifier.testTag("stamp_circle_$i")
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "สะสมแล้ว $stamps/$maxStamps แต้ม (ครบ 10 ฟรี 1 ชุด)",
                        fontSize = 11.sp,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Order History & 1-Click Reorder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenOrderHistoryClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary.copy(alpha = 0.12f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("order_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ประวัติการสั่งซื้อ (${customerOrders.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }

                if (memberUser?.lastOrder != null || customerOrders.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onReorderClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("reorder_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "สั่งซ้ำล่าสุด", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Inline Order History Preview Section
            if (customerOrders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHistoryExpanded = !isHistoryExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ประวัติการสั่งซื้อล่าสุด (${customerOrders.size} รายการ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHistoryExpanded) "ย่อลง" else "ดูตัวอย่าง",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (isHistoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isHistoryExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        customerOrders.take(2).forEach { order ->
                            PastOrderSnippetCard(
                                order = order,
                                onReorder = { onReorderSpecificOrder(order) },
                                onDetails = onOpenOrderHistoryClick
                            )
                        }

                        if (customerOrders.size > 2) {
                            TextButton(
                                onClick = onOpenOrderHistoryClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("view_all_history_button"),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ดูประวัติการสั่งซื้อและยอดบริโภคทั้งหมด (${customerOrders.size} รายการ) >",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PastOrderSnippetCard(
    order: Order,
    onReorder: () -> Unit,
    onDetails: () -> Unit
) {
    Surface(
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEBEBEB)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetails)
            .testTag("snippet_order_${order.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.timestamp.take(16).ifEmpty { "เร็วๆ นี้" },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = order.items.joinToString(", ") { "${it.name} x${it.quantity}" },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "ยอดรวม: ", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = "฿${String.format(Locale.US, "%.0f", order.totalAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = OrangePrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onReorder,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(text = "สั่งซ้ำ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onDetails,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(text = "รายละเอียด", fontSize = 10.sp, color = OrangePrimary)
                    }
                }
            }
        }
    }
}


@Composable
fun ActiveOrderBanner(
    order: com.example.data.model.Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("active_order_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldenCrisp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = GoldenCrisp,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (order.status) {
                                OrderStatus.PENDING -> "⏳"
                                OrderStatus.COOKING -> "🍳"
                                OrderStatus.READY -> "🎉"
                                else -> "🍢"
                            },
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "สถานะออเดอร์ #${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = order.status.title,
                        fontSize = 12.sp,
                        color = ChiliRed,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Surface(
                color = OrangePrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "ติดตามสด >",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PromotionTicker(shopInfo: com.example.data.model.ShopInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🎁", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = buildString {
                    val parts = mutableListOf<String>()
                    if (shopInfo.promoDiscountEnabled) parts.add(
                        "ซื้อครบ ${String.format(Locale.US, "%.0f", shopInfo.minOrderForDiscount)}.- ลด ${String.format(Locale.US, "%.0f", shopInfo.discountAmount)}.-"
                    )
                    if (shopInfo.promoFreeDeliveryEnabled) parts.add(
                        "ส่งฟรีเมื่อซื้อครบ ${String.format(Locale.US, "%.0f", shopInfo.minOrderForFreeDelivery)}.-"
                    )
                    append(if (parts.isEmpty()) "ยินดีต้อนรับ" else "โปรสุดคุ้ม: " + parts.joinToString(" | "))
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = OrangePrimary
            )
        }
    }
}

@Composable
fun CategoryHorizontalBar(
    categories: List<com.example.data.model.Category>,
    selectedCategoryId: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = cat.id == selectedCategoryId
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(cat.id) },
                label = {
                    Text(
                        text = "${cat.iconEmoji} ${cat.name}",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) OrangePrimary else Color(0xFFE0E0E0)
                )
            )
        }
    }
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val isOutOfStock = item.status == ItemStatus.OUT_OF_STOCK ||
        (!item.isUnlimitedStock && item.stockQty <= 0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("menu_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji / Item visual box
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF8E7)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = item.emoji, fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isBestseller) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = ChiliRed,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ขายดี 🔥",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "฿${String.format(Locale.US, "%.0f", item.price)} / ${item.saleUnit}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = OrangePrimary
                    )

                    if (isOutOfStock) {
                        Text(
                            text = "สินค้าหมด ❌",
                            fontSize = 11.sp,
                            color = ChiliRed,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (!item.isUnlimitedStock) {
                        Text(
                            text = "คงเหลือ ${item.stockQty} ${item.saleUnit}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    // Unlimited stock: don't show remaining count to customers.
                }
            }

            // Quantity stepper
            if (isOutOfStock) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEEEEEE),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "หมด",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    if (quantity > 0) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF0F0F0), CircleShape)
                                .testTag("btn_remove_${item.id}")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "ลด", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$quantity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(32.dp)
                            .background(OrangePrimary, CircleShape)
                            .testTag("btn_add_${item.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "เพิ่ม", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerPhoneDialog(
    currentPhone: String,
    currentName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var phoneInput by remember { mutableStateOf(currentPhone) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "📱 เบอร์โทรศัพท์สำหรับสะสมแต้ม",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ใส่เบอร์โทรศัพท์เพื่อสะสมแต้ม ดูประวัติสั่งซื้อ และกดสั่งซ้ำเมนูเดิมได้ในคลิกเดียว (หรือเว้นว่างเพื่อสั่งแบบทั่วไป)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("ชื่อ (จำเป็น)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("เบอร์โทรศัพท์ (เช่น 0812345678)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_input_field")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onConfirm("", "") }) {
                        Text("สั่งแบบทั่วไป (ไม่ใส่เบอร์)", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(phoneInput.trim(), nameInput.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("บันทึก")
                    }
                }
            }
        }
    }
}
