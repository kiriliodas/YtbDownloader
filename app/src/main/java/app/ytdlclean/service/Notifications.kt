package app.ytdlclean.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.ytdlclean.MainActivity
import app.ytdlclean.R
import app.ytdlclean.domain.DownloadStatus
import app.ytdlclean.domain.DownloadTask

/** Builds + posts notifications for the download service. */
class Notifications(private val context: Context) {

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    /** Build the live foreground notification summarizing active downloads. */
    fun buildProgress(tasks: List<DownloadTask>): NotificationCompat.Builder {
        val active = tasks.filter { it.status == DownloadStatus.DOWNLOADING }
        val queued = tasks.count { it.status == DownloadStatus.QUEUED }

        val (title, percent, indeterminate) = when {
            active.isEmpty() -> Triple(
                if (queued > 0) "Waiting in queue…" else context.getString(R.string.notif_downloading),
                0, true
            )
            active.size == 1 -> {
                val t = active.first()
                Triple(
                    "${context.getString(R.string.notif_downloading)} · ${t.title.take(40)}",
                    (t.progress * 100).toInt().coerceIn(0, 100),
                    t.progress <= 0f,
                )
            }
            else -> Triple(
                "${context.getString(R.string.notif_downloading)} · ${active.size} items",
                (active.sumOf { it.progress.toDouble() } / active.size * 100).toInt().coerceIn(0, 100),
                false,
            )
        }

        val text = buildString {
            active.firstOrNull()?.let {
                if (it.etaText != null) append("ETA ${it.etaText}")
                if (it.speedText != null) {
                    if (isNotEmpty()) append("  ·  ")
                    append(it.speedText)
                }
            }
            if (queued > 0) {
                if (isNotEmpty()) append("  ·  ")
                append("$ queued")
            }
        }.ifBlank { null }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, indeterminate)
            .setContentIntent(contentIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    fun postComplete(title: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_complete))
            .setContentText(title.take(60))
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        manager.notify(System.currentTimeMillis().toInt(), n)
    }

    companion object {
        const val CHANNEL_ID = "downloads"
        const val FOREGROUND_ID = 1
    }
}
