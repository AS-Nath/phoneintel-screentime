package com.phoneintel.app.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.NotificationStat
import com.phoneintel.app.ui.components.*
import com.phoneintel.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Today", state.totalToday.toString(), "notifications",
                        icon = { Icon(Icons.Default.Notifications, null, tint = TealAccent, modifier = Modifier.size(18.dp)) },
                        accentColor = TealAccent, modifier = Modifier.weight(1f))
                    StatCard("This Week", state.totalThisWeek.toString(), "notifications",
                        icon = { Icon(Icons.Default.Notifications, null, tint = ChartPurple, modifier = Modifier.size(18.dp)) },
                        accentColor = ChartPurple, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeRange.values().forEach { range ->
                        FilterChip(selected = state.selectedRange == range,
                            onClick = { viewModel.selectRange(range) },
                            label = { Text(range.label) })
                    }
                }
            }
            item { SectionHeader(title = "Top Notifiers") }
            if (state.topNotifiers.isEmpty()) {
                item {
                    EmptyState("No notifications tracked", "Grant notification access to start tracking",
                        icon = { Icon(Icons.Outlined.NotificationsNone, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) })
                }
            } else {
                val max = state.topNotifiers.maxOfOrNull { it.count }?.toFloat() ?: 1f
                items(state.topNotifiers) { stat -> NotificationStatRow(stat, max) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NotificationStatRow(stat: NotificationStat, maxCount: Float) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stat.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${stat.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TealAccent)
            }
            Spacer(Modifier.height(6.dp))
            UsageBarRow("", "", stat.count / maxCount, TealAccent)
        }
    }
}
