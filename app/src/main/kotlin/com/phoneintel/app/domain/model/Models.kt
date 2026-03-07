package com.phoneintel.app.domain.model

// ─── App Usage ────────────────────────────────────────────────────────────────

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val totalForegroundMs: Long,
    val launchCount: Int,
    val lastUsed: Long,
    val iconDrawable: android.graphics.drawable.Drawable? = null
) {
    val totalForegroundMinutes: Long get() = totalForegroundMs / 60_000
    val totalForegroundHours: Float get() = totalForegroundMs / 3_600_000f
}

data class DailyUsageSummary(
    val date: Long,
    val totalScreenTimeMs: Long,
    val topApps: List<AppUsageStat>
)

// ─── Notifications ────────────────────────────────────────────────────────────

data class NotificationStat(
    val packageName: String,
    val appName: String,
    val count: Int,
    val iconDrawable: android.graphics.drawable.Drawable? = null
)

data class NotificationEvent(
    val id: Long,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val category: String?,
    val isOngoing: Boolean
)

// ─── Network ──────────────────────────────────────────────────────────────────

enum class ConnectionType { WIFI, MOBILE, UNKNOWN }

data class AppNetworkUsage(
    val packageName: String,
    val appName: String,
    val bytesReceived: Long,
    val bytesSent: Long,
    val connectionType: ConnectionType,
    val iconDrawable: android.graphics.drawable.Drawable? = null
) {
    val totalBytes: Long get() = bytesReceived + bytesSent
}

// ─── Bluetooth ────────────────────────────────────────────────────────────────

enum class BluetoothEventType { CONNECTED, DISCONNECTED, SCAN_RESULT }

data class BluetoothDevice(
    val name: String,
    val address: String,
    val deviceClass: String,
    val lastSeen: Long,
    val totalConnectionMs: Long,
    val connectionCount: Int
)

data class BluetoothEvent(
    val id: Long,
    val deviceName: String,
    val deviceAddress: String,
    val eventType: BluetoothEventType,
    val timestamp: Long,
    val durationMs: Long
)

// ─── Battery ─────────────────────────────────────────────────────────────────

data class BatterySnapshot(
    val timestamp: Long,
    val level: Int,
    val isCharging: Boolean,
    val chargeType: String?,
    val temperatureCelsius: Float,
    val voltage: Int,
    val health: String
)

data class BatteryStats(
    val currentLevel: Int,
    val isCharging: Boolean,
    val chargeType: String?,
    val avgLevelToday: Float,
    val chargeCycles: Int,
    val snapshots: List<BatterySnapshot>
)

// ─── Dashboard ────────────────────────────────────────────────────────────────

data class DashboardSummary(
    val todayScreenTimeMs: Long,
    val todayNotifications: Int,
    val todayWifiBytes: Long,
    val todayMobileBytes: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val topAppToday: AppUsageStat?,
    val weeklyScreenTimeTrend: List<DailyUsageSummary>,
    val bluetoothConnectedDevices: Int
)

// ─── Unlock Sessions / Attention ─────────────────────────────────────────────

data class UnlockSession(
    val id: Long,
    val unlockTime: Long,
    val lockTime: Long,
    val durationMs: Long
)

data class AttentionStats(
    val sessionCount: Int,
    val avgSessionMs: Long,
    val longestSessionMs: Long,
    val shortSessionCount: Int,    // sessions under 3 min — compulsive checks
    val fragmentationIndex: Float  // 0..1; higher = more fractured attention
)

// ─── Phone Health ─────────────────────────────────────────────────────────────

data class PhoneHealthScore(
    val score: Int,             // 1–100
    val grade: String,          // "Excellent" / "Good" / "Fair" / "Poor"
    // component scores
    val screenTimeScore: Int,   // 0–30
    val nightUsageScore: Int,   // 0–20
    val unlockScore: Int,       // 0–20
    val sessionScore: Int,      // 0–15
    val notifScore: Int,        // 0–15
    // raw inputs shown in breakdown
    val totalScreenTimeMs: Long,
    val nightUsageMs: Long,
    val unlockCount: Int,
    val longestSessionMs: Long,
    val notificationCount: Int
) {
    companion object {
        fun compute(
            totalScreenTimeMs: Long,
            nightUsageMs: Long,
            unlockCount: Int,
            longestSessionMs: Long,
            notificationCount: Int
        ): PhoneHealthScore {
            val hours = totalScreenTimeMs / 3_600_000.0
            val st = when {
                hours <= 2.0  -> 30
                hours <= 4.0  -> 22
                hours <= 6.0  -> 12
                else          -> 0
            }
            val nightMin = nightUsageMs / 60_000
            val nu = when {
                nightMin == 0L   -> 20
                nightMin <= 10L  -> 16
                nightMin <= 30L  -> 10
                nightMin <= 60L  -> 4
                else             -> 0
            }
            val ul = when {
                unlockCount <= 20  -> 20
                unlockCount <= 50  -> 14
                unlockCount <= 80  -> 7
                else               -> 0
            }
            val longestMin = longestSessionMs / 60_000
            val ss = when {
                longestMin <= 15 -> 15
                longestMin <= 40 -> 10
                longestMin <= 90 -> 5
                else             -> 0
            }
            val ns = when {
                notificationCount <= 20  -> 15
                notificationCount <= 60  -> 10
                notificationCount <= 120 -> 5
                else                     -> 0
            }
            val total = (st + nu + ul + ss + ns).coerceIn(1, 100)
            val grade = when {
                total >= 80 -> "Excellent"
                total >= 60 -> "Good"
                total >= 40 -> "Fair"
                else        -> "Poor"
            }
            return PhoneHealthScore(total, grade, st, nu, ul, ss, ns,
                totalScreenTimeMs, nightUsageMs, unlockCount, longestSessionMs, notificationCount)
        }
    }
}

// ─── Focus Mode ───────────────────────────────────────────────────────────────

enum class FocusIntent(val label: String, val emoji: String) {
    WORK("Work", "💼"),
    STUDY("Study", "📚"),
    FAMILY("Family", "👨‍👩‍👧"),
    SLEEP("Sleep", "😴"),
    CUSTOM("Custom", "⚙️")
}

data class FocusState(
    val isActive: Boolean = false,
    val intent: FocusIntent = FocusIntent.WORK,
    val blockedPackages: Set<String> = emptySet(),
    val startedAt: Long = 0L
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val iconDrawable: android.graphics.drawable.Drawable? = null
)

// ─── Year Recap ───────────────────────────────────────────────────────────────

data class YearRecap(
    val year: Int,
    val totalScreenTimeMs: Long,
    val totalNotifications: Int,
    val totalWifiBytes: Long,
    val totalMobileBytes: Long,
    val topApps: List<AppUsageStat>,
    val topNotifier: NotificationStat?,
    val longestStreakDays: Int,
    val mostProductiveMonth: String?,
    val avgDailyScreenTimeMs: Long,
    val bluetoothDeviceCount: Int,
    val chargingCycles: Int
)
