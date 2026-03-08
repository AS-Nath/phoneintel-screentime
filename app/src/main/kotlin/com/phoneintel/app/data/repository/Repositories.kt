package com.phoneintel.app.data.repository

import android.Manifest
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.phoneintel.app.data.db.dao.*
import com.phoneintel.app.data.db.dao.UnlockSessionDao
import com.phoneintel.app.data.db.entities.*
import com.phoneintel.app.domain.model.*
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


// ─── App Name Helper ─────────────────────────────────────────────────────────
// Build a full packageName→label map upfront from getInstalledApplications(0).
// This is the only reliable approach on API 30+ where per-package
// getApplicationInfo() silently fails due to package visibility restrictions.

private fun buildAppNameMap(pm: PackageManager): Map<String, String> =
    pm.getInstalledApplications(0).associate { appInfo ->
        val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
            .getOrDefault(appInfo.packageName)
        appInfo.packageName to label
    }

// Resolve a single name using the pre-built map, with the full package name
// as the fallback (never a partial segment — partial names like "Launcher" or
// "Game" are misleading and worse than showing the full package name).
private fun resolveAppName(packageName: String, nameMap: Map<String, String>): String =
    nameMap[packageName]?.takeIf { it.isNotBlank() && it != packageName }
        ?: packageName

// ─── App Usage Repository ─────────────────────────────────────────────────────

@Singleton
class AppUsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppUsageDao
) {
    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    private val pm: PackageManager get() = context.packageManager

    suspend fun syncUsageStats() = withContext(Dispatchers.IO) {
        val startOfDay = DateUtil.startOfDay()
        val now = System.currentTimeMillis()

        // Build name map once for the whole sync — reliable on all API levels
        val nameMap = buildAppNameMap(pm)

        // ── Use queryEvents instead of queryUsageStats(INTERVAL_DAILY) ──────────
        // INTERVAL_DAILY uses Android's internal bucket boundaries which do NOT
        // align with calendar midnight. This causes wildly wrong totals and values
        // that change during the day as bucket boundaries shift.
        // queryEvents gives us precise MOVE_TO_FOREGROUND/BACKGROUND timestamps
        // so we can calculate exact foreground time for today only.
        val usageEvents = usageStatsManager.queryEvents(startOfDay, now)
            ?: return@withContext

        val event = UsageEvents.Event()
        // packageName -> timestamp when it entered foreground (null if not in fg)
        val foregroundStart = mutableMapOf<String, Long>()
        // packageName -> total foreground ms accumulated so far today
        val foregroundTotal = mutableMapOf<String, Long>()
        // packageName -> last time used
        val lastUsedMap = mutableMapOf<String, Long>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val pkg = event.packageName ?: continue
            lastUsedMap[pkg] = maxOf(lastUsedMap[pkg] ?: 0L, event.timeStamp)

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    foregroundStart[pkg] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = foregroundStart.remove(pkg) ?: continue
                    foregroundTotal[pkg] = (foregroundTotal[pkg] ?: 0L) + (event.timeStamp - start)
                }
            }
        }

        // Any app still in foreground at query time — credit time up to now
        foregroundStart.forEach { (pkg, start) ->
            foregroundTotal[pkg] = (foregroundTotal[pkg] ?: 0L) + (now - start)
        }

        val entities = foregroundTotal
            .filter { (_, ms) -> ms > 0L }
            .map { (pkg, ms) ->
                AppUsageEntity(
                    packageName = pkg,
                    appName = resolveAppName(pkg, nameMap),
                    date = startOfDay,
                    totalForegroundMs = ms,
                    launchCount = 0,
                    lastUsed = lastUsedMap[pkg] ?: now
                )
            }

        dao.upsertAll(entities)
    }

    fun observeTodayTopApps(limit: Int = 10): Flow<List<AppUsageStat>> =
        dao.observeUsageSince(DateUtil.startOfDay())
            .map { list ->
                list.take(limit).map { entity ->
                    entity.toDomain(loadIcon(entity.packageName))
                }
            }

    fun observeTodayTotalMs(): Flow<Long> =
        dao.observeDailyTotal(DateUtil.startOfDay()).map { it ?: 0L }

    suspend fun getWeeklyTrend(): List<DailyUsageSummary> = withContext(Dispatchers.IO) {
        val from = DateUtil.daysAgo(6)
        dao.getAllSince(from).groupBy { DateUtil.startOfDay(it.date) }.map { (day, entries) ->
            DailyUsageSummary(
                date = day,
                totalScreenTimeMs = entries.sumOf { it.totalForegroundMs },
                topApps = entries.sortedByDescending { it.totalForegroundMs }
                    .take(3).map { it.toDomain(loadIcon(it.packageName)) }
            )
        }.sortedBy { it.date }
    }

    private fun loadIcon(packageName: String) = runCatching {
        pm.getApplicationIcon(packageName)
    }.getOrNull()

    private fun AppUsageEntity.toDomain(icon: android.graphics.drawable.Drawable?) =
        AppUsageStat(packageName, appName, totalForegroundMs, launchCount, lastUsed, icon)

    suspend fun getRawUsageSince(fromDate: Long): List<AppUsageEntity> =
        withContext(Dispatchers.IO) { dao.getAllSince(fromDate) }

}

