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

// ─── Insights ─────────────────────────────────────────────────────────────────

data class InsightCard(
    val type: InsightType,
    val headline: String,
    val body: String,
    val action: InsightAction? = null,
    val severity: InsightSeverity = InsightSeverity.INFO
)

enum class InsightType {
    NIGHT_HABIT,
    SINGLE_APP_SINK,
    COMPULSIVE_CHECKER,
    FRAGMENTATION_SPIKE,
    NOTIFICATION_DRIVER,
    IMPROVING
}

enum class InsightSeverity { INFO, WARN, ALERT }

data class InsightAction(
    val label: String,
    val route: String
)

/**
 * Analyses recent local data and produces a ranked list of personalised
 * InsightCards. All logic runs locally — no network required.
 *
 * Thresholds are intentionally low so real cards surface after ~1 hour of use.
 * Each analyser returns null if the pattern isn't present, so the user only
 * ever sees cards that genuinely apply to them.
 */
object InsightEngine {

    fun analyse(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>,
        appUsage: List<com.phoneintel.app.data.db.entities.AppUsageEntity>,
        notifications: List<com.phoneintel.app.data.db.entities.NotificationEventEntity>
    ): List<InsightCard> {
        return listOfNotNull(
            analyseNightHabit(sessions),
            analyseSingleAppSink(appUsage),
            analyseCompulsiveChecker(sessions),
            analyseFragmentationSpike(sessions),
            analyseNotificationDriver(sessions, notifications),
            analyseImproving(sessions, appUsage)
        ).sortedByDescending { it.severity.ordinal }
    }

    // ─── Night Habit ──────────────────────────────────────────────────────────
    // Fires when the user has had screen time after 10pm on 1+ nights.

    private fun analyseNightHabit(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>
    ): InsightCard? {
        val nightSessions = sessions.filter { isNightHour(it.unlockTime) && it.durationMs > 0 }
        if (nightSessions.isEmpty()) return null
        val nightsByDay = nightSessions.groupBy { dayKey(it.unlockTime) }
        if (nightsByDay.size < 1) return null
        val avgMs = nightSessions.sumOf { it.durationMs } / nightSessions.size
        val latestHour = nightSessions.maxOf { hourOf(it.unlockTime) }
        val avgMin = avgMs / 60_000
        return InsightCard(
            type = InsightType.NIGHT_HABIT,
            headline = "You're losing sleep to your screen",
            body = "You've used your phone after 10pm on ${nightsByDay.size} of the last 7 nights. " +
                    "Your latest unlock this week was at ${latestHour}:00, " +
                    "with an average session of ${avgMin}m at that hour. " +
                    "Try leaving your phone in another room from 10pm.",
            action = InsightAction("Start Sleep Focus", "focus"),
            severity = InsightSeverity.WARN
        )
    }

    // ─── Single App Sink ──────────────────────────────────────────────────────
    // One app >45% of total screen time AND average session >5 min.

    private fun analyseSingleAppSink(
        appUsage: List<com.phoneintel.app.data.db.entities.AppUsageEntity>
    ): InsightCard? {
        if (appUsage.isEmpty()) return null
        val totalMs = appUsage.sumOf { it.totalForegroundMs }.takeIf { it > 0 } ?: return null
        val byApp = appUsage.groupBy { it.packageName }
            .mapValues { (_, e) -> e.sumOf { it.totalForegroundMs } }
            .entries.sortedByDescending { it.value }
        val top = byApp.firstOrNull() ?: return null
        val share = top.value.toFloat() / totalMs
        if (share < 0.45f) return null
        val daysActive = appUsage.count { it.packageName == top.key }.coerceAtLeast(1)
        val avgSessionMin = (top.value / daysActive) / 60_000
        if (avgSessionMin < 5) return null
        val appName = appUsage.first { it.packageName == top.key }.appName
        val sharePct = (share * 100).toInt()
        return InsightCard(
            type = InsightType.SINGLE_APP_SINK,
            headline = "$appName is absorbing your time",
            body = "$appName accounts for $sharePct% of your total screen time this week, " +
                    "with an average session of ${avgSessionMin}m. " +
                    "You're not checking it compulsively — you're getting lost in it. " +
                    "Try blocking it during your most productive hours.",
            action = InsightAction("Block in Focus", "focus"),
            severity = InsightSeverity.WARN
        )
    }

