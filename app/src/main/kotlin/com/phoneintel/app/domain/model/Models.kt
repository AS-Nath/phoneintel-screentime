package com.phoneintel.app.domain.model

import kotlin.math.floor
import kotlin.math.sqrt

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
// Append this to the bottom of Models.kt

// ─── XP / Levelling ──────────────────────────────────────────────────────────

data class XpState(
    val totalXp: Int,
    val level: Int,
    val currentLevelXp: Int,   // XP earned within this level
    val nextLevelXp: Int,      // XP needed to reach next level
    val progressFraction: Float // 0..1 within current level
) {
    companion object {
        /**
         * Level formula: level = floor(sqrt(totalXp / 100))
         *
         * Level thresholds:
         *   Level 1 →    100 XP
         *   Level 2 →    400 XP
         *   Level 3 →    900 XP
         *   Level 4 →  1 600 XP
         *   Level 5 →  2 500 XP  ...
         *
         * XP needed for level N = N² × 100
         * XP needed for level N+1 = (N+1)² × 100
         */
        fun from(totalXp: Int): XpState {
            val safeXp = maxOf(0, totalXp)
            val level = floor(sqrt(safeXp / 100.0)).toInt()
            val currentLevelStartXp = level * level * 100
            val nextLevelStartXp = (level + 1) * (level + 1) * 100
            val currentLevelXp = safeXp - currentLevelStartXp
            val nextLevelXp = nextLevelStartXp - currentLevelStartXp
            val progress = (currentLevelXp / nextLevelXp.toFloat()).coerceIn(0f, 1f)
            return XpState(safeXp, level, currentLevelXp, nextLevelXp, progress)
        }
    }
}

// ─── Insights ─────────────────────────────────────────────────────────────────

enum class InsightType {
    NIGHT_HABIT,
    SINGLE_APP_SINK,
    COMPULSIVE_CHECKER,
    FRAGMENTATION_SPIKE,
    NOTIFICATION_DRIVER,
    IMPROVING
}

enum class InsightSeverity { ALERT, WARN, INFO }

data class InsightAction(val label: String, val route: String)

data class InsightCard(
    val type: InsightType,
    val headline: String,
    val body: String,
    val action: InsightAction? = null,
    val severity: InsightSeverity = InsightSeverity.INFO
)

object InsightEngine {

    fun analyse(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>,
        appUsage: List<com.phoneintel.app.data.db.entities.AppUsageEntity>,
        notifications: List<com.phoneintel.app.data.db.entities.NotificationEventEntity>
    ): List<InsightCard> {
        val cards = mutableListOf<InsightCard>()

        nightHabit(sessions)?.let { cards.add(it) }
        singleAppSink(appUsage)?.let { cards.add(it) }
        compulsiveChecker(sessions)?.let { cards.add(it) }
        fragmentationSpike(sessions)?.let { cards.add(it) }
        notificationDriver(sessions, notifications)?.let { cards.add(it) }
        improving(sessions)?.let { cards.add(it) }

        return cards.sortedByDescending { it.severity.ordinal }
    }

    private fun nightHabit(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>
    ): InsightCard? {
        val nightSessions = sessions.filter {
            val hour = java.util.Calendar.getInstance()
                .apply { timeInMillis = it.unlockTime }
                .get(java.util.Calendar.HOUR_OF_DAY)
            hour >= 22
        }
        if (nightSessions.size < 1) return null
        return InsightCard(
            type = InsightType.NIGHT_HABIT,
            headline = "Late-night phone use detected",
            body = "You've used your phone after 10pm on ${nightSessions.size} occasion(s) this week. " +
                    "Late screen time suppresses melatonin and delays sleep onset.",
            action = InsightAction("Start Sleep Focus", "focus"),
            severity = InsightSeverity.WARN
        )
    }

