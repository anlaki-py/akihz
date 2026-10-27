package akihz.anlaki.dev.presentation

import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.presentation.notifications.KeepAliveNotification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import timber.log.Timber

/** Keeps the app process available for reliable Quick Settings tile actions. */
class KeepAliveService : Service() {
    /** Starts the foreground keep alive notification. */
    override fun onCreate() {
        super.onCreate()
        KeepAliveNotification.createChannel(this)
        val notification = KeepAliveNotification.build(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                KeepAliveNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(KeepAliveNotification.NOTIFICATION_ID, notification)
        }
    }

    /** Keeps the service sticky after a start request. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    /** Restarts the service after the task is removed when enabled. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (isKeepAliveEnabled(applicationContext)) {
            start(applicationContext)
        }
    }

    /** Returns null because clients never bind to this service. */
    override fun onBind(intent: Intent?): IBinder? = null

    /** Restarts the service after destroy when the toggle is on. */
    override fun onDestroy() {
        super.onDestroy()
        if (isKeepAliveEnabled(applicationContext)) {
            start(applicationContext)
        }
    }

    companion object {
        /**
         * Starts the foreground keep-alive service if Android permits it and the toggle is enabled.
         * @return true when Android accepted the start request
         */
        fun start(context: Context): Boolean {
            if (!isKeepAliveEnabled(context)) return false
            return try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, KeepAliveService::class.java)
                )
                true
            } catch (e: Exception) {
                Timber.w(e, "Unable to start keep-alive service")
                false
            }
        }

        /** Stops the foreground keep-alive service. */
        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }

        private fun isKeepAliveEnabled(context: Context): Boolean {
            return try {
                PreferencesHelper.init(context)
                PreferencesHelper.keepAliveEnabled
            } catch (error: Exception) {
                Timber.w(error, "Could not read keep-alive preference")
                // Fallback to raw SharedPreferences if PreferencesHelper is not yet fully initialized
                try {
                    val prefs = context.getSharedPreferences("akihz_prefs", Context.MODE_PRIVATE)
                    // If key missing -> default true per spec
                    if (!prefs.contains("keep_alive_enabled")) true else prefs.getBoolean("keep_alive_enabled", true)
                } catch (error: Exception) {
                    Timber.e(error, "Could not read keep-alive fallback preference")
                    true
                }
            }
        }
    }
}
