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
import android.util.Log
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
import com.phoneintel.app.data.repository.XpRepository
import com.phoneintel.app.domain.model.PhoneHealthScore
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
    @Inject lateinit var xpRepository: XpRepository

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

    // ─── Screen receiver ──────────────────────────────────────────────────────
    // ACTION_USER_PRESENT is unreliable on Samsung One UI with biometric unlock
    // (the broadcast is never sent after fingerprint/face auth on modern devices).
    // We use SCREEN_ON as the unlock signal instead — it fires reliably on all
    // devices and OEMs regardless of lock screen type.
    // SCREEN_OFF reliably marks the end of every session.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "screenReceiver.onReceive: action=${intent.action}")
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    val now = System.currentTimeMillis()
                    unlockTime = now
                    Log.d(TAG, "SCREEN_ON — starting session at $now")
                    scope.launch {
                        try {
                            currentSessionId = unlockSessionRepository.startSession(now)
                            Log.d(TAG, "Session started successfully, id=$currentSessionId")
                        } catch (e: Exception) {
                            Log.e(TAG, "ERROR starting session", e)
                        }
                    }
                    showMindfulUnlockNotification()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    val sid = currentSessionId
                    val start = unlockTime
                    Log.d(TAG, "ACTION_SCREEN_OFF fired — sid=$sid, start=$start")
                    if (sid > 0L && start > 0L) {
                        val now = System.currentTimeMillis()
                        val duration = now - start
                        Log.d(TAG, "Ending session id=$sid, durationMs=$duration")
                        scope.launch {
                            try {
                                unlockSessionRepository.endSession(sid, now, duration)
                                Log.d(TAG, "Session ended successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "ERROR ending session", e)
                            }
                        }
                        currentSessionId = -1L
                        unlockTime = 0L
                    } else {
                        Log.w(TAG, "SCREEN_OFF skipped — no active session (sid=$sid, start=$start)")
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
        Log.d(TAG, "DataCollectionService onCreate — SDK=${Build.VERSION.SDK_INT}")
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, screenFilter)
        }
        Log.d(TAG, "screenReceiver registered for SCREEN_ON, SCREEN_OFF")

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
        Log.d(TAG, "DataCollectionService fully started")
    }

    private fun startPeriodicSync() {
        syncJob = scope.launch {
            while (isActive) {
                Log.d(TAG, "Periodic sync running")
                runCatching { appUsageRepository.syncUsageStats() }
                    .onFailure { Log.e(TAG, "syncUsageStats failed", it) }
                runCatching { networkRepository.syncNetworkStats() }
                    .onFailure { Log.e(TAG, "syncNetworkStats failed", it) }
                runCatching { tickXp() }
                    .onFailure { Log.e(TAG, "tickXp failed", it) }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    // ─── XP Tick ──────────────────────────────────────────────────────────────
    // Called every 15-minute sync. XpRepository guards against more than one
    // award per hour internally, so this is safe to call frequently.
    private suspend fun tickXp() {
        val startOfDay = com.phoneintel.app.util.DateUtil.startOfDay()
        val sessions = unlockSessionRepository.getCompletedToday()
        val totalScreenTimeMs = appUsageRepository.getAllSince(startOfDay)
            .sumOf { it.totalForegroundMs }

        // Night usage: unlocks between 23:00 and 06:00
        val cal = java.util.Calendar.getInstance()
        val nightMs = sessions.filter {
            cal.timeInMillis = it.unlockTime
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            hour >= 23 || hour < 6
        }.sumOf { it.durationMs }

        val longestSession = sessions.maxOfOrNull { it.durationMs } ?: 0L
        val unlockCount = sessions.size

        val score = PhoneHealthScore.compute(
            totalScreenTimeMs = totalScreenTimeMs,
            nightUsageMs = nightMs,
            unlockCount = unlockCount,
            longestSessionMs = longestSession,
            notificationCount = 0   // not used in scoring (removed earlier)
        ).score

        xpRepository.recordHealthTick(score)
    }

    // ─── Mindful Unlock Notification ──────────────────────────────────────────
    private fun showMindfulUnlockNotification() {
        scope.launch {
            val sessions = runCatching { unlockSessionRepository.getCompletedToday() }
                .onFailure { Log.e(TAG, "getCompletedToday failed", it) }
                .getOrDefault(emptyList())
            val unlockCountToday = sessions.size + 1

            Log.d(TAG, "Showing mindful unlock notification — unlock #$unlockCountToday today")

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called — flags=$flags, startId=$startId")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.w(TAG, "DataCollectionService onDestroy — service is being stopped!")
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
        const val TAG = "PhoneIntel"
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
            Log.d(DataCollectionService.TAG, "BootReceiver fired — starting DataCollectionService")
            context.startForegroundService(Intent(context, DataCollectionService::class.java))
        }
    }
}