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
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import akihz.anlaki.dev.R
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RefreshRateWatchdogService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var displayManager: DisplayManager
    private lateinit var systemOverrideDetector: SystemOverrideDetector

    @Inject lateinit var refreshRateRepository: RefreshRateRepository

    private var desiredRate: Float = 0f
    private var isRunning = false
    private var isScreenOn = true
    private var listenersRegistered = false
    private var eventCollector: Job? = null
    private var checkJob: Job? = null

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
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        systemOverrideDetector = SystemOverrideDetector(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PreferencesHelper.init(applicationContext)

        if (!PreferencesHelper.watchdogEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        desiredRate = PreferencesHelper.desiredRate
        if (desiredRate <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground()
        isRunning = true

        if (!listenersRegistered) {
            isScreenOn = systemOverrideDetector.isInteractive()
            displayManager.registerDisplayListener(displayListener, handler)
            systemOverrideDetector.register()
            listenersRegistered = true

            eventCollector = scope.launch {
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
                        is SystemOverrideDetector.SystemEvent.PowerSaveChanged -> Unit
                        is SystemOverrideDetector.SystemEvent.ThermalThrottling -> Unit
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
        if (checkJob?.isActive == true) return

        checkJob = scope.launch {
            desiredRate = PreferencesHelper.desiredRate
            if (desiredRate <= 0) return@launch

            val currentResult = withContext(Dispatchers.IO) {
                refreshRateRepository.getCurrentRate()
            }

            currentResult.onSuccess { currentRate ->
                if (kotlin.math.abs(currentRate - desiredRate) >= 1f) {
                    val result = withContext(Dispatchers.IO) {
                        refreshRateRepository.setRate(desiredRate)
                    }
                    result.onError { _, message ->
                        Timber.w("Watchdog could not reapply refresh rate: %s", message)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(periodicCheckRunnable)
        checkJob?.cancel()
        eventCollector?.cancel()
        if (listenersRegistered) {
            displayManager.unregisterDisplayListener(displayListener)
            systemOverrideDetector.unregister()
            listenersRegistered = false
        }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "akihz_watchdog"
        private const val NOTIFICATION_ID = 1002
        private const val MIN_INTERVAL_MS = 1000L
        private const val MAX_INTERVAL_MS = 30000L
        private const val AGGRESSIVE_INTERVAL_MS = 500L

        fun start(context: Context) {
            if (!PreferencesHelper.watchdogEnabled) return
            try {
                val intent = Intent(context, RefreshRateWatchdogService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Timber.w(e, "Unable to start refresh-rate watchdog")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RefreshRateWatchdogService::class.java))
        }

        fun restart(context: Context) {
            stop(context)
            if (PreferencesHelper.watchdogEnabled) {
                start(context)
            }
        }
    }
}
