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
import com.example.ui.customer.AddonsDialog
import com.example.ui.customer.CustomerScreen
import com.example.ui.customer.OrderHistoryDialog
import com.example.ui.customer.OrderTrackerDialog
import com.example.ui.customer.PromptPayDialog
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
                    if (isAdminMode) viewModel.registerAdminPushToken()
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = isAdminMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "MainScreenTransition"
                    ) { adminActive ->
                        if (adminActive) {
                            AdminScreen(viewModel = viewModel)
                        } else {
                            CustomerScreen(viewModel = viewModel)
                        }
                    }

                    // Global Modals
                    AddonsDialog(viewModel = viewModel)
                    PromptPayDialog(viewModel = viewModel)
                    OrderTrackerDialog(viewModel = viewModel)
                    OrderHistoryDialog(viewModel = viewModel)
                    AdminPinDialog(viewModel = viewModel)
                    ThermalSlipDialog(viewModel = viewModel)
                }
            }
        }
    }
}
