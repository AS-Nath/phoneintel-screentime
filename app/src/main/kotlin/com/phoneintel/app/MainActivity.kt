package com.phoneintel.app

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.phoneintel.app.service.DataCollectionService
import com.phoneintel.app.ui.PhoneIntelNavGraph
import com.phoneintel.app.ui.permissions.PermissionSetupScreen
import com.phoneintel.app.ui.permissions.hasBluetoothPermission
import com.phoneintel.app.ui.permissions.hasNotificationAccess
import com.phoneintel.app.ui.permissions.hasUsageAccess
import com.phoneintel.app.ui.theme.PhoneIntelTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

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
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current

                    // Show the permission setup screen if any critical permission is missing.
                    // Once the user taps Continue, we remember the choice for this session.
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
                        PhoneIntelNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
