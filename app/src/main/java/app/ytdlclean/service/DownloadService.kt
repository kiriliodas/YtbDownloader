package app.ytdlclean.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.ytdlclean.data.DownloadManager
import app.ytdlclean.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Foreground service whose sole job is to keep the app process alive (and show a
 * progress notification) while downloads are running. It does not execute downloads
 * itself — the [DownloadManager] owns the pipeline; this service just reflects its
 * state so Android doesn't kill us when the user backgrounds the app.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observer: Job? = null
    private lateinit var notifications: Notifications
    private lateinit var manager: DownloadManager

    override fun onCreate() {
        super.onCreate()
        manager = AppContainer.get(this)
        notifications = Notifications(this).also { it.ensureChannel() }
        startForegroundCompat(buildNotification())
        observeState()
    }

    private fun observeState() {
        observer = manager.repository.tasks
            .onEach { tasks -> updateNotification(tasks) }
            .launchIn(scope)
    }

    private fun updateNotification(tasks: List<app.ytdlclean.domain.DownloadTask>) {
        val active = tasks.count { it.isActive }
        if (active > 0) {
            val notification = buildNotification().build()
            androidx.core.app.NotificationManagerCompat.from(this)
                .notify(Notifications.FOREGROUND_ID, notification)
        } else {
            // Nothing active — stop the service; the manager has already finished.
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(): NotificationCompat.Builder =
        notifications.buildProgress(manager.repository.tasks.value)

    @Suppress("DEPRECATION")
    private fun startForegroundCompat(notification: NotificationCompat.Builder) {
        val built = notification.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Notifications.FOREGROUND_ID, built, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(Notifications.FOREGROUND_ID, built)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_NOT_STICKY: if killed, don't restart — the manager owns restart-on-retry.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observer?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, DownloadService::class.java)) }
        }
    }
}
