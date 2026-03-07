package com.phoneintel.app.ui.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.AppUsageStat
import com.phoneintel.app.domain.model.YearRecap
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapScreen(viewModel: RecapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("${state.year} Recap", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Building your ${state.year} recap…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }
        val recap = state.recap ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Not enough data yet. Keep using PhoneIntel through the year!")
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { RecapHeroBanner(recap) }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Your ${recap.year} Highlights", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))
            }
            item { RecapHighlightGrid(recap) }
            if (recap.topApps.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Most Used Apps", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(recap.topApps.mapIndexed { i, app -> i to app }) { (idx, app) ->
                    RecapAppRow(rank = idx + 1, app = app)
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                RecapFunFacts(recap)
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun RecapHeroBanner(recap: YearRecap) {
    Box(Modifier.fillMaxWidth().padding(16.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF6A1B9A), Color(0xFFAD1457))))
        .padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = AmberAccent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("${recap.year} in Review", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("You spent ${DateUtil.formatDuration(recap.totalScreenTimeMs)} on your phone this year",
                style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RecapHighlightGrid(recap: YearRecap) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecapHighlightCard(Icons.Default.PhoneAndroid, "Total Screen Time", DateUtil.formatDuration(recap.totalScreenTimeMs), IndigoBase, Modifier.weight(1f))
            RecapHighlightCard(Icons.Default.Notifications, "Notifications", recap.totalNotifications.toString(), TealAccent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecapHighlightCard(Icons.Default.Wifi, "Wi-Fi Used", DateUtil.formatBytes(recap.totalWifiBytes), ChartGreen, Modifier.weight(1f))
            RecapHighlightCard(Icons.Default.SignalCellularAlt, "Mobile Data", DateUtil.formatBytes(recap.totalMobileBytes), CoralAccent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecapHighlightCard(Icons.Default.Alarm, "Daily Average", DateUtil.formatDuration(recap.avgDailyScreenTimeMs), ChartAmber, Modifier.weight(1f))
            RecapHighlightCard(Icons.Default.LocalFireDepartment, "Longest Streak", "${recap.longestStreakDays} days", ChartCoral, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecapHighlightCard(icon: ImageVector, title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecapAppRow(rank: Int, app: AppUsageStat) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#$rank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = when (rank) { 1 -> AmberAccent; 2 -> Color(0xFFB0BEC5); 3 -> Color(0xFFCD7F32); else -> MaterialTheme.colorScheme.onSurfaceVariant },
            modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f)) {
            Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(DateUtil.formatDuration(app.totalForegroundMs), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecapFunFacts(recap: YearRecap) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text("Fun Facts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val days = recap.totalScreenTimeMs / 86_400_000
            FunFactRow("📱 You picked up your phone enough to fill $days full days of screen time.")
            recap.mostProductiveMonth?.let { FunFactRow("🌿 $it was your most productive month — lowest daily screen time.") }
            if (recap.bluetoothDeviceCount > 0) FunFactRow("🎧 You connected Bluetooth ${recap.bluetoothDeviceCount} times.")
            FunFactRow("🔋 Your phone charged ${recap.chargingCycles} times this year.")
        }
    }
}

@Composable
private fun FunFactRow(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 4.dp), lineHeight = 18.sp)
}
