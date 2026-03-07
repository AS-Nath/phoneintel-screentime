package com.phoneintel.app.data.repository

import android.content.Context
import com.phoneintel.app.domain.model.FocusIntent
import com.phoneintel.app.domain.model.FocusState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)

    private val _focusState = MutableStateFlow(loadPersistedState())
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    fun startFocus(intent: FocusIntent, blockedPackages: Set<String>) {
        val now = System.currentTimeMillis()
        val state = FocusState(
            isActive = true,
            intent = intent,
            blockedPackages = blockedPackages,
            startedAt = now
        )
        persist(state)
        _focusState.value = state
    }

    fun stopFocus() {
        val state = FocusState(isActive = false)
        persist(state)
        _focusState.value = state
    }

    fun isFocusActive(): Boolean = _focusState.value.isActive
    fun getBlockedPackages(): Set<String> = _focusState.value.blockedPackages

    private fun persist(state: FocusState) {
        prefs.edit()
            .putBoolean("active", state.isActive)
            .putString("intent", state.intent.name)
            .putStringSet("blocked", state.blockedPackages)
            .putLong("started", state.startedAt)
            .apply()
    }

    private fun loadPersistedState(): FocusState {
        val active = prefs.getBoolean("active", false)
        val intentName = prefs.getString("intent", FocusIntent.WORK.name) ?: FocusIntent.WORK.name
        val blocked = prefs.getStringSet("blocked", emptySet()) ?: emptySet()
        val started = prefs.getLong("started", 0L)
        return FocusState(
            isActive = active,
            intent = runCatching { FocusIntent.valueOf(intentName) }.getOrDefault(FocusIntent.WORK),
            blockedPackages = blocked,
            startedAt = started
        )
    }
}
