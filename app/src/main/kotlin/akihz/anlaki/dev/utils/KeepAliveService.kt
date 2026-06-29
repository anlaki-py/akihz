package akihz.anlaki.dev.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import timber.log.Timber

class KeepAliveService : Service() {

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        start(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        start(applicationContext)
    }

    companion object {
        private const val TAG = "KeepAliveService"

        fun start(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (exception: Exception) {
                Timber.w(exception, "Unable to start keep-alive service")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}
