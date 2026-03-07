package com.phoneintel.app.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.NetworkRepository
import com.phoneintel.app.domain.model.AppNetworkUsage
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class NetworkUiState(
    val totalWifi: Long = 0L,
    val totalMobile: Long = 0L,
    val topUsers: List<AppNetworkUsage> = emptyList(),
    val selectedRange: Int = 0
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val repository: NetworkRepository
) : ViewModel() {
    private val _daysBack = MutableStateFlow(0)
    val uiState: StateFlow<NetworkUiState> = _daysBack
        .flatMapLatest { days ->
            val since = DateUtil.daysAgo(days)
            combine(
                repository.observeTopDataUsers(since),
                repository.observeTotalWifi(since),
                repository.observeTotalMobile(since)
            ) { users, wifi, mobile ->
                NetworkUiState(totalWifi = wifi, totalMobile = mobile, topUsers = users, selectedRange = days)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkUiState())

    fun selectRange(daysBack: Int) { _daysBack.value = daysBack }
}
