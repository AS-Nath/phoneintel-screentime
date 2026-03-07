package com.phoneintel.app.ui.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.BluetoothRepository
import com.phoneintel.app.domain.model.BluetoothDevice
import com.phoneintel.app.domain.model.BluetoothEvent
import com.phoneintel.app.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class BluetoothUiState(
    val knownDevices: List<BluetoothDevice> = emptyList(),
    val recentEvents: List<BluetoothEvent> = emptyList()
)

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val repository: BluetoothRepository
) : ViewModel() {
    val uiState: StateFlow<BluetoothUiState> = combine(
        repository.observeKnownDevices(),
        repository.observeRecentEvents(DateUtil.daysAgo(6))
    ) { devices, events ->
        BluetoothUiState(knownDevices = devices, recentEvents = events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothUiState())
}
