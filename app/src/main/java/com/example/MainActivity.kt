package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.admin.AdminPinDialog
import com.example.ui.admin.AdminScreen
import com.example.ui.admin.ThermalSlipDialog
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Granted or not, the in-app sound and RTDB listener still alert the admin.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val isAdminMode by viewModel.isAdminMode.collectAsState()

                // Keep this device registered for push so alerts arrive even when the app is closed.
                LaunchedEffect(isAdminMode) {
                    if (isAdminMode) {
                        viewModel.registerAdminPushToken()
                        // Auto-start order alert service if enabled
                        val prefs = getSharedPreferences("order_alert_service", android.content.Context.MODE_PRIVATE)
                        if (prefs.getBoolean("enabled", false)) {
                            com.example.util.OrderAlertService.start(this@MainActivity)
                        }
                    } else {
                        viewModel.openPinDialog()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isAdminMode) {
                        AdminScreen(viewModel = viewModel)
                    }
                    // Global Modals (admin only)
                    AdminPinDialog(viewModel = viewModel)
                    ThermalSlipDialog(viewModel = viewModel)
                }
            }
        }
    }
}