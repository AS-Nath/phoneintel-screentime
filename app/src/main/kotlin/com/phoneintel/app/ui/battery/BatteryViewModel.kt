package com.phoneintel.app.ui.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.BatteryRepository
import com.phoneintel.app.domain.model.BatterySnapshot
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class BatteryUiState(
    val currentLevel: Int = 0,
    val isCharging: Boolean = false,
    val chargeType: String? = null,
    val avgLevel: Float = 0f,
    val snapshots: List<BatterySnapshot> = emptyList()
)

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val repository: BatteryRepository
) : ViewModel() {
    val uiState: StateFlow<BatteryUiState> = combine(
        repository.observeLatestSnapshot(),
        repository.observeSnapshots(DateUtil.daysAgo(0)),
        repository.observeAverageLevel(DateUtil.daysAgo(6))
    ) { latest, snapshots, avg ->
        BatteryUiState(
            currentLevel = latest?.level ?: 0,
            isCharging = latest?.isCharging ?: false,
            chargeType = latest?.chargeType,
            avgLevel = avg,
            snapshots = snapshots
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatteryUiState())
}
