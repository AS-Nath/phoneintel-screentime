package com.phoneintel.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.phoneintel.app.domain.model.AppUsageStat
import com.phoneintel.app.domain.model.DailyUsageSummary
import com.phoneintel.app.domain.model.InsightCard
import com.phoneintel.app.domain.model.InsightSeverity
import com.phoneintel.app.ui.Screen
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // No Scaffold here — MainActivity owns the Scaffold and bottom nav
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {

        // ── Top bar ──────────────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PHONEINTEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh, null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Hero headline ────────────────────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Your Phone",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 38.sp
                )
                Text(
                    "Today",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 38.sp
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        // ── Health score ─────────────────────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "PHONE HEALTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (state.healthScore > 0) state.healthScore.toString() else "—",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Mint,
                        fontSize = 64.sp
                    )
                    Column(Modifier.padding(bottom = 10.dp)) {
                        Text(
                            state.healthGrade,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            "Top 15% of users",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Insight strip ────────────────────────────────────────────────
        val topInsight = state.topInsight
        if (topInsight != null) {
            item {
                InsightStrip(
                    insight = topInsight,
                    onClick = { navController.navigate(Screen.Insights.route) }
                )
            }
        }

        // ── Stats row ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill(
                    label = "FRAGMENTATION",
                    value = when {
                        state.fragmentationIndex < 0.33f -> "Low"
                        state.fragmentationIndex < 0.66f -> "Medium"
                        else -> "High"
                    },
                    valueColor = when {
                        state.fragmentationIndex < 0.33f -> Mint
                        state.fragmentationIndex < 0.66f -> AmberAccent
                        else -> CoralAccent
                    },
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "AVG SESSION",
                    value = DateUtil.formatDuration(state.avgSessionMs),
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Active focus banner ──────────────────────────────────────────
        if (state.focusState.isActive) {
            item {
                ActiveFocusBanner(state.focusState) {
                    navController.navigate(Screen.Focus.route)
                }
            }
        }

        // ── Weekly trend ─────────────────────────────────────────────────
        item {
            SectionLabel("THIS WEEK")
            WeeklyBars(state.weeklyTrend)
            Spacer(Modifier.height(20.dp))
        }

        // ── Top apps ─────────────────────────────────────────────────────
        if (state.topApps.isNotEmpty()) {
            item { SectionLabel("TODAY'S TOP APPS") }
            items(state.topApps.take(5)) { app ->
                AppRow(app, state.topApps.firstOrNull()?.totalForegroundMs ?: 1L)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─── Insight Strip ────────────────────────────────────────────────────────────

@Composable
private fun InsightStrip(insight: InsightCard, onClick: () -> Unit) {
    val accentColor = when (insight.severity) {
        InsightSeverity.ALERT -> CoralAccent
        InsightSeverity.WARN  -> AmberAccent
        InsightSeverity.INFO  -> Mint
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "INSIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextDim)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    val parts = insight.body.split("between")
                    if (parts.size > 1) {
                        append("You were ")
                        withStyle(SpanStyle(color = accentColor, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)) {
                            append("most distracted")
                        }
                        append(" between")
                        append(parts[1].substringBefore(".").trim().let { " $it." })
                    } else {
                        append(insight.headline)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight, null,
                tint = TextDim,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Stat Pill ────────────────────────────────────────────────────────────────

@Composable
private fun StatPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.sp,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// ─── Active Focus Banner ──────────────────────────────────────────────────────

@Composable
private fun ActiveFocusBanner(
    focusState: com.phoneintel.app.domain.model.FocusState,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MintSubtle)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(focusState.intent.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${focusState.intent.label} focus active",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Mint
                )
                Text(
                    "${focusState.blockedPackages.size} apps blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MintDim
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MintDim, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

// ─── Weekly Bars ──────────────────────────────────────────────────────────────

@Composable
private fun WeeklyBars(trend: List<DailyUsageSummary>) {
    val maxMs = trend.maxOfOrNull { it.totalScreenTimeMs }?.takeIf { it > 0 } ?: 1L
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        trend.forEach { day ->
            val frac = (day.totalScreenTimeMs / maxMs.toFloat()).coerceIn(0.03f, 1f)
            val isToday = DateUtil.isToday(day.date)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .height((56 * frac + 4).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (isToday) Mint else BgCard)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    DateUtil.formatDate(day.date, "EEE").take(1),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isToday) Mint else TextDim,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ─── App Row ──────────────────────────────────────────────────────────────────

@Composable
private fun AppRow(app: AppUsageStat, maxMs: Long) {
    val frac = (app.totalForegroundMs / maxMs.toFloat()).coerceIn(0f, 1f)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                DateUtil.formatDuration(app.totalForegroundMs),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BgCard)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Mint)
            )
        }
    }
}

// ─── Bottom Nav ───────────────────────────────────────────────────────────────

@Composable
fun PhoneIntelBottomNav(navController: NavController, currentRoute: String) {
    val items = listOf(
        Triple(Screen.Dashboard.route,     Icons.Default.Home,           "Home"),
        Triple(Screen.PhoneHealth.route,   Icons.Outlined.BarChart,      "Stats"),
        Triple(Screen.Notifications.route, Icons.Outlined.Notifications, "Alerts"),
        Triple(Screen.Insights.route,      Icons.Outlined.Lightbulb,     "Insights"),
    )
    NavigationBar(
        containerColor = BgDeep,
        tonalElevation = 0.dp
    ) {
        items.forEach { (route, icon, label) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, null, modifier = Modifier.size(22.dp)) },
                label = {
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Mint,
                    selectedTextColor = Mint,
                    unselectedIconColor = TextDim,
                    unselectedTextColor = TextDim,
                    indicatorColor = MintSubtle
                )
            )
        }
    }
}