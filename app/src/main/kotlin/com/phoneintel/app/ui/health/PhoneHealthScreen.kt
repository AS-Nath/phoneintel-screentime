package com.phoneintel.app.ui.health

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        containerColor = BgBase,
        topBar = {
            TopAppBar(
                title = { Text("Phone Health", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, null, tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBase)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Mint, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Calculating your health score…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { state.score?.let { ScoreHero(it) } }
            item { state.score?.let { StatsTable(it) } }
            item { state.attention?.let { AttentionSection(it) } }
            item { state.score?.let { TipsCard(it) } }
        }
    }
}

// ─── Score Hero ───────────────────────────────────────────────────────────────
// Matches the Figma: giant number centred, label below, one-liner summary

@Composable
private fun ScoreHero(score: PhoneHealthScore) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            score.score.toString(),
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = Mint,
            lineHeight = 88.sp
        )
        Text(
            "HEALTH SCORE",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                gradeMessage(score.grade),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ─── Stats Table ──────────────────────────────────────────────────────────────
// Matches the Figma: label left, value right, simple list — not cards

@Composable
private fun StatsTable(score: PhoneHealthScore) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
    ) {
        StatRow("SCREEN TIME", DateUtil.formatDuration(score.totalScreenTimeMs), isFirst = true)
        RowDivider()
        StatRow("SESSIONS", score.unlockCount.toString())
        RowDivider()
        StatRow("AVG LENGTH", DateUtil.formatDuration(score.longestSessionMs / score.unlockCount.coerceAtLeast(1).toLong()))
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun StatRow(label: String, value: String, isFirst: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(TextDim.copy(alpha = 0.3f))
    )
}

// ─── Attention Section ────────────────────────────────────────────────────────
// Matches Figma "Attention Fragmentation" screen: big label, status word, description

@Composable
private fun AttentionSection(stats: AttentionStats) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            "ATTENTION FRAGMENTATION",
            style = MaterialTheme.typography.labelSmall,
            color = Mint,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(8.dp))

        val fragLabel = when {
            stats.fragmentationIndex < 0.33f -> "Low"
            stats.fragmentationIndex < 0.66f -> "Medium"
            else -> "High"
        }
        val fragColor = when {
            stats.fragmentationIndex < 0.33f -> Mint
            stats.fragmentationIndex < 0.66f -> AmberAccent
            else -> CoralAccent
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                fragLabel,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = fragColor
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.CheckCircle, null, tint = fragColor, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text(
            fragDescription(stats),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(20.dp))

        // Two stat pills
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AttentionPill(
                label = "AVG SESSION",
                value = DateUtil.formatDuration(stats.avgSessionMs),
                modifier = Modifier.weight(1f)
            )
            AttentionPill(
                label = "QUICK CHECKS",
                value = stats.shortSessionCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // More stats
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgCard)
        ) {
            InlineRow(Icons.Default.WorkspacePremium, "Deep focus sessions",
                stats.let { s ->
                    val longSessions = (s.sessionCount - s.shortSessionCount).coerceAtLeast(0)
                    longSessions.toString()
                }
            )
            RowDivider()
            InlineRow(Icons.Default.SwapHoriz, "Context switching",
                when {
                    stats.fragmentationIndex < 0.25f -> "Minimal"
                    stats.fragmentationIndex < 0.55f -> "Moderate"
                    else -> "High"
                }
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AttentionPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .padding(16.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.sp,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun InlineRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

// ─── Tips Card ────────────────────────────────────────────────────────────────

@Composable
private fun TipsCard(score: PhoneHealthScore) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .padding(20.dp)
    ) {
        Text(
            "What this means",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        buildTips(score).forEach { tip ->
            Row(
                Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .padding(top = 7.dp)
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Mint)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun gradeMessage(grade: String) = when (grade) {
    "Excellent" -> "Your phone use is genuinely healthy today. Keep it up."
    "Good"      -> "Your attention today was mostly intentional."
    "Fair"      -> "Your phone habits are getting in the way. Focus mode can help."
    else        -> "High phone activity is affecting your wellbeing. Try a focus session."
}

private fun fragDescription(stats: AttentionStats) = when {
    stats.fragmentationIndex < 0.33f ->
        "Your usage today consisted of fewer, longer sessions. This suggests higher focus and less reactive behaviour."
    stats.fragmentationIndex < 0.66f ->
        "Your attention is moderately fragmented. Try grouping your phone use into deliberate windows."
    else ->
        "${stats.shortSessionCount} of your ${stats.sessionCount} sessions were under 3 minutes — likely compulsive checks."
}

private fun buildTips(score: PhoneHealthScore): List<String> {
    val tips = mutableListOf<String>()
    if (score.screenTimeScore < 20) tips += "Screen time above 4 hours is linked to increased anxiety and reduced focus."
    if (score.nightUsageScore < 10) tips += "Night-time phone use suppresses melatonin. Try enabling Sleep focus after 10pm."
    if (score.unlockScore < 10) tips += "Frequent unlocks often mean reactive checking. Try scheduling phone-free windows."
    if (score.sessionScore < 8) tips += "Long unbroken sessions strain attention. The Pomodoro technique helps."
    if (score.notifScore < 8) tips += "High notification volume fragments your focus. Disable non-essential alerts."
    if (tips.isEmpty()) tips += "You're doing great. Your phone use looks balanced and intentional today."
    return tips
}