// ─── Notification Repository ──────────────────────────────────────────────────

@Singleton
class NotificationRepository @Inject constructor(
    private val dao: NotificationDao
) {
    fun observeTopNotifiers(since: Long): Flow<List<NotificationStat>> =
        dao.observeTopNotifiers(since).map { list ->
            list.map { NotificationStat(it.packageName, it.appName, it.count) }
        }

    fun observeTotalCount(since: Long): Flow<Int> = dao.observeTotalCount(since)

    suspend fun insertEvent(entity: NotificationEventEntity) = dao.insert(entity)

    fun observeRecent(since: Long): Flow<List<NotificationEvent>> =
        dao.observeSince(since).map { list ->
            list.map { NotificationEvent(it.id, it.packageName, it.appName, it.timestamp, it.category, it.isOngoing) }
        }

    suspend fun getRawSince(since: Long): List<NotificationEventEntity> =
        withContext(Dispatchers.IO) { dao.getRange(since, System.currentTimeMillis()) }
}

// ─── Network Repository ───────────────────────────────────────────────────────

@Singleton
class NetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: NetworkUsageDao
) {
    private val networkStatsManager by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    }
    private val pm: PackageManager get() = context.packageManager

    suspend fun syncNetworkStats(fromDate: Long = DateUtil.startOfDay()) = withContext(Dispatchers.IO) {
        val endTime = System.currentTimeMillis()
        val entities = mutableListOf<NetworkUsageEntity>()
        val today = DateUtil.startOfDay()

        // Build UID -> package map and name map together in one pass
        val uidToPackage = mutableMapOf<Int, String>()
        val nameMap = mutableMapOf<String, String>()
        pm.getInstalledApplications(0).forEach { appInfo ->
            uidToPackage[appInfo.uid] = appInfo.packageName
            val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(appInfo.packageName)
            nameMap[appInfo.packageName] = label
        }

        // Get subscriber ID for accurate mobile data — requires READ_PHONE_STATE.
        // On devices that restrict it, fall back to null (WiFi still works fine).
        val subscriberId: String? = runCatching {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) null // restricted on Q+
                else @Suppress("DEPRECATION") tm.subscriberId
            } else null
        }.getOrNull()

        for ((connType, connLabel, subId) in listOf(
            Triple(ConnectivityManager.TYPE_WIFI, "WIFI", null as String?),
            Triple(ConnectivityManager.TYPE_MOBILE, "MOBILE", subscriberId)
        )) {
            runCatching {
                val stats = networkStatsManager.querySummary(connType, subId, fromDate, endTime)
                val bucket = android.app.usage.NetworkStats.Bucket()
                val uidTotals = mutableMapOf<Int, Pair<Long, Long>>()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val uid = bucket.uid
                    val prev = uidTotals[uid] ?: (0L to 0L)
                    uidTotals[uid] = (prev.first + bucket.rxBytes) to (prev.second + bucket.txBytes)
                }
                stats.close()
                var systemRx = 0L; var systemTx = 0L
                uidTotals.forEach { (uid, rxTx) ->
                    val (rx, tx) = rxTx
                    if (rx + tx == 0L) return@forEach
                    val packageName = uidToPackage[uid]
                    if (packageName == null) {
                        // System UIDs — accumulate separately so totals are accurate
                        systemRx += rx; systemTx += tx
                        return@forEach
                    }
                    val appName = resolveAppName(packageName, nameMap)
                    entities.add(NetworkUsageEntity(
                        packageName = packageName, appName = appName,
                        date = today, connectionType = connLabel,
                        bytesReceived = rx, bytesSent = tx
                    ))
                }
                if (systemRx + systemTx > 0) {
                    entities.add(NetworkUsageEntity(
                        packageName = "_system_", appName = "System & Other",
                        date = today, connectionType = connLabel,
                        bytesReceived = systemRx, bytesSent = systemTx
                    ))
                }
            }
        }
        dao.upsertAll(entities)
    }

    fun observeTopDataUsers(since: Long): Flow<List<AppNetworkUsage>> =
        dao.observeSince(since).map { list ->
            list.groupBy { it.packageName }.map { (pkg, entries) ->
                AppNetworkUsage(
                    packageName = pkg,
                    appName = entries.first().appName,
                    bytesReceived = entries.sumOf { it.bytesReceived },
                    bytesSent = entries.sumOf { it.bytesSent },
                    connectionType = ConnectionType.UNKNOWN,
                    iconDrawable = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
                )
            }.sortedByDescending { it.totalBytes }
        }

    fun observeTotalWifi(since: Long): Flow<Long> =
        dao.observeTotalByType(since, "WIFI").map { it ?: 0L }

    fun observeTotalMobile(since: Long): Flow<Long> =
        dao.observeTotalByType(since, "MOBILE").map { it ?: 0L }
}

