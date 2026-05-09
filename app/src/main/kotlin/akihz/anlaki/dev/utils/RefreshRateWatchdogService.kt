package akihz.anlaki.dev.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import akihz.anlaki.dev.R
import akihz.anlaki.dev.data.DisplayManagerDataSource
import akihz.anlaki.dev.data.RefreshRateRepository
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that continuously monitors the current refresh rate
 * and re-applies the desired rate when the system overrides it.
 *
 * Configurable via [PreferencesHelper]:
 * - Enabled/disabled
 * - Re-apply interval (ms)
 * - Aggressive mode (shorter interval)
 */
class RefreshRateWatchdogService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var displayManager: DisplayManager
    private lateinit var refreshRateRepository: RefreshRateRepository
    private lateinit var systemOverrideDetector: SystemOverrideDetector

    private var desiredRate: Float = 0f
    private var isRunning = false
    private var isScreenOn = true

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == android.view.Display.DEFAULT_DISPLAY) {
                checkAndReapply()
            }
        }
    }

    private val periodicCheckRunnable = object : Runnable {
        override fun run() {
            if (isRunning && isScreenOn) {
                checkAndReapply()
                scheduleNextCheck()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        PreferencesHelper.init(applicationContext)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        refreshRateRepository = RefreshRateRepository(DisplayManagerDataSource(applicationContext))
        systemOverrideDetector = SystemOverrideDetector(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PreferencesHelper.watchdogEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        desiredRate = PreferencesHelper.desiredRate
        if (desiredRate <= 0) {
            desiredRate = PreferencesHelper.lastRate
        }

        if (desiredRate <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground()
        isRunning = true

        displayManager.registerDisplayListener(displayListener, handler)
        systemOverrideDetector.register()

        scope.launch {
            systemOverrideDetector.events.collect { event ->
                when (event) {
                    is SystemOverrideDetector.SystemEvent.ScreenOn -> {
                        isScreenOn = true
                        scheduleNextCheck()
                    }
                    is SystemOverrideDetector.SystemEvent.ScreenOff -> {
                        isScreenOn = false
                        handler.removeCallbacks(periodicCheckRunnable)
                    }
                    is SystemOverrideDetector.SystemEvent.PowerSaveChanged -> {
                        if (event.enabled && !PreferencesHelper.batterySaverOverride) {
                            // Battery saver is on and user hasn't enabled override
                            // Skip re-applying to avoid fighting the system
                        } else {
                            checkAndReapply()
                        }
                    }
                    is SystemOverrideDetector.SystemEvent.ThermalThrottling -> {
                        // Skip re-applying during thermal throttling
                        Log.d(TAG, "Thermal throttling detected: ${event.temperatureCelsius}C")
                    }
                }
            }
        }

        scheduleNextCheck()

        return START_STICKY
    }

    private fun startForeground() {
        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.watchdog_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.watchdog_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rateText = if (desiredRate > 0) "${desiredRate.toInt()} Hz" else "Active"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.watchdog_notification_title))
            .setContentText(getString(R.string.watchdog_notification_text, rateText))
            .setSmallIcon(R.drawable.ic_refresh_rate)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .build()
    }

    private fun scheduleNextCheck() {
        handler.removeCallbacks(periodicCheckRunnable)
        val interval = if (PreferencesHelper.watchdogAggressive) {
            AGGRESSIVE_INTERVAL_MS
        } else {
            PreferencesHelper.watchdogIntervalMs.coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
        }
        handler.postDelayed(periodicCheckRunnable, interval)
    }

    private fun checkAndReapply() {
        if (!isRunning || !isScreenOn) return
        if (!ShizukuHelper.isBinderReady() || !ShizukuHelper.hasPermission()) return

        // Skip if battery saver is on and override is disabled
        if (systemOverrideDetector.isPowerSaveMode() && !PreferencesHelper.batterySaverOverride) {
            return
        }

        scope.launch {
            val currentResult = withContext(Dispatchers.IO) {
                refreshRateRepository.getCurrentRate()
            }

            currentResult.onSuccess { currentRate ->
                if (kotlin.math.abs(currentRate - desiredRate) >= 1f) {
                    Log.d(TAG, "Rate mismatch: current=$currentRate, desired=$desiredRate. Re-applying...")
                    reapplyRate()
                }
            }
        }
    }

    private fun reapplyRate() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(desiredRate)
            }

            result.onSuccess {
                Log.d(TAG, "Successfully re-applied ${desiredRate.toInt()} Hz")
            }.onError { _, message ->
                Log.w(TAG, "Failed to re-apply rate: $message")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(periodicCheckRunnable)
        displayManager.unregisterDisplayListener(displayListener)
        systemOverrideDetector.unregister()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RefreshRateWatchdog"
        private const val CHANNEL_ID = "akihz_watchdog"
        private const val NOTIFICATION_ID = 1002
        private const val MIN_INTERVAL_MS = 1000L
        private const val MAX_INTERVAL_MS = 30000L
        private const val AGGRESSIVE_INTERVAL_MS = 500L

        /**
         * Starts the watchdog service if enabled in preferences.
         */
        fun start(context: Context) {
            if (!PreferencesHelper.watchdogEnabled) return
            try {
                val intent = Intent(context, RefreshRateWatchdogService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to start watchdog service", e)
            }
        }

        /**
         * Stops the watchdog service.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, RefreshRateWatchdogService::class.java))
        }

        /**
         * Restarts the watchdog service with updated settings.
         */
        fun restart(context: Context) {
            stop(context)
            if (PreferencesHelper.watchdogEnabled) {
                start(context)
            }
        }
    }
}