    private fun singleAppSink(
        appUsage: List<com.phoneintel.app.data.db.entities.AppUsageEntity>
    ): InsightCard? {
        val total = appUsage.sumOf { it.totalForegroundMs }.takeIf { it > 0 } ?: return null
        val top = appUsage.maxByOrNull { it.totalForegroundMs } ?: return null
        val share = top.totalForegroundMs / total.toFloat()
        val avgSessionMin = top.totalForegroundMs / 60_000L
        if (share < 0.45f || avgSessionMin < 5) return null
        return InsightCard(
            type = InsightType.SINGLE_APP_SINK,
            headline = "${top.appName} is absorbing your time",
            body = "${top.appName} accounts for ${(share * 100).toInt()}% of your screen time this week.",
            action = InsightAction("Block in Focus", "focus"),
            severity = InsightSeverity.WARN
        )
    }

    private fun compulsiveChecker(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>
    ): InsightCard? {
        if (sessions.size < 4) return null
        val avgMs = sessions.map { it.durationMs }.average()
        if (avgMs > 3 * 60_000) return null

        // Find a 2-hour window with 3+ sessions
        val sorted = sessions.sortedBy { it.unlockTime }
        var windowCount = 0
        var windowStart = 0L
        for (s in sorted) {
            if (windowStart == 0L || s.unlockTime - windowStart > 2 * 60 * 60_000L) {
                windowStart = s.unlockTime
                windowCount = 1
            } else {
                windowCount++
            }
        }
        if (windowCount < 3) return null

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = windowStart }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        return InsightCard(
            type = InsightType.COMPULSIVE_CHECKER,
            headline = "Compulsive checking pattern detected",
            body = "You had $windowCount+ unlocks between ${hour}:00–${hour + 2}:00 " +
                    "with an average session under 3 minutes. This often signals habit-loop checking.",
            action = InsightAction("Block distractions", "focus"),
            severity = InsightSeverity.ALERT
        )
    }

    private fun fragmentationSpike(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>
    ): InsightCard? {
        if (sessions.size < 4) return null
        val half = sessions.size / 2
        val older = sessions.take(half)
        val recent = sessions.drop(half)
        val olderFrag = older.count { it.durationMs < 3 * 60_000L } / older.size.toFloat()
        val recentFrag = recent.count { it.durationMs < 3 * 60_000L } / recent.size.toFloat()
        if (recentFrag - olderFrag < 0.20f) return null
        return InsightCard(
            type = InsightType.FRAGMENTATION_SPIKE,
            headline = "Your attention is more fragmented recently",
            body = "Short sessions (under 3 min) have increased from " +
                    "${(olderFrag * 100).toInt()}% to ${(recentFrag * 100).toInt()}% of your unlocks.",
            severity = InsightSeverity.WARN
        )
    }

    private fun notificationDriver(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>,
        notifications: List<com.phoneintel.app.data.db.entities.NotificationEventEntity>
    ): InsightCard? {
        if (sessions.size < 2 || notifications.size < 2) return null
        val topNotifier = notifications.groupBy { it.packageName }
            .maxByOrNull { it.value.size } ?: return null
        val appName = topNotifier.value.first().appName
        val count = topNotifier.value.size
        if (count < 5) return null
        return InsightCard(
            type = InsightType.NOTIFICATION_DRIVER,
            headline = "Notifications are pulling you in",
            body = "$appName sent $count notifications this week — the most of any app. " +
                    "High notification days correlate with more frequent unlocks.",
            action = InsightAction("View notifications", "notifications"),
            severity = InsightSeverity.WARN
        )
    }

    private fun improving(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>
    ): InsightCard? {
        if (sessions.size < 3) return null
        val half = sessions.size / 2
        val olderAvg = sessions.take(half).map { it.durationMs }.average()
        val recentAvg = sessions.drop(half).map { it.durationMs }.average()
        if (olderAvg == 0.0 || recentAvg / olderAvg > 0.85) return null
        return InsightCard(
            type = InsightType.IMPROVING,
            headline = "Your sessions are getting shorter",
            body = "Average session length has dropped by ${((1 - recentAvg / olderAvg) * 100).toInt()}% " +
                    "compared to earlier this week. You're building better habits.",
            severity = InsightSeverity.INFO
        )
    }
}