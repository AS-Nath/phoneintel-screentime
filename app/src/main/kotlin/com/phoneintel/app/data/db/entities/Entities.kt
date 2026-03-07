package com.phoneintel.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ─── App Usage ────────────────────────────────────────────────────────────────

@Entity(
    tableName = "app_usage",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val date: Long,                  // epoch millis, start-of-day
    val totalForegroundMs: Long,
    val launchCount: Int,
    val lastUsed: Long
)

// ─── Notification Events ──────────────────────────────────────────────────────

@Entity(tableName = "notification_events")
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val title: String?,              // stored hashed or blank for privacy
    val category: String?,
    val isOngoing: Boolean,
    val isDismissed: Boolean = false,
    val dismissedAt: Long? = null
)

// ─── Network Usage ────────────────────────────────────────────────────────────

@Entity(
    tableName = "network_usage",
    indices = [Index(value = ["packageName", "date", "connectionType"], unique = true)]
)
data class NetworkUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val date: Long,
    val connectionType: String,      // "WIFI" | "MOBILE"
    val bytesReceived: Long,
    val bytesSent: Long
)

// ─── Bluetooth Events ─────────────────────────────────────────────────────────

@Entity(tableName = "bluetooth_events")
data class BluetoothEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceName: String,
    val deviceAddress: String,       // MAC — store hashed if needed
    val deviceClass: String,
    val eventType: String,           // "CONNECTED" | "DISCONNECTED" | "SCAN_RESULT"
    val timestamp: Long,
    val connectedDurationMs: Long = 0
)

// ─── Battery Snapshots ────────────────────────────────────────────────────────

@Entity(tableName = "battery_snapshots")
data class BatterySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: Int,                  // 0-100
    val isCharging: Boolean,
    val chargeType: String?,         // "USB" | "AC" | "WIRELESS" | null
    val temperature: Float,          // Celsius
    val voltage: Int,                // mV
    val health: String               // "GOOD" | "OVERHEAT" | etc.
)

// ─── Unlock Sessions ──────────────────────────────────────────────────────────

@Entity(tableName = "unlock_sessions")
data class UnlockSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unlockTime: Long,      // epoch ms when ACTION_USER_PRESENT fired
    val lockTime: Long = 0L,   // epoch ms when ACTION_SCREEN_OFF fired; 0 = still active
    val durationMs: Long = 0L  // 0 while active
)

// ─── Daily Summary (pre-aggregated for performance) ───────────────────────────

@Entity(
    tableName = "daily_summary",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val totalScreenTimeMs: Long,
    val totalUnlocks: Int,
    val totalNotifications: Int,
    val totalAppsUsed: Int,
    val totalWifiBytes: Long,
    val totalMobileBytes: Long,
    val avgBatteryLevel: Float,
    val chargingCycles: Int,
    val bluetoothConnections: Int,
    val topApp: String?
)
