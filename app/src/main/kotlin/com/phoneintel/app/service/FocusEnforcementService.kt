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
 * When a blocked app is detected, launches FocusBlockedActivity on top of it.
 * Note: for maximum reliability, the user should grant "Display over other apps"
 * (SYSTEM_ALERT_WINDOW) in device settings. The feature degrades gracefully to
 * notification-only without it.
 */
@AndroidEntryPoint
class FocusEnforcementService : Service() {

    @Inject lateinit var focusRepository: FocusRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastBlockedPkg: String? = null

    // Guard against onStartCommand being called multiple times due to START_STICKY.
    // Without this, each restart would spawn a duplicate polling loop and collect.
    private val started = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildFocusNotification())

        if (started.compareAndSet(false, true)) {
            // Observe focus state — stop service if focus is disabled externally
            scope.launch {
                focusRepository.focusState.collect { state ->
                    if (!state.isActive) {
                        stopSelf()
                    } else {
                        // Refresh ongoing notification with current intent label
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
                    val foregroundPkg = detectForegroundApp(usm)
                    if (foregroundPkg != null && foregroundPkg in blocked &&
                        foregroundPkg != packageName && foregroundPkg != lastBlockedPkg) {
                        lastBlockedPkg = foregroundPkg
                        launchBlockScreen(foregroundPkg)
                    } else if (foregroundPkg != null && foregroundPkg !in blocked) {
                        lastBlockedPkg = null
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun detectForegroundApp(usm: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val events = runCatching { usm.queryEvents(now - 2_000, now) }.getOrNull() ?: return null
        val event = UsageEvents.Event()
        var lastFg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastFg = event.packageName
            }
        }
        return lastFg
    }

    private fun launchBlockScreen(blockedPackage: String) {
        runCatching {
            val i = Intent(this, FocusBlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(FocusBlockedActivity.EXTRA_BLOCKED_PKG, blockedPackage)
            }
            startActivity(i)
        }
        // Always also post a heads-up notification as a reliable fallback
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
