package akihz.anlaki.dev.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import akihz.anlaki.dev.R
import kotlin.math.roundToInt

/**
 * Shows short-lived status notices for Quick Settings tile actions.
 *
 * On Stock Android the tile's subtitle is visible after reopening QS,
 * but while QS is collapsed the user sees nothing. VPN apps show a
 * heads-up notification ("Connecting…") that remains visible after the
 * panel closes. This helper mirrors that behavior and falls back to
 * Toast when notifications are blocked (MIUI or permission denied).
 */
object TileFeedbackNotification {
    private const val CHANNEL_ID = "akihz_tile_feedback"
    private const val NOTIFICATION_ID = 1002

    fun showConnecting(context: Context) {
        show(context, "akiHz", "Connecting...")
    }

    fun showSwitching(context: Context, rate: Float) {
        show(context, "akiHz", "Switching to ${rate.roundToInt()} Hz...")
    }

    fun showSwitched(context: Context, rate: Float) {
        show(context, "akiHz", "Switched to ${rate.roundToInt()} Hz")
    }

    fun showError(context: Context, message: String) {
        show(context, "akiHz", message)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun show(context: Context, title: String, text: String) {
        createChannel(context)
        if (!canShowNotification(context)) {
            // Guaranteed visible even when notifications are blocked or on MIUI where
            // tile subtitles are not displayed.
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_refresh_rate)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setTimeoutAfter(2500)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun canShowNotification(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tile feedback",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows brief notices when the Quick Settings tile switches refresh rates"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
