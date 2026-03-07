package com.phoneintel.app.ui.health

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.AttentionStats
import com.phoneintel.app.domain.model.PhoneHealthScore
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneHealthScreen(viewModel: PhoneHealthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Health", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Calculating your health score…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }

        val score = state.score
        val attention = state.attention

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                score?.let { HealthScoreHero(it) }
            }
            item {
                score?.let { ScoreBreakdown(it) }
            }
            item {
                attention?.let { AttentionSection(it) }
            }
            item {
                score?.let { WhatThisMeans(it) }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun HealthScoreHero(score: PhoneHealthScore) {
    val gradeColor = when (score.grade) {
        "Excellent" -> ChartGreen
        "Good"      -> TealAccent
        "Fair"      -> ChartAmber
        else        -> CoralAccent
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1A237E))))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Today's Phone Health", style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))

            // Animated score arc
            val animScore by animateFloatAsState(
                targetValue = score.score / 100f,
                animationSpec = tween(durationMillis = 1200), label = "score"
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(Modifier.size(160.dp)) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    val sweep = 270f
                    val start = 135f
                    // Track
                    drawArc(Color.White.copy(alpha = 0.12f), start, sweep, false, style = stroke)
                    // Fill
                    drawArc(gradeColor, start, sweep * animScore, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score.score.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Text(score.grade, style = MaterialTheme.typography.bodyMedium,
                        color = gradeColor, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(gradeMessage(score.grade),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ScoreBreakdown(score: PhoneHealthScore) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Score Breakdown", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))

        ScoreRow(
            icon = Icons.Default.PhoneAndroid, label = "Screen Time",
            subtitle = DateUtil.formatDuration(score.totalScreenTimeMs),
            score = score.screenTimeScore, maxScore = 30, color = IndigoBase
        )
        ScoreRow(
            icon = Icons.Default.Bedtime, label = "Night Usage",
            subtitle = if (score.nightUsageMs == 0L) "None" else DateUtil.formatDuration(score.nightUsageMs) + " after 11pm",
            score = score.nightUsageScore, maxScore = 20, color = ChartPurple
        )
        ScoreRow(
            icon = Icons.Default.TouchApp, label = "Unlock Frequency",
            subtitle = "${score.unlockCount} unlocks today",
            score = score.unlockScore, maxScore = 20, color = TealAccent
        )
        ScoreRow(
            icon = Icons.Default.Timer, label = "Longest Session",
            subtitle = if (score.longestSessionMs == 0L) "No sessions yet" else DateUtil.formatDuration(score.longestSessionMs),
            score = score.sessionScore, maxScore = 15, color = ChartAmber
        )
        ScoreRow(
            icon = Icons.Default.Notifications, label = "Notifications",
            subtitle = "${score.notificationCount} received today",
            score = score.notifScore, maxScore = 15, color = CoralAccent
        )
    }
}

@Composable
private fun ScoreRow(
    icon: ImageVector, label: String, subtitle: String,
    score: Int, maxScore: Int, color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { score / maxScore.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("$score/$maxScore", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun AttentionSection(stats: AttentionStats) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Attention Fragmentation", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))

        Text(
            "Attention fragmentation measures how broken up your phone use is. " +
            "Many short sessions signal reactive, compulsive checking rather than intentional use.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttentionStatCard("Sessions", stats.sessionCount.toString(), Icons.Default.RepeatOne,
                TealAccent, Modifier.weight(1f))
            AttentionStatCard("Avg Length", DateUtil.formatDuration(stats.avgSessionMs),
                Icons.Default.Timer, IndigoBase, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttentionStatCard("Longest", DateUtil.formatDuration(stats.longestSessionMs),
                Icons.Default.HourglassBottom, ChartGreen, Modifier.weight(1f))
            AttentionStatCard("Quick Checks", "${stats.shortSessionCount} <3min",
                Icons.Default.FlashOn, CoralAccent, Modifier.weight(1f))
        }

        // Fragmentation bar
        Spacer(Modifier.height(12.dp))
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fragmentation Index", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    val fragPct = (stats.fragmentationIndex * 100).toInt()
                    val fragColor = when {
                        fragPct < 33  -> ChartGreen
                        fragPct < 66  -> ChartAmber
                        else          -> CoralAccent
                    }
                    Text("$fragPct%", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = fragColor)
                }
                Spacer(Modifier.height(8.dp))
                val animFrag by animateFloatAsState(
                    targetValue = stats.fragmentationIndex,
                    animationSpec = tween(800), label = "frag"
                )
                LinearProgressIndicator(
                    progress = { animFrag },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = when {
                        stats.fragmentationIndex < 0.33f -> ChartGreen
                        stats.fragmentationIndex < 0.66f -> ChartAmber
                        else -> CoralAccent
                    },
                    trackColor = MaterialTheme.colorScheme.background
                )
                Spacer(Modifier.height(8.dp))
                Text(fragmentationMessage(stats), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AttentionStatCard(
    label: String, value: String, icon: ImageVector,
    color: Color, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WhatThisMeans(score: PhoneHealthScore) {
    Card(
        Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("💡 What This Means", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val tips = buildTips(score)
            tips.forEach { tip ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Text("•  ", style = MaterialTheme.typography.bodySmall, color = IndigoLight)
                    Text(tip, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                }
            }
        }
    }
}

private fun gradeMessage(grade: String) = when (grade) {
    "Excellent" -> "Your phone use is genuinely healthy today. Keep it up."
    "Good"      -> "Good balance overall. Small improvements can push you higher."
    "Fair"      -> "Your phone habits are getting in the way. Focus mode can help."
    else        -> "High phone activity is affecting your wellbeing. Try a focus session."
}

private fun fragmentationMessage(stats: AttentionStats) = when {
    stats.fragmentationIndex < 0.25f -> "Your attention is well-preserved today — mostly long, intentional sessions."
    stats.fragmentationIndex < 0.55f -> "Moderate fragmentation. Try grouping your phone use into deliberate windows."
    else -> "${stats.shortSessionCount} of your ${stats.sessionCount} sessions were under 3 minutes — likely compulsive checks."
}

private fun buildTips(score: PhoneHealthScore): List<String> {
    val tips = mutableListOf<String>()
    if (score.screenTimeScore < 20) tips += "Screen time above 4 hours is linked to increased anxiety and reduced focus."
    if (score.nightUsageScore < 10) tips += "Night-time phone use suppresses melatonin. Try enabling Sleep focus after 10pm."
    if (score.unlockScore < 10) tips += "Frequent unlocks often mean reactive checking. Try scheduling phone-free windows."
    if (score.sessionScore < 8) tips += "Long unbroken sessions strain attention. The Pomodoro technique (25min on, 5min off) helps."
    if (score.notifScore < 8) tips += "High notification volume fragments your focus. Consider disabling non-essential app notifications."
    if (tips.isEmpty()) tips += "You're doing great. Your phone use looks balanced and intentional today."
    return tips
}
