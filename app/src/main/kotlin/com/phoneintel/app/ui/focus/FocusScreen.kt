package com.phoneintel.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
    val elapsed = remember(focusState.startedAt) {
        System.currentTimeMillis() - focusState.startedAt
    }

    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Big intent emoji
        Box(
            Modifier.size(100.dp).clip(CircleShape)
                .background(IndigoBase.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(focusState.intent.emoji, fontSize = 48.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("${focusState.intent.label} Focus", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Session started ${DateUtil.formatDate(focusState.startedAt, "h:mm a")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("${focusState.blockedPackages.size} apps blocked",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
        ) {
            Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("End Focus Session", fontWeight = FontWeight.SemiBold)
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
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("What are you focusing on?",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusIntent.values().forEach { intent ->
                        IntentChip(
                            intent = intent,
                            selected = state.selectedIntent == intent,
                            onClick = { onSelectIntent(intent) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Choose apps to block", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("${state.selectedPackages.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.isLoadingApps) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!state.isLoadingApps) {
            items(state.installedApps) { app ->
                AppPickerRow(
                    app = app,
                    isSelected = app.packageName in state.selectedPackages,
                    onToggle = { onToggleApp(app.packageName) }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStart,
                enabled = state.selectedPackages.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoBase)
            ) {
                Text(
                    "${state.selectedIntent.emoji}  Start ${state.selectedIntent.label} Focus",
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (state.selectedPackages.isEmpty()) {
                Text(
                    "Select at least one app to block",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IntentChip(
    intent: FocusIntent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) IndigoBase else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(intent.emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(intent.label, style = MaterialTheme.typography.labelSmall,
            color = fg, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AppPickerRow(app: InstalledApp, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = IndigoBase)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Android, null, tint = IndigoBase.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            app.appName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = IndigoBase, modifier = Modifier.size(16.dp))
        }
    }
}
