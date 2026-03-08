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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.phoneintel.app.domain.model.InsightCard
import com.phoneintel.app.domain.model.InsightSeverity
import com.phoneintel.app.domain.model.InsightType
import com.phoneintel.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Insights", fontWeight = FontWeight.Bold)
                        Text(
                            "Personalised to your habits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.insights.isEmpty() -> {
                EmptyInsightsState(Modifier.padding(padding))
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            "Based on your last 7 days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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

@Composable
private fun InsightCardView(card: InsightCard, onAction: (String) -> Unit) {
    val (accentColor, icon) = insightStyle(card)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    card.headline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(2.dp)
                    .background(accentColor.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                card.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.5f
            )
            card.action?.let { action ->
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { onAction(action.route) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(0.5f)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        action.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInsightsState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Lightbulb, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("No insights yet", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Use the app for a few days and PhoneIntel will start recognising patterns in your habits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun insightStyle(card: InsightCard): Pair<Color, ImageVector> {
    val color = when (card.severity) {
        InsightSeverity.ALERT -> CoralAccent
        InsightSeverity.WARN  -> ChartAmber
        InsightSeverity.INFO  -> ChartGreen
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