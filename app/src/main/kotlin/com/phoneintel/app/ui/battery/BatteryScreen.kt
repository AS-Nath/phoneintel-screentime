package com.phoneintel.app.ui.battery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.BatterySnapshot
import com.phoneintel.app.ui.components.*
import com.phoneintel.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(viewModel: BatteryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Battery", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(
                            if (state.currentLevel > 20) listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
                            else listOf(Color(0xFFB71C1C), Color(0xFFC62828))
                        )).padding(24.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(if (state.isCharging) "Charging" else "On Battery",
                                style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.7f))
                            Spacer(Modifier.height(4.dp))
                            Text("${state.currentLevel}%", style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            state.chargeType?.let {
                                Text("via $it", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                            }
                        }
                        Icon(
                            when {
                                state.isCharging -> Icons.Default.BatteryChargingFull
                                state.currentLevel > 80 -> Icons.Default.BatteryFull
                                state.currentLevel > 50 -> Icons.Default.Battery5Bar
                                state.currentLevel > 20 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.Battery1Bar
                            },
                            contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("7-Day Avg", "${state.avgLevel.toInt()}%", "average level",
                        icon = { Icon(Icons.Outlined.Analytics, null, tint = AmberAccent, modifier = Modifier.size(18.dp)) },
                        accentColor = AmberAccent, modifier = Modifier.weight(1f))
                    StatCard("Snapshots", state.snapshots.size.toString(), "today",
                        icon = { Icon(Icons.Outlined.Timeline, null, tint = TealAccent, modifier = Modifier.size(18.dp)) },
                        accentColor = TealAccent, modifier = Modifier.weight(1f))
                }
            }
            if (state.snapshots.isNotEmpty()) {
                item { SectionHeader(title = "Today's Battery Level") }
                item { BatteryTimeline(state.snapshots) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BatteryTimeline(snapshots: List<BatterySnapshot>) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom) {
                snapshots.takeLast(24).forEach { snap ->
                    val barColor = when {
                        snap.isCharging -> AmberAccent
                        snap.level > 50 -> ChartGreen
                        snap.level > 20 -> ChartAmber
                        else -> CoralAccent
                    }
                    Box(Modifier.weight(1f).padding(horizontal = 1.dp)
                        .fillMaxHeight(snap.level / 100f)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(barColor))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("24h ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
