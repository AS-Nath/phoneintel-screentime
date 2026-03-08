package com.phoneintel.app.data.db.dao

import androidx.room.*
import com.phoneintel.app.data.db.entities.*
import kotlinx.coroutines.flow.Flow

// ─── App Usage DAO ────────────────────────────────────────────────────────────

@Dao
interface AppUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage WHERE date >= :fromDate ORDER BY totalForegroundMs DESC")
    fun observeUsageSince(fromDate: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :from AND :to ORDER BY totalForegroundMs DESC")
    suspend fun getUsageRange(from: Long, to: Long): List<AppUsageEntity>

    @Query("""
        SELECT 0 as id, packageName, appName, SUM(totalForegroundMs) as totalForegroundMs,
               SUM(launchCount) as launchCount, MAX(lastUsed) as lastUsed, :today as date
        FROM app_usage WHERE date >= :fromDate
        GROUP BY packageName ORDER BY totalForegroundMs DESC LIMIT :limit
    """)
    suspend fun getTopApps(fromDate: Long, today: Long, limit: Int = 10): List<AppUsageEntity>

    @Query("SELECT SUM(totalForegroundMs) FROM app_usage WHERE date = :date")
    fun observeDailyTotal(date: Long): Flow<Long?>

    @Query("SELECT * FROM app_usage WHERE date >= :fromDate ORDER BY date ASC")
    suspend fun getAllSince(fromDate: Long): List<AppUsageEntity>

    @Query("DELETE FROM app_usage WHERE date < :beforeDate")
    suspend fun pruneOlderThan(beforeDate: Long)
}

// ─── Notification DAO ─────────────────────────────────────────────────────────

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEventEntity): Long

    @Insert
    suspend fun insertAll(entities: List<NotificationEventEntity>)

    @Update
    suspend fun update(entity: NotificationEventEntity)

    @Query("SELECT * FROM notification_events WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    fun observeSince(fromTime: Long): Flow<List<NotificationEventEntity>>

    @Query("""
        SELECT packageName, appName, COUNT(*) as count
        FROM notification_events WHERE timestamp >= :fromTime
        GROUP BY packageName ORDER BY count DESC LIMIT :limit
    """)
    fun observeTopNotifiers(fromTime: Long, limit: Int = 10): Flow<List<NotificationCountProjection>>

    @Query("SELECT COUNT(*) FROM notification_events WHERE timestamp >= :fromTime")
    fun observeTotalCount(fromTime: Long): Flow<Int>

    @Query("SELECT * FROM notification_events WHERE timestamp BETWEEN :from AND :to")
    suspend fun getRange(from: Long, to: Long): List<NotificationEventEntity>

    @Query("DELETE FROM notification_events WHERE timestamp < :beforeTime")
    suspend fun pruneOlderThan(beforeTime: Long)
}

data class NotificationCountProjection(
    val packageName: String,
    val appName: String,
    val count: Int
)

// ─── Network DAO ──────────────────────────────────────────────────────────────

@Dao
interface NetworkUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NetworkUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NetworkUsageEntity>)

    @Query("SELECT * FROM network_usage WHERE date >= :fromDate ORDER BY bytesReceived + bytesSent DESC")
    fun observeSince(fromDate: Long): Flow<List<NetworkUsageEntity>>

    @Query("""
        SELECT 0 as id, packageName, appName, connectionType,
               SUM(bytesReceived) as bytesReceived, SUM(bytesSent) as bytesSent,
               :today as date
        FROM network_usage WHERE date >= :fromDate
        GROUP BY packageName, connectionType
        ORDER BY (bytesReceived + bytesSent) DESC LIMIT :limit
    """)
    suspend fun getTopDataUsers(fromDate: Long, today: Long, limit: Int = 15): List<NetworkUsageEntity>

    @Query("SELECT SUM(bytesReceived + bytesSent) FROM network_usage WHERE date >= :fromDate AND connectionType = :type")
    fun observeTotalByType(fromDate: Long, type: String): Flow<Long?>

    @Query("SELECT * FROM network_usage WHERE date BETWEEN :from AND :to")
    suspend fun getRange(from: Long, to: Long): List<NetworkUsageEntity>

    @Query("DELETE FROM network_usage WHERE date < :beforeDate")
    suspend fun pruneOlderThan(beforeDate: Long)
}

// ─── Bluetooth DAO ────────────────────────────────────────────────────────────

@Dao
interface BluetoothDao {

    @Insert
    suspend fun insert(entity: BluetoothEventEntity): Long

    @Insert
    suspend fun insertAll(entities: List<BluetoothEventEntity>)

    @Update
    suspend fun update(entity: BluetoothEventEntity)

