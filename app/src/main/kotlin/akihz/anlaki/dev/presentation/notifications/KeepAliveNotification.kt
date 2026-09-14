package akihz.anlaki.dev.presentation.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import akihz.anlaki.dev.R
import akihz.anlaki.dev.presentation.MainActivity

/** Creates the persistent notification used by [KeepAliveService]. */
object KeepAliveNotification {
    const val NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "akihz_keep_alive"

    /** Creates the low-importance notification channel for the foreground service. */
    fun createChannel(context: Context) {
        NotificationChannels.ensure(
            context,
            CHANNEL_ID,
            context.getString(R.string.keep_alive_channel_name),
            context.getString(R.string.keep_alive_notification_text),
            NotificationManager.IMPORTANCE_LOW
        ) {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
    }

    /** Builds the ongoing notification that opens [MainActivity] when tapped. */
    fun build(context: Context): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
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
