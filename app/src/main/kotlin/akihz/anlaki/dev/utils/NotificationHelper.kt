package akihz.anlaki.dev.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import akihz.anlaki.dev.R
import akihz.anlaki.dev.presentation.MainActivity

/**
 * Creates the persistent notification used by [KeepAliveService].
 */
object NotificationHelper {

    /**
     * Creates the keep-alive notification channel on Android 8+.
     *
     * @param context context used to access [NotificationManager]
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            NotificationConstants.KEEP_ALIVE_CHANNEL_ID,
            context.getString(R.string.keep_alive_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.keep_alive_notification_text)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds the ongoing foreground service notification.
     *
     * @param context context used to resolve strings and app launch intent
     * @return notification passed to [android.app.Service.startForeground]
     */
    fun buildNotification(context: Context): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, NotificationConstants.KEEP_ALIVE_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.keep_alive_notification_title))
            .setContentText(context.getString(R.string.keep_alive_notification_text))
            .setSmallIcon(R.drawable.ic_refresh_rate)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .build()
    }
}
