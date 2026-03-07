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

    init { load() }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val todayStart = DateUtil.startOfDay()

            // Completed unlock sessions today
            val sessions = unlockSessionRepository.getCompletedSince(todayStart)

            // Night usage: sessions whose unlock fell in the 23:00–06:00 window
            val nightMs = sessions.filter { isNightHour(it.unlockTime) }.sumOf { it.durationMs }

            // Attention stats
            val sessionCount = sessions.size
            val avgMs = if (sessions.isEmpty()) 0L else sessions.sumOf { it.durationMs } / sessions.size
            val longestMs = sessions.maxOfOrNull { it.durationMs } ?: 0L
            val shortCount = sessions.count { it.durationMs < 3 * 60_000L }
            val frag = if (sessionCount == 0) 0f
                       else (shortCount.toFloat() / sessionCount).coerceIn(0f, 1f)
            val attention = AttentionStats(sessionCount, avgMs, longestMs, shortCount, frag)

            val screenTimeMs = appUsageRepository.observeTodayTotalMs().first()
            val notifCount = notificationRepository.observeTotalCount(todayStart).first()

            val score = PhoneHealthScore.compute(
                totalScreenTimeMs = screenTimeMs,
                nightUsageMs = nightMs,
                unlockCount = sessionCount,
                longestSessionMs = longestMs,
                notificationCount = notifCount
            )

            _uiState.update { it.copy(score = score, attention = attention, isLoading = false) }
        }
    }

    private fun isNightHour(epochMs: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 6
    }
}
