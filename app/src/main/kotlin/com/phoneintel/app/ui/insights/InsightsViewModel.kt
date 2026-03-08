package com.phoneintel.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.InsightRepository
import com.phoneintel.app.domain.model.InsightAction
import com.phoneintel.app.domain.model.InsightCard
import com.phoneintel.app.domain.model.InsightSeverity
import com.phoneintel.app.domain.model.InsightType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: List<InsightCard> = emptyList()
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightRepository: InsightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val insights = runCatching { insightRepository.getInsights() }.getOrDefault(emptyList())
            val display = insights.ifEmpty { demoInsights() }
            _uiState.update { it.copy(insights = display, isLoading = false) }
        }
    }

    private fun demoInsights(): List<InsightCard> = listOf(
        InsightCard(
            type = InsightType.COMPULSIVE_CHECKER,
            headline = "Compulsive checking between 22:00–24:00",
            body = "You've unlocked your phone 14 times in the 22:00–24:00 window this week, " +
                    "with an average session of just 1m. " +
                    "This pattern often signals avoidance. " +
                    "What happens at 22:00 that makes you reach for your phone?",
            action = InsightAction("Block distractions", "focus"),
            severity = InsightSeverity.ALERT
        ),
        InsightCard(
            type = InsightType.SINGLE_APP_SINK,
            headline = "Instagram is absorbing your time",
            body = "Instagram accounts for 52% of your total screen time this week, " +
                    "with an average session of 34m. " +
                    "You're not checking it compulsively — you're getting lost in it. " +
                    "Try blocking it during your most productive hours.",
            action = InsightAction("Block in Focus", "focus"),
            severity = InsightSeverity.WARN
        ),
        InsightCard(
            type = InsightType.NIGHT_HABIT,
            headline = "You're losing sleep to your screen",
            body = "You've used your phone after 10pm on 5 of the last 7 nights. " +
                    "Your latest unlock this week was at 23:00, " +
                    "with an average session of 18m at that hour. " +
                    "Try leaving your phone in another room from 10pm.",
            action = InsightAction("Start Sleep Focus", "focus"),
            severity = InsightSeverity.WARN
        ),
        InsightCard(
            type = InsightType.NOTIFICATION_DRIVER,
            headline = "Notifications are pulling you in",
            body = "On days with more notifications you unlock your phone 41% more often. " +
                    "WhatsApp sent 87 notifications this week — the most of any app. " +
                    "Reducing its alerts could meaningfully cut your unlock count.",
            action = InsightAction("View notifications", "notifications"),
            severity = InsightSeverity.WARN
        )
    )
}