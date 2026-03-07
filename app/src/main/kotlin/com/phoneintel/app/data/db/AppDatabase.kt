package com.phoneintel.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phoneintel.app.data.db.dao.*
import com.phoneintel.app.data.db.entities.*

@Database(
    entities = [
        AppUsageEntity::class,
        NotificationEventEntity::class,
        NetworkUsageEntity::class,
        BluetoothEventEntity::class,
        BatterySnapshotEntity::class,
        DailySummaryEntity::class,
        UnlockSessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun networkUsageDao(): NetworkUsageDao
    abstract fun bluetoothDao(): BluetoothDao
    abstract fun batteryDao(): BatteryDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun unlockSessionDao(): UnlockSessionDao

    companion object {
        const val DATABASE_NAME = "phoneintel.db"
    }
}
