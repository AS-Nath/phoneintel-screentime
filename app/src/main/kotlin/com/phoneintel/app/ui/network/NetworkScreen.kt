package com.phoneintel.app.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.AppNetworkUsage
import com.phoneintel.app.ui.components.*
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(viewModel: NetworkViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Network Usage", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Wi-Fi", DateUtil.formatBytes(state.totalWifi), "received + sent",
                        icon = { Icon(Icons.Outlined.Wifi, null, tint = ChartGreen, modifier = Modifier.size(18.dp)) },
                        accentColor = ChartGreen, modifier = Modifier.weight(1f))
                    StatCard("Mobile Data", DateUtil.formatBytes(state.totalMobile), "received + sent",
                        icon = { Icon(Icons.Outlined.SignalCellularAlt, null, tint = CoralAccent, modifier = Modifier.size(18.dp)) },
                        accentColor = CoralAccent, modifier = Modifier.weight(1f))
                }
            }
            item { NetworkSplitBar(state.totalWifi, state.totalMobile) }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Today" to 0, "7 Days" to 6, "30 Days" to 29).forEach { (label, days) ->
                        FilterChip(selected = state.selectedRange == days,
                            onClick = { viewModel.selectRange(days) },
                            label = { Text(label) })
                    }
                }
            }
            item { SectionHeader(title = "Apps by Data Usage") }
            if (state.topUsers.isEmpty()) {
                item { EmptyState("No network data", "Grant network access permissions to see usage") }
            } else {
                val max = state.topUsers.maxOfOrNull { it.totalBytes }?.toFloat() ?: 1f
                items(state.topUsers) { usage -> NetworkAppRow(usage, max) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NetworkSplitBar(wifi: Long, mobile: Long) {
    val total = wifi + mobile
    if (total == 0L) return
    val wifiFrac = wifi / total.toFloat()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
            Box(Modifier.fillMaxWidth(wifiFrac).fillMaxHeight().background(ChartGreen))
            Box(Modifier.fillMaxWidth().fillMaxHeight().background(CoralAccent))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(ChartGreen))
                Spacer(Modifier.width(4.dp))
                Text("Wi-Fi ${(wifiFrac * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(CoralAccent))
                Spacer(Modifier.width(4.dp))
                Text("Mobile ${((1 - wifiFrac) * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun NetworkAppRow(usage: AppNetworkUsage, maxBytes: Float) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(usage.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(DateUtil.formatBytes(usage.totalBytes), style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold, color = ChartGreen)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("↓ ${DateUtil.formatBytes(usage.bytesReceived)}", style = MaterialTheme.typography.labelSmall, color = TealAccent)
                Text("↑ ${DateUtil.formatBytes(usage.bytesSent)}", style = MaterialTheme.typography.labelSmall, color = ChartAmber)
            }
            Spacer(Modifier.height(6.dp))
            UsageBarRow("", "", usage.totalBytes / maxBytes, ChartGreen)
        }
    }
}
