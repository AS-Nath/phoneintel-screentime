package com.phoneintel.app.ui.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.db.dao.AppUsageDao
import com.phoneintel.app.data.db.dao.BatteryDao
import com.phoneintel.app.data.db.dao.BluetoothDao
import com.phoneintel.app.data.db.dao.NetworkUsageDao
import com.phoneintel.app.data.db.dao.NotificationDao
import com.phoneintel.app.domain.model.*
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecapUiState(
    val isLoading: Boolean = true,
    val year: Int = DateUtil.currentYear(),
    val recap: YearRecap? = null
)

@HiltViewModel
class RecapViewModel @Inject constructor(
    private val appUsageDao: AppUsageDao,
    private val notificationDao: NotificationDao,
    private val networkDao: NetworkUsageDao,
    private val batteryDao: BatteryDao,
    private val bluetoothDao: BluetoothDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecapUiState())
    val uiState: StateFlow<RecapUiState> = _uiState.asStateFlow()

    init { buildRecap(DateUtil.currentYear()) }

    fun buildRecap(year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, year = year) }

            val from = DateUtil.startOfYear(year)
            val to = DateUtil.endOfYear(year)

            // Pull directly from populated tables — daily_summary is never written
            val appRows = appUsageDao.getUsageRange(from, to)
            val notifCount = notificationDao.getRange(from, to).size
            val networkRows = networkDao.getRange(from, to)
            val batteryRows = batteryDao.getRange(from, to)
            val btEvents = bluetoothDao.getRange(from, to)

            val totalScreenTimeMs = appRows.sumOf { it.totalForegroundMs }
            val avgDailyMs = if (appRows.isEmpty()) 0L else {
                val days = appRows.map { DateUtil.startOfDay(it.date) }.toSet().size.coerceAtLeast(1)
                totalScreenTimeMs / days
            }

            val totalWifi = networkRows.filter { it.connectionType == "WIFI" }
                .sumOf { it.bytesReceived + it.bytesSent }
            val totalMobile = networkRows.filter { it.connectionType == "MOBILE" }
                .sumOf { it.bytesReceived + it.bytesSent }

            val topApps = appRows
                .groupBy { it.packageName }
                .map { (_, rows) ->
                    AppUsageStat(
                        packageName = rows.first().packageName,
                        appName = rows.first().appName,
                        totalForegroundMs = rows.sumOf { it.totalForegroundMs },
                        launchCount = rows.sumOf { it.launchCount },
                        lastUsed = rows.maxOf { it.lastUsed },
                        iconDrawable = null
                    )
                }
                .sortedByDescending { it.totalForegroundMs }
                .take(5)

            // Most productive month = month with lowest avg daily screen time
            val byMonth = appRows.groupBy { DateUtil.monthName(it.date) }
            val mostProductiveMonth = byMonth.minByOrNull { (_, rows) ->
                val days = rows.map { DateUtil.startOfDay(it.date) }.toSet().size.coerceAtLeast(1)
                rows.sumOf { it.totalForegroundMs } / days
            }?.key

            // Longest daily streak where app was used
            val usageDates = appRows.map { DateUtil.startOfDay(it.date) }.toSet()
            val streak = calculateLongestStreak(usageDates)

            // Charging cycles = number of times battery went from not-charging to charging
            val chargingCycles = batteryRows.zipWithNext().count { (a, b) ->
                !a.isCharging && b.isCharging
            }

            val btConnections = btEvents.count { it.eventType == "CONNECTED" }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    recap = YearRecap(
                        year = year,
                        totalScreenTimeMs = totalScreenTimeMs,
                        totalNotifications = notifCount,
                        totalWifiBytes = totalWifi,
                        totalMobileBytes = totalMobile,
                        topApps = topApps,
                        topNotifier = null,
                        longestStreakDays = streak,
                        mostProductiveMonth = mostProductiveMonth,
                        avgDailyScreenTimeMs = avgDailyMs,
                        bluetoothDeviceCount = btConnections,
                        chargingCycles = chargingCycles
                    )
                )
            }
        }
    }

    private fun calculateLongestStreak(dates: Set<Long>): Int {
        if (dates.isEmpty()) return 0
        val dayMs = 86_400_000L
        val sorted = dates.sorted()
        var maxStreak = 1; var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] - sorted[i - 1] <= dayMs) current++ else current = 1
            if (current > maxStreak) maxStreak = current
        }
        return maxStreak
    }
}
