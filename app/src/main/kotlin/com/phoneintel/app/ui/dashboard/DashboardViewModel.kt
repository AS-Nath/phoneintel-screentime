package com.phoneintel.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.*
import com.phoneintel.app.domain.model.*
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    // Screen time
    val todayScreenTimeMs: Long = 0L,
    val topApps: List<AppUsageStat> = emptyList(),
    val weeklyTrend: List<DailyUsageSummary> = emptyList(),
    // Attention
    val sessionCount: Int = 0,
    val avgSessionMs: Long = 0L,
    val longestSessionMs: Long = 0L,
    val shortSessionCount: Int = 0,
    val fragmentationIndex: Float = 0f,
    // Notifications
    val todayNotifications: Int = 0,
    // Battery
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    // Health score (lightweight version for dashboard — full breakdown on HealthScreen)
    val healthScore: Int = 0,
    val healthGrade: String = "—",
    // Focus state
    val focusState: FocusState = FocusState(),
    val topInsight: InsightCard? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository,
    private val notificationRepository: NotificationRepository,
    private val networkRepository: NetworkRepository,
    private val batteryRepository: BatteryRepository,
    private val unlockSessionRepository: UnlockSessionRepository,
    private val focusRepository: FocusRepository,
    private val insightRepository: InsightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeReactiveData()
        loadComputedData()
    }

    private fun observeReactiveData() {
        val todayStart = DateUtil.startOfDay()

        appUsageRepository.observeTodayTotalMs()
            .onEach { ms -> _uiState.update { it.copy(todayScreenTimeMs = ms, isLoading = false) } }
            .launchIn(viewModelScope)

        appUsageRepository.observeTodayTopApps()
            .onEach { apps -> _uiState.update { it.copy(topApps = apps) } }
            .launchIn(viewModelScope)

        notificationRepository.observeTotalCount(todayStart)
            .onEach { count -> _uiState.update { it.copy(todayNotifications = count) } }
            .launchIn(viewModelScope)

        batteryRepository.observeLatestSnapshot()
            .onEach { snap -> snap?.let { s ->
                _uiState.update { it.copy(batteryLevel = s.level, isCharging = s.isCharging) }
            }}
            .launchIn(viewModelScope)

        unlockSessionRepository.observeCountToday()
            .onEach { count -> _uiState.update { it.copy(sessionCount = count) } }
            .launchIn(viewModelScope)

        unlockSessionRepository.observeAvgDurationToday()
            .onEach { avg ->
                _uiState.update { it.copy(avgSessionMs = avg) }
                recomputeHealthScore()
            }
            .launchIn(viewModelScope)

        unlockSessionRepository.observeMaxDurationToday()
            .onEach { max -> _uiState.update { it.copy(longestSessionMs = max) } }
            .launchIn(viewModelScope)

        focusRepository.focusState
            .onEach { fs -> _uiState.update { it.copy(focusState = fs) } }
            .launchIn(viewModelScope)
    }

    private fun loadComputedData() {
        viewModelScope.launch {
            val trend = appUsageRepository.getWeeklyTrend()
            _uiState.update { it.copy(weeklyTrend = trend) }
            val insights = runCatching { insightRepository.getInsights() }.getOrDefault(emptyList())
            _uiState.update { it.copy(topInsight = insights.firstOrNull()) }
            // Compute lightweight health score from today's data
            recomputeHealthScore()
        }
    }

    private suspend fun recomputeHealthScore() {
        val sessions = unlockSessionRepository.getCompletedToday()
        val shortCount = sessions.count { it.durationMs < 3 * 60_000L }
        val frag = if (sessions.isEmpty()) 0f else (shortCount.toFloat() / sessions.size).coerceIn(0f, 1f)
        val nightMs = sessions.filter { isNightHour(it.unlockTime) }.sumOf { it.durationMs }
        val screenMs = _uiState.value.todayScreenTimeMs
        val notifCount = _uiState.value.todayNotifications
        val longestMs = sessions.maxOfOrNull { it.durationMs } ?: 0L

        val score = PhoneHealthScore.compute(
            totalScreenTimeMs = screenMs,
            nightUsageMs = nightMs,
            unlockCount = sessions.size,
            longestSessionMs = longestMs,
            notificationCount = notifCount
        )
        _uiState.update {
            it.copy(
                shortSessionCount = shortCount,
                fragmentationIndex = frag,
                healthScore = score.score,
                healthGrade = score.grade
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            appUsageRepository.syncUsageStats()
            networkRepository.syncNetworkStats()
            val trend = appUsageRepository.getWeeklyTrend()
            _uiState.update { it.copy(weeklyTrend = trend) }
            recomputeHealthScore()
        }
    }

    private fun isNightHour(epochMs: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 6
    }
}
