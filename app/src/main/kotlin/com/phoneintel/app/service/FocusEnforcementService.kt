package com.phoneintel.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.phoneintel.app.R
import com.phoneintel.app.data.repository.FocusRepository
import com.phoneintel.app.ui.focus.FocusBlockedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Runs while focus mode is active. Polls the foreground app every 1 second
 * using UsageEvents (requires PACKAGE_USAGE_STATS, already granted).
 *
 * Key design: we maintain a persistent `currentForegroundPkg` that is updated
 * from ALL UsageEvents since the service started — not just a 2-second window.
 * This fixes apps like Aaj Tak that fire MOVE_TO_FOREGROUND once on launch and
 * then go quiet, which caused the original 2-second window to miss them entirely.
 *
 * Note: for maximum reliability, the user should also grant "Display over other
 * apps" (SYSTEM_ALERT_WINDOW) in device settings. The feature degrades gracefully
 * to notification-only without it.
 */
@AndroidEntryPoint
class FocusEnforcementService : Service() {

    @Inject lateinit var focusRepository: FocusRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)

    // The last package we confirmed as foreground — persists across poll cycles
    private var currentForegroundPkg: String? = null
    // The last package we launched the block screen for — prevents re-firing every second
    private var lastBlockedPkg: String? = null
    // Timestamp from which we start scanning events — moves forward each poll
    private var eventScanFrom: Long = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildFocusNotification())

        if (started.compareAndSet(false, true)) {
            // Start scanning from now — no need to replay history
            eventScanFrom = System.currentTimeMillis() - 2_000L

            scope.launch {
                focusRepository.focusState.collect { state ->
                    if (!state.isActive) {
                        stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java)
                            .notify(NOTIFICATION_ID, buildFocusNotification())
                    }
                }
            }
            startPolling()
        }

        return START_STICKY
    }

    private fun startPolling() {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        scope.launch {
            while (isActive) {
                val blocked = focusRepository.getBlockedPackages()
                if (blocked.isNotEmpty()) {
                    // Update our persistent view of what's in the foreground
                    updateForegroundState(usm)

                    val pkg = currentForegroundPkg
                    when {
                        // Blocked app is in foreground and we haven't shown the screen yet
                        pkg != null && pkg in blocked && pkg != packageName && pkg != lastBlockedPkg -> {
                            lastBlockedPkg = pkg
                            launchBlockScreen(pkg)
                        }
                        // User navigated away from the blocked app — reset so we can block again
                        pkg != null && pkg !in blocked -> {
                            lastBlockedPkg = null
                        }
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Scans UsageEvents from [eventScanFrom] to now, updating [currentForegroundPkg].
     * Advances [eventScanFrom] to avoid replaying the same events on the next poll.
     *
     * Because we track ALL transitions (foreground + background), we always know the
     * current foreground app even if it hasn't fired an event recently.
     */
    private fun updateForegroundState(usm: UsageStatsManager) {
        val now = System.currentTimeMillis()
        val events = runCatching { usm.queryEvents(eventScanFrom, now) }.getOrNull() ?: return
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    currentForegroundPkg = event.packageName
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    // Only clear if this is still the current foreground app.
                    // A rapid app switch can produce FG(A) → FG(B) → BG(A) —
                    // clearing blindly would wrongly null out B.
                    if (currentForegroundPkg == event.packageName) {
                        currentForegroundPkg = null
                    }
                }
            }
        }

        // Advance the scan window — subtract a small overlap to avoid missing events
        // that land exactly on the boundary due to millisecond rounding.
        eventScanFrom = now - 200L
    }

    private fun launchBlockScreen(blockedPackage: String) {
        runCatching {
            val i = Intent(this, FocusBlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(FocusBlockedActivity.EXTRA_BLOCKED_PKG, blockedPackage)
            }
            startActivity(i)
        }
        postBlockedNotification(blockedPackage)
    }

    private fun postBlockedNotification(blockedPackage: String) {
        val tapIntent = PendingIntent.getActivity(
            this, NOTIFICATION_BLOCKED_ID,
            Intent(this, FocusBlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(FocusBlockedActivity.EXTRA_BLOCKED_PKG, blockedPackage)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, DataCollectionService.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_phoneintel)
            .setContentTitle("Focus mode is active")
            .setContentText("This app is blocked during your session")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(5_000)
            .setContentIntent(tapIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_BLOCKED_ID, notif)
    }

    private fun buildFocusNotification() = run {
        val state = focusRepository.focusState.value
        val label = if (state.isActive) "${state.intent.emoji} ${state.intent.label} focus" else "Focus"
        NotificationCompat.Builder(this, DataCollectionService.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_phoneintel)
            .setContentTitle(label)
            .setContentText("${state.blockedPackages.size} apps blocked")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val NOTIFICATION_BLOCKED_ID = 2002
        private const val POLL_INTERVAL_MS = 1_000L
    }
}