package com.phoneintel.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.phoneintel.app.data.db.entities.NotificationEventEntity
import com.phoneintel.app.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerService : NotificationListenerService() {

    @Inject lateinit var repository: NotificationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // App name cache — built once on IO thread, refreshed if a package is missing.
    // Avoids calling getInstalledApplications() on the main thread on every notification.
    @Volatile private var appNameCache: Map<String, String> = emptyMap()
    @Volatile private var cacheBuilt = false

    // Tracks ongoing notification keys already recorded this session.
    // Ongoing notifications (Internet Speed Meter, music players, etc.) re-post
    // constantly with updated content — we only want to count them once.
    private val seenOngoingKeys = mutableSetOf<String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Build the cache eagerly on IO thread when the listener connects,
        // so it's ready before the first notification arrives.
        scope.launch {
            buildCache()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        if (sbn.isOngoing) {
            val key = "${packageName}:${sbn.id}"
            if (!seenOngoingKeys.add(key)) return
        }

        val category = sbn.notification.category
        val isOngoing = sbn.isOngoing

        scope.launch {
            // Resolve app name fully on IO thread — never on main thread
            val appName = resolveAppNameAsync(packageName)
            repository.insertEvent(
                NotificationEventEntity(
                    packageName = packageName,
                    appName = appName,
                    timestamp = System.currentTimeMillis(),
                    title = null, // Privacy: never store notification content
                    category = category,
                    isOngoing = isOngoing
                )
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.isOngoing) {
            seenOngoingKeys.remove("${sbn.packageName}:${sbn.id}")
        }
    }

    private suspend fun buildCache() = withContext(Dispatchers.IO) {
        val pm = packageManager
        appNameCache = pm.getInstalledApplications(0).associate { appInfo ->
            val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
                .getOrDefault(appInfo.packageName)
            appInfo.packageName to label
        }
        cacheBuilt = true
    }

    private suspend fun resolveAppNameAsync(packageName: String): String {
        if (!cacheBuilt) buildCache()
        val cached = appNameCache[packageName]
        if (cached != null && cached.isNotBlank() && cached != packageName) return cached

        // Package not in cache — new install. Rebuild cache and try again.
        buildCache()
        val refreshed = appNameCache[packageName]
        if (refreshed != null && refreshed.isNotBlank() && refreshed != packageName) return refreshed

        return packageName // honest fallback
    }
}