    // ─── Compulsive Checker ───────────────────────────────────────────────────
    // 3+ unlocks in a 2-hour window averaging under 3 minutes each.

    private fun analyseCompulsiveChecker(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>
    ): InsightCard? {
        if (sessions.size < 4) return null
        val windowBuckets = mutableMapOf<Int, MutableList<com.phoneintel.app.data.db.entities.UnlockSessionEntity>>()
        sessions.forEach { s ->
            val windowStart = (hourOf(s.unlockTime) / 2) * 2
            windowBuckets.getOrPut(windowStart) { mutableListOf() }.add(s)
        }
        val worst = windowBuckets.entries
            .filter { it.value.size >= 3 }
            .maxByOrNull { entry ->
                val completed = entry.value.filter { it.durationMs > 0 }
                val avg = if (completed.isEmpty()) Long.MAX_VALUE
                else completed.sumOf { it.durationMs } / completed.size
                entry.value.size * (1.0 / (avg + 1))
            } ?: return null
        val completed = worst.value.filter { it.durationMs > 0 }
        if (completed.isEmpty()) return null
        val avgMin = completed.sumOf { it.durationMs } / completed.size / 60_000
        if (avgMin > 3) return null
        val start = worst.key
        val end = start + 2
        return InsightCard(
            type = InsightType.COMPULSIVE_CHECKER,
            headline = "Compulsive checking between ${start}:00–${end}:00",
            body = "You've unlocked your phone ${worst.value.size} times in the " +
                    "${start}:00–${end}:00 window this week, " +
                    "with an average session of just ${avgMin}m. " +
                    "This pattern often signals avoidance. " +
                    "What happens at ${start}:00 that makes you reach for your phone?",
            action = InsightAction("Block distractions", "focus"),
            severity = InsightSeverity.ALERT
        )
    }

    // ─── Fragmentation Spike ─────────────────────────────────────────────────
    // Compares recent fragmentation against the user's own earlier baseline.

    private fun analyseFragmentationSpike(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>
    ): InsightCard? {
        if (sessions.size < 4) return null
        val cutoff = System.currentTimeMillis() - 3 * 86_400_000L
        val baseline = sessions.filter { it.unlockTime < cutoff && it.durationMs > 0 }
        val recent   = sessions.filter { it.unlockTime >= cutoff && it.durationMs > 0 }
        if (baseline.isEmpty() || recent.isEmpty()) return null
        fun frag(list: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>) =
            list.count { it.durationMs < 3 * 60_000L }.toFloat() / list.size
        val baselineFrag = frag(baseline)
        val recentFrag   = frag(recent)
        if (recentFrag - baselineFrag < 0.20f) return null
        return InsightCard(
            type = InsightType.FRAGMENTATION_SPIKE,
            headline = "Your attention is more fragmented this week",
            body = "Your fragmentation has risen from ${(baselineFrag * 100).toInt()}% " +
                    "to ${(recentFrag * 100).toInt()}% in the last 3 days — " +
                    "more of your sessions are under 3 minutes. " +
                    "Something may be driving more compulsive checking. " +
                    "Stress, a new app, or a change in routine?",
            severity = InsightSeverity.ALERT
        )
    }

    // ─── Notification Driver ──────────────────────────────────────────────────
    // High notification days correlate with high unlock days.