    @Query("SELECT * FROM bluetooth_events WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    fun observeSince(fromTime: Long): Flow<List<BluetoothEventEntity>>

    @Query("""
        SELECT * FROM bluetooth_events
        WHERE eventType = 'CONNECTED' AND timestamp >= :fromTime
        ORDER BY timestamp DESC
    """)
    fun observeConnections(fromTime: Long): Flow<List<BluetoothEventEntity>>

    @Query("SELECT DISTINCT deviceName, deviceAddress, deviceClass, MAX(timestamp) as timestamp, eventType, connectedDurationMs, id FROM bluetooth_events GROUP BY deviceAddress ORDER BY timestamp DESC")
    fun observeKnownDevices(): Flow<List<BluetoothEventEntity>>

    @Query("SELECT * FROM bluetooth_events WHERE timestamp BETWEEN :from AND :to")
    suspend fun getRange(from: Long, to: Long): List<BluetoothEventEntity>

    @Query("DELETE FROM bluetooth_events WHERE timestamp < :beforeTime")
    suspend fun pruneOlderThan(beforeTime: Long)
}

// ─── Battery DAO ──────────────────────────────────────────────────────────────

@Dao
interface BatteryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BatterySnapshotEntity): Long

    @Query("SELECT * FROM battery_snapshots WHERE timestamp >= :fromTime ORDER BY timestamp ASC")
    fun observeSince(fromTime: Long): Flow<List<BatterySnapshotEntity>>

    @Query("SELECT * FROM battery_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<BatterySnapshotEntity?>

    @Query("SELECT AVG(level) FROM battery_snapshots WHERE timestamp >= :fromTime")
    fun observeAverageLevel(fromTime: Long): Flow<Float?>

    @Query("SELECT * FROM battery_snapshots WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun getRange(from: Long, to: Long): List<BatterySnapshotEntity>

    @Query("DELETE FROM battery_snapshots WHERE timestamp < :beforeTime")
    suspend fun pruneOlderThan(beforeTime: Long)
}

// ─── Unlock Session DAO ───────────────────────────────────────────────────────

@Dao
interface UnlockSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UnlockSessionEntity): Long

    @Query("UPDATE unlock_sessions SET lockTime = :lockTime, durationMs = :durationMs WHERE id = :id")
    suspend fun closeSession(id: Long, lockTime: Long, durationMs: Long)

    @Query("SELECT * FROM unlock_sessions WHERE unlockTime >= :fromTime ORDER BY unlockTime DESC")
    fun observeSince(fromTime: Long): Flow<List<UnlockSessionEntity>>

    @Query("SELECT * FROM unlock_sessions WHERE unlockTime >= :fromTime AND durationMs > 0 ORDER BY unlockTime ASC")
    suspend fun getCompletedSince(fromTime: Long): List<UnlockSessionEntity>

    @Query("SELECT COUNT(*) FROM unlock_sessions WHERE unlockTime >= :fromTime")
    fun observeCountSince(fromTime: Long): Flow<Int>

    @Query("SELECT AVG(durationMs) FROM unlock_sessions WHERE unlockTime >= :fromTime AND durationMs > 0")
    fun observeAvgDuration(fromTime: Long): Flow<Long?>

    @Query("SELECT MAX(durationMs) FROM unlock_sessions WHERE unlockTime >= :fromTime")
    fun observeMaxDuration(fromTime: Long): Flow<Long?>

    @Query("DELETE FROM unlock_sessions WHERE unlockTime < :beforeTime")
    suspend fun pruneOlderThan(beforeTime: Long)
}

// ─── Daily Summary DAO ────────────────────────────────────────────────────────

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailySummaryEntity)

    @Query("SELECT * FROM daily_summary ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    suspend fun getForDate(date: Long): DailySummaryEntity?

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun getRange(from: Long, to: Long): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summary ORDER BY date ASC")
    suspend fun getAll(): List<DailySummaryEntity>

    @Query("DELETE FROM daily_summary WHERE date < :beforeDate")
    suspend fun pruneOlderThan(beforeDate: Long)
}

// ─── XP DAO ───────────────────────────────────────────────────────────────────

@Dao
interface XpDao {

    @Insert
    suspend fun insert(event: XpEventEntity): Long

    @Query("SELECT * FROM xp_ledger ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<XpEventEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger")
    fun observeTotalXp(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger")
    suspend fun getTotalXp(): Int

    @Query("SELECT COUNT(*) FROM xp_ledger WHERE type = 'HEALTH_SCORE_TICK' AND timestamp >= :fromTime")
    suspend fun getTickCountSince(fromTime: Long): Int
}