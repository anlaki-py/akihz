package akihz.anlaki.dev.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Foreground service that keeps the app process at foreground priority.
 */
class KeepAliveService : Service() {

    /**
     * Creates the notification channel and promotes this service to foreground.
     */
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        val notification = NotificationHelper.buildNotification(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationConstants.KEEP_ALIVE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationConstants.KEEP_ALIVE_NOTIFICATION_ID, notification)
        }
    }

    /**
     * Requests system restart if the service is killed.
     *
     * @param intent start intent
     * @param flags start flags
     * @param startId service start ID
     * @return [START_STICKY] so Android can recreate the service
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    /**
     * Restarts the service when the task is removed from recents.
     *
     * @param rootIntent intent that launched the removed task
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        start(applicationContext)
    }

    /**
     * This service is not bound.
     *
     * @param intent bind intent
     * @return always `null`
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Attempts to restart when Android destroys the service normally.
     */
    override fun onDestroy() {
        super.onDestroy()
        start(applicationContext)
    }

    companion object {
        private const val TAG = "KeepAliveService"

        /**
         * Starts the foreground keep-alive service when Android allows it.
         *
         * @param context context used to start the service
         */
        fun start(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (exception: Exception) {
                Log.w(TAG, "Unable to start keep-alive service", exception)
            }
        }

        /**
         * Stops the foreground keep-alive service.
         *
         * @param context context used to stop the service
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}