    private fun analyseNotificationDriver(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>,
        notifications: List<com.phoneintel.app.data.db.entities.NotificationEventEntity>
    ): InsightCard? {
        if (sessions.isEmpty() || notifications.isEmpty()) return null
        val sessionsByDay  = sessions.groupBy { dayKey(it.unlockTime) }.mapValues { it.value.size }
        val notifsByDay    = notifications.groupBy { dayKey(it.timestamp) }.mapValues { it.value.size }
        val commonDays     = sessionsByDay.keys.intersect(notifsByDay.keys)
        if (commonDays.size < 2) return null
        val avgNotifs   = notifsByDay.values.average()
        val avgSessions = sessionsByDay.values.average()
        val highNotifDays = commonDays.filter { (notifsByDay[it] ?: 0) > avgNotifs }
        if (highNotifDays.isEmpty()) return null
        val avgSessionsOnHighDays = highNotifDays.mapNotNull { sessionsByDay[it] }.average()
        if (avgSessionsOnHighDays < avgSessions * 1.35) return null
        val topApp      = notifications.groupBy { it.packageName }.maxByOrNull { it.value.size }
        val topAppName  = topApp?.value?.firstOrNull()?.appName ?: "an app"
        val topAppCount = topApp?.value?.size ?: 0
        val upliftPct   = ((avgSessionsOnHighDays / avgSessions - 1) * 100).toInt()
        return InsightCard(
            type = InsightType.NOTIFICATION_DRIVER,
            headline = "Notifications are pulling you in",
            body = "On days with more notifications you unlock your phone ${upliftPct}% more often. " +
                    "$topAppName sent $topAppCount notifications this week — the most of any app. " +
                    "Reducing its alerts could meaningfully cut your unlock count.",
            action = InsightAction("View notifications", "notifications"),
            severity = InsightSeverity.WARN
        )
    }

    // ─── Improving ────────────────────────────────────────────────────────────
    // Screen time trending down 15%+ and fragmentation not worsening.

    private fun analyseImproving(
        sessions: List<com.phoneintel.app.data.db.entities.UnlockSessionEntity>,
        appUsage: List<com.phoneintel.app.data.db.entities.AppUsageEntity>
    ): InsightCard? {
        if (appUsage.isEmpty() || sessions.size < 3) return null
        val cutoff       = System.currentTimeMillis() - 3 * 86_400_000L
        val earlyPerDay  = appUsage.filter { it.date < cutoff }.sumOf { it.totalForegroundMs } / 4.0
        val recentPerDay = appUsage.filter { it.date >= cutoff }.sumOf { it.totalForegroundMs } / 3.0
        if (earlyPerDay == 0.0 || recentPerDay / earlyPerDay > 0.85) return null
        val earlySessions  = sessions.filter { it.unlockTime < cutoff && it.durationMs > 0 }
        val recentSessions = sessions.filter { it.unlockTime >= cutoff && it.durationMs > 0 }
        if (earlySessions.isNotEmpty() && recentSessions.isNotEmpty()) {
            val ef = earlySessions.count { it.durationMs < 180_000L }.toFloat() / earlySessions.size
            val rf = recentSessions.count { it.durationMs < 180_000L }.toFloat() / recentSessions.size
            if (rf > ef + 0.1f) return null
        }
        val pct = ((1.0 - recentPerDay / earlyPerDay) * 100).toInt()
        return InsightCard(
            type = InsightType.IMPROVING,
            headline = "You're using your phone less — keep going",
            body = "Your daily screen time is down $pct% compared to earlier this week. " +
                    "The gradual approach is working. " +
                    "No cold turkey, no rebound — just steady progress.",
            severity = InsightSeverity.INFO
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun isNightHour(epochMs: Long): Boolean {
        val h = hourOf(epochMs); return h >= 22 || h < 6
    }

    private fun hourOf(epochMs: Long): Int =
        java.util.Calendar.getInstance()
            .apply { timeInMillis = epochMs }
            .get(java.util.Calendar.HOUR_OF_DAY)

    private fun dayKey(epochMs: Long): String {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        return "${c.get(java.util.Calendar.YEAR)}-${c.get(java.util.Calendar.DAY_OF_YEAR)}"
    }
}