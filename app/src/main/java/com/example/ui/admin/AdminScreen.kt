package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.adminSelectedTab.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()

    // Handle back button to exit admin mode
    BackHandler {
        viewModel.exitAdminMode()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.ShopLogoView(
                            shopInfo = shopInfo,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ระบบบริหารร้านค้า (Admin)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = shopInfo.name,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.exitAdminMode() },
                        modifier = Modifier.testTag("admin_back_to_customer_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "กลับหน้าร้านค้าลูกค้า"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setAdminTab(0) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "ออเดอร์") },
                    label = { Text("ออเดอร์", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        selectedTextColor = OrangePrimary,
                        indicatorColor = Color(0xFFFFF3E0)
                    ),
                    modifier = Modifier.testTag("nav_admin_orders")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setAdminTab(1) },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "POS แต้ม") },
                    label = { Text("POS แต้ม", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        selectedTextColor = OrangePrimary,
                        indicatorColor = Color(0xFFFFF3E0)
                    ),
                    modifier = Modifier.testTag("nav_admin_pos")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setAdminTab(2) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "สต็อก") },
                    label = { Text("สต็อก", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        selectedTextColor = OrangePrimary,
                        indicatorColor = Color(0xFFFFF3E0)
                    ),
                    modifier = Modifier.testTag("nav_admin_inventory")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setAdminTab(3) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "ต้นทุน/กำไร") },
                    label = { Text("ต้นทุน/กำไร", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        selectedTextColor = OrangePrimary,
                        indicatorColor = Color(0xFFFFF3E0)
                    ),
                    modifier = Modifier.testTag("nav_admin_analytics")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setAdminTab(4) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "ตั้งค่า") },
                    label = { Text("ตั้งค่า", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        selectedTextColor = OrangePrimary,
                        indicatorColor = Color(0xFFFFF3E0)
                    ),
                    modifier = Modifier.testTag("nav_admin_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> OrderManagementView(viewModel = viewModel)
                1 -> PosStampView(viewModel = viewModel)
                2 -> InventoryView(viewModel = viewModel)
                3 -> AnalyticsView(viewModel = viewModel)
                4 -> SettingsView(viewModel = viewModel)
            }
        }
    }
}
