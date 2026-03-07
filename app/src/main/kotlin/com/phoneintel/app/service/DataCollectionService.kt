package com.phoneintel.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.phoneintel.app.MainActivity
import com.phoneintel.app.R
import com.phoneintel.app.data.db.entities.BatterySnapshotEntity
import com.phoneintel.app.data.db.entities.BluetoothEventEntity
import com.phoneintel.app.data.repository.AppUsageRepository
import com.phoneintel.app.data.repository.BatteryRepository
import com.phoneintel.app.data.repository.BluetoothRepository
import com.phoneintel.app.data.repository.NetworkRepository
import com.phoneintel.app.data.repository.UnlockSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class DataCollectionService : Service() {

    @Inject lateinit var appUsageRepository: AppUsageRepository
    @Inject lateinit var networkRepository: NetworkRepository
    @Inject lateinit var batteryRepository: BatteryRepository
    @Inject lateinit var bluetoothRepository: BluetoothRepository
    @Inject lateinit var unlockSessionRepository: UnlockSessionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    // Session tracking state
    @Volatile private var currentSessionId: Long = -1L
    @Volatile private var unlockTime: Long = 0L

    // ─── Battery receiver ─────────────────────────────────────────────────────
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL
            val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val chargeType = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> null
            }
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            val levelPct = if (scale > 0) (level * 100 / scale) else level
            scope.launch {
                batteryRepository.insertSnapshot(BatterySnapshotEntity(
                    timestamp = System.currentTimeMillis(),
                    level = levelPct,
                    isCharging = isCharging,
                    chargeType = chargeType,
                    temperature = temperature,
                    voltage = voltage,
                    health = "GOOD"
                ))
            }
        }
    }

    // ─── Screen / unlock receiver ─────────────────────────────────────────────
    // ACTION_USER_PRESENT fires when the user dismisses the keyguard — this
    // is the real "intentional unlock" moment.
    // ACTION_SCREEN_OFF fires when the screen turns off — session end.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> {
                    val now = System.currentTimeMillis()
                    unlockTime = now
                    scope.launch {
                        currentSessionId = unlockSessionRepository.startSession(now)
                    }
                    showMindfulUnlockNotification()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    val sid = currentSessionId
                    val start = unlockTime
                    if (sid > 0L && start > 0L) {
                        val now = System.currentTimeMillis()
                        scope.launch {
                            unlockSessionRepository.endSession(sid, now, now - start)
                        }
                        currentSessionId = -1L
                        unlockTime = 0L
                    }
                }
            }
        }
    }

    // ─── Bluetooth receiver ───────────────────────────────────────────────────
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action != BluetoothDevice.ACTION_ACL_CONNECTED &&
                action != BluetoothDevice.ACTION_ACL_DISCONNECTED) return

            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
            device ?: return

            val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else true

            val deviceName = if (hasBluetoothPermission)
                runCatching { device.name }.getOrNull() ?: "Unknown Device"
            else "Unknown Device"

            val deviceClass = if (hasBluetoothPermission) {
                runCatching {
                    device.bluetoothClass?.let { btClass ->
                        when (btClass.majorDeviceClass) {
                            android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
                            android.bluetooth.BluetoothClass.Device.Major.COMPUTER -> "Computer"
                            android.bluetooth.BluetoothClass.Device.Major.PHONE -> "Phone"
                            android.bluetooth.BluetoothClass.Device.Major.HEALTH -> "Health"
                            android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
                            android.bluetooth.BluetoothClass.Device.Major.IMAGING -> "Imaging"
                            android.bluetooth.BluetoothClass.Device.Major.WEARABLE -> "Wearable"
                            android.bluetooth.BluetoothClass.Device.Major.TOY -> "Toy"
                            android.bluetooth.BluetoothClass.Device.Major.NETWORKING -> "Networking"
                            else -> "Other"
                        }
                    } ?: "Unknown"
                }.getOrDefault("Unknown")
            } else "Unknown"

            val isConnected = action == BluetoothDevice.ACTION_ACL_CONNECTED
            scope.launch {
                bluetoothRepository.insertEvent(BluetoothEventEntity(
                    deviceName = deviceName,
                    deviceAddress = if (hasBluetoothPermission)
                        runCatching { device.address }.getOrDefault("unknown").hashCode().toString()
                    else "unknown",
                    deviceClass = deviceClass,
                    eventType = if (isConnected) "CONNECTED" else "DISCONNECTED",
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        // ACTION_BATTERY_CHANGED is sticky — no flag needed; null context variant not required here
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Screen events must be registered dynamically (not in manifest).
        // API 34+ requires RECEIVER_NOT_EXPORTED or RECEIVER_EXPORTED for all dynamic receivers.
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, screenFilter)
        }

        // Bluetooth events come from other apps / system — must be EXPORTED
        val btFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, btFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, btFilter)
        }
        startPeriodicSync()
    }

    private fun startPeriodicSync() {
        syncJob = scope.launch {
            while (isActive) {
                runCatching { appUsageRepository.syncUsageStats() }
                runCatching { networkRepository.syncNetworkStats() }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    // ─── Mindful Unlock Notification ──────────────────────────────────────────
    // Shown at the exact moment of every intentional unlock — the highest-
    // leverage moment for behavior change that Digital Wellbeing completely ignores.
    private fun showMindfulUnlockNotification() {
        scope.launch {
            val sessions = runCatching { unlockSessionRepository.getCompletedToday() }.getOrDefault(emptyList())
            val unlockCountToday = sessions.size + 1  // +1 for the session just started

            val tapIntent = PendingIntent.getActivity(
                this@DataCollectionService, 0,
                Intent(this@DataCollectionService, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(this@DataCollectionService, CHANNEL_MINDFUL)
                .setSmallIcon(R.drawable.ic_stat_phoneintel)
                .setContentTitle("Unlock #$unlockCountToday today")
                .setContentText("What do you need right now?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setTimeoutAfter(8_000)
                .setContentIntent(tapIntent)
                .build()

            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_MINDFUL_ID, notif)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        runCatching { unregisterReceiver(batteryReceiver) }
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { unregisterReceiver(bluetoothReceiver) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(CHANNEL_BG, "PhoneIntel Background", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Collecting usage data locally" },
            NotificationChannel(CHANNEL_MINDFUL, "Mindful Unlock", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Shown at every unlock — a gentle moment of awareness" },
            NotificationChannel(CHANNEL_FOCUS, "Focus Mode", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Active focus session status" }
        ).forEach { nm.createNotificationChannel(it) }
    }

    private fun buildForegroundNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_BG)
            .setContentTitle("PhoneIntel")
            .setContentText("Collecting data locally")
            .setSmallIcon(R.drawable.ic_stat_phoneintel)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_MINDFUL_ID = 1002
        const val CHANNEL_BG = "phoneintel_bg"
        const val CHANNEL_MINDFUL = "phoneintel_mindful"
        const val CHANNEL_FOCUS = "phoneintel_focus"
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            context.startForegroundService(Intent(context, DataCollectionService::class.java))
        }
    }
}
