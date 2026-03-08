package com.phoneintel.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.phoneintel.app.domain.model.InsightCard
import com.phoneintel.app.domain.model.InsightSeverity
import com.phoneintel.app.domain.model.InsightType
import com.phoneintel.app.ui.theme.*
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgBase,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, null, tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBase)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Mint, strokeWidth = 2.dp)
                }
            }
            state.insights.isEmpty() -> {
                EmptyState(Modifier.padding(padding))
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Figma header: "INSIGHTS / Your Week" in large type
                    item { InsightsHeader() }

                    // Personality card — derived from top insight type
                    item { PersonalityCard(state.insights) }

                    // Attention leak
                    item { AttentionLeakCard(state.insights) }

                    // All insight cards
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "THIS WEEK",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(state.insights) { card ->
                        InsightCardView(card = card) { route -> navController.navigate(route) }
                    }
                }
            }
        }
    }
}

// ─── Insights Header ──────────────────────────────────────────────────────────
// Matches Figma: "INSIGHTS" label + "Your Week" large headline

@Composable
private fun InsightsHeader() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            "INSIGHTS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 3.sp
        )
        Text(
            "Your Week",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 40.sp
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ─── Personality Card ─────────────────────────────────────────────────────────
// Matches Figma: "Digital Personality" label + two-word bold/italic label

@Composable
private fun PersonalityCard(insights: List<InsightCard>) {
    val (adjective, noun) = derivePersonality(insights)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .padding(20.dp)
    ) {
        Text(
            "Digital Personality",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)) {
                    append("$adjective\n")
                }
                withStyle(SpanStyle(color = Mint, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 28.sp)) {
                    append(noun)
                }
            },
            lineHeight = 32.sp
        )
    }
    Spacer(Modifier.height(8.dp))
}

// ─── Attention Leak Card ──────────────────────────────────────────────────────
// Matches Figma: "Biggest Attention Leak" + two stacked app names

@Composable
private fun AttentionLeakCard(insights: List<InsightCard>) {
    val sinkInsight = insights.firstOrNull { it.type == InsightType.SINGLE_APP_SINK }
        ?: insights.firstOrNull { it.type == InsightType.NOTIFICATION_DRIVER }
        ?: return

    // Extract app name from headline — "X is absorbing your time" or "X sent..."
    val appName = sinkInsight.headline.substringBefore(" is").substringBefore(" sent")

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Biggest Attention Leak",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 26.sp
            )
            // Secondary app if notification driver also exists
            val notifInsight = insights.firstOrNull { it.type == InsightType.NOTIFICATION_DRIVER }
            if (notifInsight != null && sinkInsight.type != InsightType.NOTIFICATION_DRIVER) {
                val notifApp = notifInsight.body.substringAfter(". ").substringBefore(" sent")
                Text(
                    notifApp,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontSize = 22.sp
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

// ─── Insight Card ─────────────────────────────────────────────────────────────

@Composable
private fun InsightCardView(card: InsightCard, onAction: (String) -> Unit) {
    val (accentColor, icon) = insightStyle(card)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                card.headline,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Thin accent line
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(accentColor.copy(alpha = 0.2f))
        )

        Spacer(Modifier.height(12.dp))

        Text(
            card.body,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 19.sp
        )

        card.action?.let { action ->
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable(onClick = { onAction(action.route) })
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    action.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Lightbulb, null,
                tint = TextDim,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No insights yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Use the app for a little while and PhoneIntel will start recognising patterns in your habits.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun insightStyle(card: InsightCard): Pair<Color, ImageVector> {
    val color = when (card.severity) {
        InsightSeverity.ALERT -> CoralAccent
        InsightSeverity.WARN  -> AmberAccent
        InsightSeverity.INFO  -> Mint
    }
    val icon = when (card.type) {
        InsightType.NIGHT_HABIT         -> Icons.Default.Bedtime
        InsightType.SINGLE_APP_SINK     -> Icons.Default.Tsunami
        InsightType.COMPULSIVE_CHECKER  -> Icons.Default.RepeatOne
        InsightType.FRAGMENTATION_SPIKE -> Icons.Default.ScatterPlot
        InsightType.NOTIFICATION_DRIVER -> Icons.Default.NotificationsActive
        InsightType.IMPROVING           -> Icons.Default.TrendingDown
    }
    return color to icon
}

// Derives a "Digital Personality" label from the dominant insight pattern
private fun derivePersonality(insights: List<InsightCard>): Pair<String, String> {
    val types = insights.map { it.type }
    return when {
        InsightType.COMPULSIVE_CHECKER in types  -> "Reactive" to "Communicator"
        InsightType.SINGLE_APP_SINK in types     -> "Deep" to "Diver"
        InsightType.NIGHT_HABIT in types         -> "Night" to "Scroller"
        InsightType.FRAGMENTATION_SPIKE in types -> "Scattered" to "Browser"
        InsightType.IMPROVING in types           -> "Mindful" to "User"
        else                                     -> "Balanced" to "Thinker"
    }
}
