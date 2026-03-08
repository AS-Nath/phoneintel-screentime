package com.phoneintel.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.InsightRepository
import com.phoneintel.app.domain.model.InsightCard
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
            _uiState.update { it.copy(insights = insights, isLoading = false) }
        }
    }
}