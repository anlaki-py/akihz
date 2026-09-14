package akihz.anlaki.dev.presentation.fps

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import akihz.anlaki.dev.R
import akihz.anlaki.dev.presentation.MainActivity
import akihz.anlaki.dev.presentation.notifications.NotificationChannels

/** Notification for the FPS monitor foreground service. */
object FpsMonitorNotification {
    const val NOTIFICATION_ID = 1002
    private const val CHANNEL_ID = "akihz_fps_monitor"

    fun createChannel(context: Context) {
        NotificationChannels.ensure(
            context,
            CHANNEL_ID,
            "FPS monitoring",
            "Shows real-time FPS from SurfaceFlinger while another app is foreground",
            NotificationManager.IMPORTANCE_LOW
        ) {
            setShowBadge(false)
        }
    }

    fun build(context: Context, contentText: String = "Monitoring the foreground app"): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("akihz.extra.DEBUG_PAGE", "fps_monitor")
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            context,
            3,
            Intent(context, FpsMonitorService::class.java).apply { action = FpsMonitorService.ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_refresh_rate)
            .setContentTitle("FPS Monitor")
            .setContentText(contentText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(NotificationCompat.Action.Builder(null, "Stop", stopIntent).build())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .build()
    }
}