// ─── Unlock Session Repository ────────────────────────────────────────────────

@Singleton
class UnlockSessionRepository @Inject constructor(
    private val dao: UnlockSessionDao
) {
    /** Called when ACTION_USER_PRESENT fires. Returns the new session id. */
    suspend fun startSession(unlockTime: Long): Long = withContext(Dispatchers.IO) {
        dao.insert(UnlockSessionEntity(unlockTime = unlockTime))
    }

    /** Called when ACTION_SCREEN_OFF fires. Closes the open session. */
    suspend fun endSession(sessionId: Long, lockTime: Long, durationMs: Long) =
        withContext(Dispatchers.IO) {
            dao.closeSession(sessionId, lockTime, durationMs)
        }

    fun observeCountToday(): Flow<Int> = dao.observeCountSince(
        com.phoneintel.app.util.DateUtil.startOfDay()
    )

    fun observeAvgDurationToday(): Flow<Long> =
        dao.observeAvgDuration(com.phoneintel.app.util.DateUtil.startOfDay())
            .map { it ?: 0L }

    fun observeMaxDurationToday(): Flow<Long> =
        dao.observeMaxDuration(com.phoneintel.app.util.DateUtil.startOfDay())
            .map { it ?: 0L }

    suspend fun getCompletedToday(): List<UnlockSessionEntity> =
        withContext(Dispatchers.IO) {
            dao.getCompletedSince(com.phoneintel.app.util.DateUtil.startOfDay())
        }

    suspend fun getCompletedSince(fromTime: Long): List<UnlockSessionEntity> =
        withContext(Dispatchers.IO) { dao.getCompletedSince(fromTime) }
}

// ─── Battery Repository ───────────────────────────────────────────────────────

@Singleton
class BatteryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: BatteryDao
) {
    fun observeLatestSnapshot(): Flow<BatterySnapshot?> =
        dao.observeLatest().map { it?.toDomain() }

    fun observeSnapshots(since: Long): Flow<List<BatterySnapshot>> =
        dao.observeSince(since).map { list -> list.map { it.toDomain() } }

    fun observeAverageLevel(since: Long): Flow<Float> =
        dao.observeAverageLevel(since).map { it ?: 0f }

    // Called directly from DataCollectionService's battery broadcast receiver
    suspend fun insertSnapshot(entity: BatterySnapshotEntity) = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    private fun BatterySnapshotEntity.toDomain() =
        BatterySnapshot(timestamp, level, isCharging, chargeType, temperature, voltage, health)
}

// ─── Bluetooth Repository ─────────────────────────────────────────────────────

@Singleton
class BluetoothRepository @Inject constructor(
    private val dao: BluetoothDao
) {
    fun observeKnownDevices(): Flow<List<BluetoothDevice>> =
        dao.observeKnownDevices().map { list ->
            list.map {
                BluetoothDevice(it.deviceName, it.deviceAddress, it.deviceClass, it.timestamp, it.connectedDurationMs, 1)
            }
        }

    fun observeRecentEvents(since: Long): Flow<List<BluetoothEvent>> =
        dao.observeSince(since).map { list ->
            list.map {
                BluetoothEvent(it.id, it.deviceName, it.deviceAddress,
                    BluetoothEventType.valueOf(it.eventType), it.timestamp, it.connectedDurationMs)
            }
        }

    suspend fun insertEvent(entity: BluetoothEventEntity) = dao.insert(entity)
}

// ─── Insight Repository ───────────────────────────────────────────────────────

@Singleton
class InsightRepository @Inject constructor(
    private val unlockSessionRepository: UnlockSessionRepository,
    private val appUsageRepository: AppUsageRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend fun getInsights(): List<InsightCard> = withContext(Dispatchers.IO) {
        val sevenDaysAgo = DateUtil.daysAgo(6)
        val sessions      = unlockSessionRepository.getCompletedSince(sevenDaysAgo)
        val appUsage      = appUsageRepository.getRawUsageSince(sevenDaysAgo)
        val notifications = notificationRepository.getRawSince(sevenDaysAgo)
        InsightEngine.analyse(sessions, appUsage, notifications)
    }
}