package com.phoneintel.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.phoneintel.app.domain.model.AppUsageStat
import com.phoneintel.app.domain.model.DailyUsageSummary
import com.phoneintel.app.domain.model.FocusState
import com.phoneintel.app.ui.Screen
import com.phoneintel.app.ui.components.*
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAllApps by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PhoneIntel", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Your digital wellbeing", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Recap.route) }) {
                        Icon(Icons.Outlined.AutoAwesome, "Year Recap")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {

            // ── Phone Health Score hero ──────────────────────────────────────
            item {
                HealthScoreCard(
                    score = state.healthScore,
                    grade = state.healthGrade,
                    screenTimeMs = state.todayScreenTimeMs,
                    batteryLevel = state.batteryLevel,
                    isCharging = state.isCharging,
                    onClick = { navController.navigate(Screen.PhoneHealth.route) }
                )
            }

            // ── Active focus banner (shown when focus is on) ─────────────────
            if (state.focusState.isActive) {
                item { ActiveFocusBanner(state.focusState) { navController.navigate(Screen.Focus.route) } }
            }

            // ── Attention stats ──────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Attention",
                    action = "Details",
                    onAction = { navController.navigate(Screen.PhoneHealth.route) }
                )
                AttentionRow(
                    sessionCount = state.sessionCount,
                    avgSessionMs = state.avgSessionMs,
                    longestSessionMs = state.longestSessionMs,
                    shortCount = state.shortSessionCount,
                    fragmentationIndex = state.fragmentationIndex
                )
            }

            // ── Quick actions (Health + Focus) ───────────────────────────────
            item {
                SectionHeader(title = "Tools")
                QuickActionsRow(navController)
            }

            // ── Weekly trend ─────────────────────────────────────────────────
            item { SectionHeader(title = "This Week") }
            item { WeeklyTrendBar(state.weeklyTrend) }

            // ── Top apps today ───────────────────────────────────────────────
            if (state.topApps.isNotEmpty()) {
                val visible = if (showAllApps) state.topApps else state.topApps.take(5)
                item {
                    SectionHeader(
                        title = "Today's Top Apps",
                        action = if (state.topApps.size > 5)
                            if (showAllApps) "Show less" else "See all (${state.topApps.size})"
                        else null,
                        onAction = { showAllApps = !showAllApps }
                    )
                }
                items(visible) { app ->
                    AppUsageRow(app, state.topApps.firstOrNull()?.totalForegroundMs ?: 1L)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─── Health Score Card ────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(
    score: Int, grade: String,
    screenTimeMs: Long, batteryLevel: Int, isCharging: Boolean,
    onClick: () -> Unit
) {
    val gradeColor = when (grade) {
        "Excellent" -> ChartGreen
        "Good"      -> TealAccent
        "Fair"      -> ChartAmber
        else        -> CoralAccent
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1A237E))))
                .padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Score arc
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                    val animScore by animateFloatAsState(
                        targetValue = score / 100f,
                        animationSpec = tween(1000), label = "healthArc"
                    )
                    Canvas(Modifier.size(88.dp)) {
                        val stroke = Stroke(10.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(Color.White.copy(0.12f), 135f, 270f, false, style = stroke)
                        drawArc(gradeColor, 135f, 270f * animScore, false, style = stroke)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (score > 0) score.toString() else "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color.White, fontSize = 26.sp)
                        Text(grade.take(4), style = MaterialTheme.typography.labelSmall,
                            color = gradeColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("Phone Health", style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(0.6f))
                    Spacer(Modifier.height(4.dp))
                    Text(DateUtil.formatDuration(screenTimeMs) + " screen time",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap for full breakdown", style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.45f))
                }
                // Battery
                Column(horizontalAlignment = Alignment.End) {
                    CircularProgressRing(
                        progress = batteryLevel / 100f, size = 44.dp, strokeWidth = 4.dp,
                        trackColor = Color.White.copy(0.15f),
                        progressColor = if (isCharging) ChartAmber else TealAccent
                    ) {
                        Text("$batteryLevel%", style = MaterialTheme.typography.labelSmall,
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                    if (isCharging) {
                        Icon(Icons.Default.Bolt, null, tint = ChartAmber,
                            modifier = Modifier.size(12.dp).padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

// ─── Active Focus Banner ──────────────────────────────────────────────────────

@Composable
private fun ActiveFocusBanner(focusState: FocusState, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoBase.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoBase.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(focusState.intent.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${focusState.intent.label} focus active",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = IndigoLight)
                Text("${focusState.blockedPackages.size} apps blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = IndigoLight)
        }
    }
}

// ─── Attention Row ────────────────────────────────────────────────────────────

@Composable
private fun AttentionRow(
    sessionCount: Int, avgSessionMs: Long, longestSessionMs: Long,
    shortCount: Int, fragmentationIndex: Float
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniStatCard("Unlocks", sessionCount.toString(), Icons.Default.TouchApp,
            TealAccent, Modifier.weight(1f))
        MiniStatCard("Avg Session", DateUtil.formatDuration(avgSessionMs),
            Icons.Default.Timer, IndigoBase, Modifier.weight(1f))
        val fragPct = (fragmentationIndex * 100).toInt()
        val fragColor = when {
            fragPct < 33 -> ChartGreen
            fragPct < 66 -> ChartAmber
            else         -> CoralAccent
        }
        MiniStatCard("Fragmented", "$fragPct%", Icons.Default.ScatterPlot,
            fragColor, Modifier.weight(1f))
    }
}

@Composable
private fun MiniStatCard(
    label: String, value: String, icon: ImageVector,
    color: Color, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

// ─── Quick Actions ────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(navController: NavController) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionCard("Phone Health", Icons.Default.FavoriteBorder, IndigoBase,
            Modifier.weight(1f)) { navController.navigate(Screen.PhoneHealth.route) }
        ActionCard("Focus Mode", Icons.Default.DoNotDisturb, TealAccent,
            Modifier.weight(1f)) { navController.navigate(Screen.Focus.route) }
        ActionCard("Notifications", Icons.Outlined.Notifications, ChartPurple,
            Modifier.weight(1f)) { navController.navigate(Screen.Notifications.route) }
        ActionCard("Network", Icons.Outlined.NetworkWifi, ChartGreen,
            Modifier.weight(1f)) { navController.navigate(Screen.Network.route) }
    }
}

@Composable
private fun ActionCard(
    label: String, icon: ImageVector, color: Color,
    modifier: Modifier, onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Weekly Trend ─────────────────────────────────────────────────────────────

@Composable
private fun WeeklyTrendBar(trend: List<DailyUsageSummary>) {
    val maxMs = trend.maxOfOrNull { it.totalScreenTimeMs }?.takeIf { it > 0 } ?: 1L
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        trend.forEach { day ->
            val frac = day.totalScreenTimeMs / maxMs.toFloat()
            val isToday = DateUtil.isToday(day.date)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 3.dp)
                    .height((60 * frac + 4).dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(if (isToday) IndigoBase else MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.height(4.dp))
                Text(DateUtil.formatDate(day.date, "EEE").take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) IndigoBase else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

// ─── App Usage Row ────────────────────────────────────────────────────────────

@Composable
private fun AppUsageRow(app: AppUsageStat, maxMs: Long) {
    val frac = (app.totalForegroundMs / maxMs.toFloat()).coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Android, null, tint = IndigoBase, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(DateUtil.formatDuration(app.totalForegroundMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(IndigoBase))
            }
        }
    }
}
