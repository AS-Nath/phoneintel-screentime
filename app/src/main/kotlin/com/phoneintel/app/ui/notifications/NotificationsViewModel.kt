package com.phoneintel.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.NotificationRepository
import com.phoneintel.app.domain.model.NotificationStat
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class NotificationsUiState(
    val totalToday: Int = 0,
    val totalThisWeek: Int = 0,
    val topNotifiers: List<NotificationStat> = emptyList(),
    val selectedRange: TimeRange = TimeRange.TODAY
)

enum class TimeRange(val label: String, val daysBack: Int) {
    TODAY("Today", 0),
    WEEK("7 Days", 6),
    MONTH("30 Days", 29)
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(TimeRange.TODAY)
    val uiState: StateFlow<NotificationsUiState> = _selectedRange
        .flatMapLatest { range ->
            val since = DateUtil.daysAgo(range.daysBack)
            combine(
                repository.observeTopNotifiers(since),
                repository.observeTotalCount(since),
                repository.observeTotalCount(DateUtil.startOfDay()),
                repository.observeTotalCount(DateUtil.daysAgo(6))
            ) { topNotifiers, total, today, week ->
                NotificationsUiState(
                    totalToday = today,
                    totalThisWeek = week,
                    topNotifiers = topNotifiers,
                    selectedRange = range
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsUiState())

    fun selectRange(range: TimeRange) { _selectedRange.value = range }
}
