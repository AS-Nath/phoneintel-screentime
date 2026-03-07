package com.phoneintel.app.ui.bluetooth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.BluetoothDevice
import com.phoneintel.app.domain.model.BluetoothEvent
import com.phoneintel.app.domain.model.BluetoothEventType
import com.phoneintel.app.ui.components.*
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(viewModel: BluetoothViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bluetooth", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Known Devices", state.knownDevices.size.toString(),
                        icon = { Icon(Icons.Default.Bluetooth, null, tint = IndigoLight, modifier = Modifier.size(18.dp)) },
                        accentColor = IndigoLight, modifier = Modifier.weight(1f))
                    StatCard("Events (7d)", state.recentEvents.size.toString(),
                        icon = { Icon(Icons.Default.BluetoothConnected, null, tint = TealAccent, modifier = Modifier.size(18.dp)) },
                        accentColor = TealAccent, modifier = Modifier.weight(1f))
                }
            }
            item { SectionHeader(title = "Known Devices") }
            if (state.knownDevices.isEmpty()) {
                item {
                    EmptyState("No Bluetooth devices", "Connect a device to see it tracked here",
                        icon = { Icon(Icons.Outlined.BluetoothDisabled, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) })
                }
            } else {
                items(state.knownDevices) { BluetoothDeviceRow(it) }
            }
            if (state.recentEvents.isNotEmpty()) {
                item { SectionHeader(title = "Recent Events") }
                items(state.recentEvents.take(20)) { BluetoothEventRow(it) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BluetoothDeviceRow(device: BluetoothDevice) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BluetoothConnected, null, tint = IndigoBase, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name.ifBlank { "Unknown Device" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(device.deviceClass, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(device.lastSeen)),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (device.totalConnectionMs > 0) {
                    Text(DateUtil.formatDuration(device.totalConnectionMs), style = MaterialTheme.typography.labelSmall, color = TealAccent)
                }
            }
        }
    }
}

@Composable
private fun BluetoothEventRow(event: BluetoothEvent) {
    val isConnect = event.eventType == BluetoothEventType.CONNECTED
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isConnect) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled, null,
            tint = if (isConnect) TealAccent else CoralAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("${event.deviceName} — ${event.eventType.name.lowercase().replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
