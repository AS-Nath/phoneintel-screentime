package com.phoneintel.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.AppUsageRepository
import com.phoneintel.app.data.repository.NotificationRepository
import com.phoneintel.app.data.repository.UnlockSessionRepository
import com.phoneintel.app.domain.model.AttentionStats
import com.phoneintel.app.domain.model.PhoneHealthScore
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class PhoneHealthUiState(
    val score: PhoneHealthScore? = null,
    val attention: AttentionStats? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class PhoneHealthViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository,
    private val notificationRepository: NotificationRepository,
    private val unlockSessionRepository: UnlockSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneHealthUiState())
    val uiState: StateFlow<PhoneHealthUiState> = _uiState.asStateFlow()

    init {
        observeReactiveData()
    }

    fun refresh() {
        // Force a recompute — the flows will re-emit on their own, but this
        // lets the user manually trigger a refresh if needed.
        viewModelScope.launch { recompute() }
    }

    private fun observeReactiveData() {
        val todayStart = DateUtil.startOfDay()

        // Re-recompute whenever a session completes (avg duration changes)
        // or the session count changes (new unlock).
        unlockSessionRepository.observeAvgDurationToday()
            .onEach { recompute() }
            .launchIn(viewModelScope)

        unlockSessionRepository.observeCountToday()
            .onEach { recompute() }
            .launchIn(viewModelScope)

        // Also recompute when screen time changes (affects the score).
        appUsageRepository.observeTodayTotalMs()
            .onEach { recompute() }
            .launchIn(viewModelScope)

        notificationRepository.observeTotalCount(todayStart)
            .onEach { recompute() }
            .launchIn(viewModelScope)
    }

    private suspend fun recompute() {
        val todayStart = DateUtil.startOfDay()

        // Completed sessions — used for duration-based stats (avg, longest, short count).
        // The active session is excluded here intentionally: its duration is 0 and
        // would skew the average and longest-session values downward.
        val completedSessions = unlockSessionRepository.getCompletedSince(todayStart)

        // Total unlock count includes the active session so the score reflects
        // reality (you've unlocked N times, even if the Nth is still open).
        val totalUnlockCount = completedSessions.size +
                if (unlockSessionRepository.hasActiveSession()) 1 else 0

        val nightMs = completedSessions
            .filter { isNightHour(it.unlockTime) }
            .sumOf { it.durationMs }

        val avgMs = if (completedSessions.isEmpty()) 0L
        else completedSessions.sumOf { it.durationMs } / completedSessions.size
        val longestMs = completedSessions.maxOfOrNull { it.durationMs } ?: 0L
        val shortCount = completedSessions.count { it.durationMs < 3 * 60_000L }
        val frag = if (completedSessions.isEmpty()) 0f
        else (shortCount.toFloat() / completedSessions.size).coerceIn(0f, 1f)

        val attention = AttentionStats(
            sessionCount  = totalUnlockCount,
            avgSessionMs  = avgMs,
            longestSessionMs = longestMs,
            shortSessionCount = shortCount,
            fragmentationIndex = frag
        )

        val screenTimeMs = appUsageRepository.observeTodayTotalMs().first()
        val notifCount   = notificationRepository.observeTotalCount(todayStart).first()

        val score = PhoneHealthScore.compute(
            totalScreenTimeMs = screenTimeMs,
            nightUsageMs      = nightMs,
            unlockCount       = totalUnlockCount,
            longestSessionMs  = longestMs,
            notificationCount = notifCount
        )

        _uiState.update { it.copy(score = score, attention = attention, isLoading = false) }
    }

    private fun isNightHour(epochMs: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 6
    }
}