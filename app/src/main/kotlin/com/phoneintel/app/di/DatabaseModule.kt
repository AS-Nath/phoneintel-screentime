package com.phoneintel.app.di

import android.content.Context
import androidx.room.Room
import com.phoneintel.app.data.db.AppDatabase
import com.phoneintel.app.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAppUsageDao(db: AppDatabase): AppUsageDao = db.appUsageDao()
    @Provides fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()
    @Provides fun provideNetworkDao(db: AppDatabase): NetworkUsageDao = db.networkUsageDao()
    @Provides fun provideBluetoothDao(db: AppDatabase): BluetoothDao = db.bluetoothDao()
    @Provides fun provideBatteryDao(db: AppDatabase): BatteryDao = db.batteryDao()
    @Provides fun provideDailySummaryDao(db: AppDatabase): DailySummaryDao = db.dailySummaryDao()
    @Provides fun provideUnlockSessionDao(db: AppDatabase): UnlockSessionDao = db.unlockSessionDao()
}
