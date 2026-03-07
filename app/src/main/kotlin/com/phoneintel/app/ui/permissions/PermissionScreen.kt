package com.phoneintel.app.ui.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phoneintel.app.ui.theme.*

fun hasUsageAccess(context: Context): Boolean = runCatching {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName)
    }
    mode == AppOpsManager.MODE_ALLOWED
}.getOrDefault(false)

fun hasNotificationAccess(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?: return false
    return flat.contains(context.packageName)
}

fun hasBluetoothPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    } else true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var usageGranted by remember { mutableStateOf(hasUsageAccess(context)) }
    var notifGranted by remember { mutableStateOf(hasNotificationAccess(context)) }
    var btGranted by remember { mutableStateOf(hasBluetoothPermission(context)) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        usageGranted = hasUsageAccess(context)
        notifGranted = hasNotificationAccess(context)
        btGranted = hasBluetoothPermission(context)
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> btGranted = granted }

    val allGranted = usageGranted && notifGranted && btGranted

    // Scaffold handles system bar insets (status bar, nav bar) correctly under
    // edge-to-edge mode set by MainActivity.enableEdgeToEdge().
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)          // system bar safe area
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()), // scrollable on small screens
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(
                Modifier.size(80.dp).clip(CircleShape).background(IndigoBase.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhoneAndroid, null, tint = IndigoBase, modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text("Set Up PhoneIntel", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "PhoneIntel needs a few special permissions to collect data. " +
                "All data stays on your device — nothing is uploaded anywhere.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            PermissionCard(
                Icons.Default.Smartphone, IndigoBase, "Usage Access",
                "Tracks which apps you use and for how long. Required for screen time and app usage stats.",
                usageGranted, "Open Settings"
            ) { settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }

            Spacer(Modifier.height(12.dp))

            PermissionCard(
                Icons.Default.Notifications, TealAccent, "Notification Access",
                "Counts notifications per app. PhoneIntel never reads notification content.",
                notifGranted, "Open Settings"
            ) { settingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

            Spacer(Modifier.height(12.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionCard(
                    Icons.Default.Bluetooth, ChartPurple, "Bluetooth Access",
                    "Tracks Bluetooth device connections and disconnections.",
                    btGranted, "Allow"
                ) { btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allGranted) IndigoBase else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (allGranted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    if (allGranted) "Get Started" else "Continue Without All Permissions",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (!allGranted) {
                Text(
                    "You can grant remaining permissions later from app Settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector, iconColor: Color, title: String, description: String,
    isGranted: Boolean, buttonLabel: String, onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        border = if (!isGranted) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ) else null
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, null, tint = ChartGreen, modifier = Modifier.size(24.dp))
            } else {
                TextButton(onClick = onGrant) {
                    Text(buttonLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
