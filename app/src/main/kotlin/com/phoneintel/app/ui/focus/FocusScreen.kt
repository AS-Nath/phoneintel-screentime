package com.phoneintel.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.FocusIntent
import com.phoneintel.app.domain.model.InstalledApp
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil

@Composable
fun FocusScreen(viewModel: FocusViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    // No Scaffold — MainActivity owns it
    if (state.focusState.isActive) {
        ActiveFocusView(
            focusState = state.focusState,
            onStop = { viewModel.stopFocus() }
        )
    } else {
        SetupFocusView(
            state = state,
            onSelectIntent = { viewModel.selectIntent(it) },
            onToggleApp = { viewModel.toggleApp(it) },
            onStart = { viewModel.startFocus() }
        )
    }
}

// ─── Active Focus View ────────────────────────────────────────────────────────

@Composable
private fun ActiveFocusView(
    focusState: com.phoneintel.app.domain.model.FocusState,
    onStop: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MintSubtle),
            contentAlignment = Alignment.Center
        ) {
            Text(focusState.intent.emoji, fontSize = 44.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "${focusState.intent.label} Focus",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Started at ${DateUtil.formatDate(focusState.startedAt, "h:mm a")}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${focusState.blockedPackages.size} apps blocked",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralAccent, contentColor = Color.White)
        ) {
            Text("End Focus Session", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

// ─── Setup Focus View ─────────────────────────────────────────────────────────

@Composable
private fun SetupFocusView(
    state: FocusUiState,
    onSelectIntent: (FocusIntent) -> Unit,
    onToggleApp: (String) -> Unit,
    onStart: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(state.installedApps, searchQuery) {
        if (searchQuery.isBlank()) state.installedApps
        else state.installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(Modifier.fillMaxSize()) {

        // ── Headline ─────────────────────────────────────────────────────
        item {
            Text(
                "What do you want\nto focus on?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 36.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Intent grid — 2x2 ────────────────────────────────────────────
        item {
            val intents = FocusIntent.values().toList()
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                intents.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { intent ->
                            IntentTile(
                                intent = intent,
                                selected = state.selectedIntent == intent,
                                onClick = { onSelectIntent(intent) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Start button — top, above app list ───────────────────────────
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Button(
                    onClick = onStart,
                    enabled = state.selectedPackages.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Mint,
                        contentColor = BgDeep,
                        disabledContainerColor = MintSubtle,
                        disabledContentColor = MintDim
                    )
                ) {
                    Text("Start Focusing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (state.selectedPackages.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Select at least one app to block",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Apps header ───────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CHOOSE APPS TO BLOCK",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp
                )
                if (!state.isLoadingApps) {
                    Text(
                        "${state.selectedPackages.size} selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Mint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Search bar ────────────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = TextDim, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Mint),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search apps…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDim
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, null,
                        tint = TextDim,
                        modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (state.isLoadingApps) {
                Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Mint, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ── App list ──────────────────────────────────────────────────────
        if (!state.isLoadingApps) {
            if (filteredApps.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim
                        )
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppToggleRow(
                        app = app,
                        isSelected = app.packageName in state.selectedPackages,
                        onToggle = { onToggleApp(app.packageName) }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ─── Intent Tile ──────────────────────────────────────────────────────────────

@Composable
private fun IntentTile(
    intent: FocusIntent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg     = if (selected) MintSubtle else BgCard
    val border = if (selected) Mint else TextDim.copy(alpha = 0.3f)
    val fg     = if (selected) Mint else TextSecondary

    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(intentIcon(intent), null, tint = fg, modifier = Modifier.size(18.dp))
        Text(
            intent.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

// ─── App Toggle Row ───────────────────────────────────────────────────────────

@Composable
private fun AppToggleRow(app: InstalledApp, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(BgCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Android, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            app.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgDeep,
                checkedTrackColor = Mint,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = BgCard,
                uncheckedBorderColor = TextDim.copy(alpha = 0.3f)
            )
        )
    }
}

private fun intentIcon(intent: FocusIntent): ImageVector = when (intent) {
    FocusIntent.STUDY  -> Icons.Outlined.School
    FocusIntent.WORK   -> Icons.Outlined.Work
    FocusIntent.SLEEP  -> Icons.Outlined.Bedtime
    FocusIntent.FAMILY -> Icons.Outlined.FamilyRestroom
    FocusIntent.CUSTOM -> Icons.Outlined.Tune
}