package com.phoneintel.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneintel.app.domain.model.FocusIntent
import com.phoneintel.app.domain.model.InstalledApp
import com.phoneintel.app.ui.theme.*
import com.phoneintel.app.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: FocusViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgBase,
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBase)
            )
        }
    ) { padding ->
        if (state.focusState.isActive) {
            ActiveFocusView(
                focusState = state.focusState,
                onStop = { viewModel.stopFocus() },
                modifier = Modifier.padding(padding)
            )
        } else {
            SetupFocusView(
                state = state,
                onSelectIntent = { viewModel.selectIntent(it) },
                onToggleApp = { viewModel.toggleApp(it) },
                onStart = { viewModel.startFocus() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ─── Active Focus View ────────────────────────────────────────────────────────

@Composable
private fun ActiveFocusView(
    focusState: com.phoneintel.app.domain.model.FocusState,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MintSubtle),
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
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize()) {

        // Headline
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

        // Intent grid — 2x2 like Figma
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
                        // Pad odd row
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Apps to block header
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
                        "Select All",
                        style = MaterialTheme.typography.labelSmall,
                        color = Mint,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            // select all — no-op placeholder, could wire to VM
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.isLoadingApps) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Mint, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            }
        }

        // App list
        if (!state.isLoadingApps) {
            items(state.installedApps) { app ->
                AppToggleRow(
                    app = app,
                    isSelected = app.packageName in state.selectedPackages,
                    onToggle = { onToggleApp(app.packageName) }
                )
            }
        }

        // CTA
        item {
            Spacer(Modifier.height(20.dp))
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
                    Text(
                        "Start Focusing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                if (state.focusState.isActive.not() && state.selectedPackages.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Select at least one app to block",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                // Timer hint from Figma
                Spacer(Modifier.height(10.dp))
                Text(
                    "Ends in 45 minutes at 14:30",  // TODO: wire to actual timer
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Intent Tile ─────────────────────────────────────────────────────────────
// Matches Figma: outlined tile, selected = filled with subtle mint tint

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
    val icon   = intentIcon(intent)

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
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(
            intent.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

// ─── App Toggle Row ───────────────────────────────────────────────────────────
// Matches Figma: app icon + name + toggle switch on right

@Composable
private fun AppToggleRow(app: InstalledApp, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon placeholder
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Android, null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
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