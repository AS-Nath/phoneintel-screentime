package com.phoneintel.app

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phoneintel.app.service.DataCollectionService
import com.phoneintel.app.ui.PhoneIntelNavGraph
import com.phoneintel.app.ui.Screen
import com.phoneintel.app.ui.dashboard.PhoneIntelBottomNav
import com.phoneintel.app.ui.permissions.PermissionSetupScreen
import com.phoneintel.app.ui.permissions.hasBluetoothPermission
import com.phoneintel.app.ui.permissions.hasNotificationAccess
import com.phoneintel.app.ui.permissions.hasUsageAccess
import com.phoneintel.app.ui.theme.BgBase
import com.phoneintel.app.ui.theme.PhoneIntelTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

// Screens that should NOT show the bottom nav
private val bottomNavRoutes = setOf(
    Screen.Dashboard.route,
    Screen.PhoneHealth.route,
    Screen.Focus.route,
    Screen.Notifications.route,
    Screen.Insights.route,
    Screen.Network.route,
    Screen.Bluetooth.route,
    Screen.Battery.route,
    Screen.Recap.route,
)

@HiltAndroidApp
class PhoneIntelApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startForegroundService(Intent(this, DataCollectionService::class.java))

        setContent {
            PhoneIntelTheme {
                val context = LocalContext.current
                val needsSetup = remember {
                    !hasUsageAccess(context) ||
                            !hasNotificationAccess(context) ||
                            !hasBluetoothPermission(context)
                }
                var setupDone by remember { mutableStateOf(!needsSetup) }

                if (!setupDone) {
                    PermissionSetupScreen(onComplete = { setupDone = true })
                } else {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route

                    Scaffold(
                        containerColor = BgBase,
                        bottomBar = {
                            if (currentRoute in bottomNavRoutes) {
                                PhoneIntelBottomNav(
                                    navController = navController,
                                    currentRoute = currentRoute ?: Screen.Dashboard.route
                                )
                            }
                        }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            PhoneIntelNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }
}