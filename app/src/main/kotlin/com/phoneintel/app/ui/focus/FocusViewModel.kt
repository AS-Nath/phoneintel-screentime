package com.phoneintel.app.ui.focus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneintel.app.data.repository.FocusRepository
import com.phoneintel.app.domain.model.FocusIntent
import com.phoneintel.app.domain.model.FocusState
import com.phoneintel.app.domain.model.InstalledApp
import com.phoneintel.app.service.FocusEnforcementService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FocusUiState(
    val focusState: FocusState = FocusState(),
    val installedApps: List<InstalledApp> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val selectedIntent: FocusIntent = FocusIntent.WORK,
    val isLoadingApps: Boolean = true
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusRepository: FocusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedIntent = MutableStateFlow(FocusIntent.WORK)
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)

    val uiState: StateFlow<FocusUiState> = combine(
        focusRepository.focusState,
        _installedApps,
        _selectedPackages,
        _selectedIntent,
        _isLoadingApps
    ) { focusState, apps, selected, intent, loading ->
        FocusUiState(focusState, apps, selected, intent, loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    init {
        loadInstalledApps()
        // Sync picker state from active focus session if one exists
        val active = focusRepository.focusState.value
        if (active.isActive) {
            _selectedPackages.value = active.blockedPackages
            _selectedIntent.value = active.intent
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
                    .map { info ->
                        val pkg = info.activityInfo.packageName
                        val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                        val icon = runCatching { info.loadIcon(pm) }.getOrNull()
                        InstalledApp(pkg, label, icon)
                    }
                    .sortedBy { it.appName }
                    .distinctBy { it.packageName }
                    .filter { it.packageName != context.packageName }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun toggleApp(packageName: String) {
        _selectedPackages.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
    }

    fun selectIntent(intent: FocusIntent) {
        _selectedIntent.value = intent
        // Apply smart defaults for each intent type
        if (_selectedPackages.value.isEmpty()) {
            val defaults = defaultBlockList(intent)
            val matching = _installedApps.value
                .filter { app -> defaults.any { app.packageName.contains(it, ignoreCase = true) } }
                .map { it.packageName }
                .toSet()
            _selectedPackages.value = matching
        }
    }

    fun startFocus() {
        val intent = _selectedIntent.value
        val blocked = _selectedPackages.value
        focusRepository.startFocus(intent, blocked)
        context.startForegroundService(
            Intent(context, FocusEnforcementService::class.java)
        )
    }

    fun stopFocus() {
        focusRepository.stopFocus()
        context.stopService(Intent(context, FocusEnforcementService::class.java))
    }

    // Suggest package name fragments to block by default for each intent.
    // The actual packages are matched from the user's installed app list.
    private fun defaultBlockList(intent: FocusIntent): List<String> = when (intent) {
        FocusIntent.WORK   -> listOf("instagram", "tiktok", "youtube", "twitter", "facebook",
                                     "snapchat", "reddit", "netflix", "spotify", "games")
        FocusIntent.STUDY  -> listOf("instagram", "tiktok", "youtube", "twitter", "facebook",
                                     "snapchat", "reddit", "netflix", "games", "discord")
        FocusIntent.FAMILY -> listOf("slack", "teams", "gmail", "outlook", "linkedin",
                                     "zoom", "meet", "work", "office")
        FocusIntent.SLEEP  -> listOf("instagram", "tiktok", "youtube", "twitter", "facebook",
                                     "snapchat", "reddit", "netflix", "games", "news",
                                     "slack", "teams", "gmail", "discord")
        FocusIntent.CUSTOM -> emptyList()
    }
}
