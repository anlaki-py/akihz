package akihz.anlaki.dev.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import akihz.anlaki.dev.R
import akihz.anlaki.dev.presentation.UpdateInstallActivity

/** Posts user-visible update completion and failure notifications. */
object UpdateNotification {
    private const val CHANNEL_ID = "app_updates"
    private const val READY_NOTIFICATION_ID = 2001
    private const val FAILURE_NOTIFICATION_ID = 2002

    /** Notifies the user that a verified APK is ready for installation. */
    fun showReady(context: Context, downloadId: Long, versionName: String) {
        createChannel(context)
        val installIntent = Intent(context, UpdateInstallActivity::class.java)
            .putExtra(UpdateInstallActivity.EXTRA_DOWNLOAD_ID, downloadId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.hashCode(),
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_refresh_rate)
            .setContentTitle("akiHz $versionName is ready")
            .setContentText("Tap to install the verified update")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(READY_NOTIFICATION_ID, notification)
    }

    /** Notifies the user that the downloaded update could not be verified. */
    fun showFailure(context: Context, message: String) {
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_refresh_rate)
            .setContentTitle("akiHz update failed")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(FAILURE_NOTIFICATION_ID, notification)
    }

    /** Removes the ready notification once installation begins. */
    fun cancelReady(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(READY_NOTIFICATION_ID)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when an akiHz update is ready to install"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
