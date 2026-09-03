package akihz.anlaki.dev.utils

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.data.fps.FpsDebugLogger
import akihz.anlaki.dev.data.fps.LayerStat
import akihz.anlaki.dev.data.fps.TimeStatsParser
import timber.log.Timber
import java.util.Locale
import java.util.Objects

/**
 * Foreground service that samples SurfaceFlinger TimeStats every 500 ms
 * and updates the floating FPS overlay.
 */
class FpsMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var samplingJob: Job? = null
    private var overlay: FpsOverlayController? = null
    private var logger: FpsDebugLogger? = null
    private var lastLoggedForeground: String? = null
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        FpsMonitorNotification.createChannel(this)
        val notification = FpsMonitorNotification.build(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FpsMonitorNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FpsMonitorNotification.NOTIFICATION_ID, notification)
        }
        PreferencesHelper.init(this)
        logger = FpsDebugLogger(this).also {
            it.init(PreferencesHelper.fpsDebugLoggingEnabled)
            it.append(
                "FPS Monitor started; binder=${ShizukuHelper.isBinderReady()} " +
                    "uid=${safeUid()} permission=${safePermission()}"
            )
        }
        overlay = FpsOverlayController(this).also { it.attach() }
        PreferencesHelper.fpsRunning = true

        ShizukuHelper.acquireUserService(
            owner = OWNER,
            onConnected = {
                overlay?.setStatus("Collecting data…")
                runShell("dumpsys SurfaceFlinger --timestats -clear -enable")
                logger?.append("TimeStats enabled; beginning 500 ms sampling")
                startSampling()
            },
            onFailed = { _, message ->
                overlay?.setStatus(message)
                logger?.append("ERROR Shizuku bind failed: $message")
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                logger?.append("User action: stop monitor")
                stopSelf()
            }
            ACTION_NOTE -> {
                val note = intent.getStringExtra(EXTRA_NOTE) ?: return START_NOT_STICKY
                logger?.append("User action: $note")
            }
            ACTION_SET_LOGGING -> {
                val enabled = intent.getBooleanExtra(EXTRA_ENABLED, false)
                logger?.setEnabled(enabled)
            }
            ACTION_SET_SCALE -> {
                val scale = intent.getIntExtra(EXTRA_SCALE, 100)
                overlay?.setScale(scale)
            }
            ACTION_SET_LAYER -> {
                val layer = intent.getStringExtra(EXTRA_LAYER)
                overlay?.setSelectedLayer(layer)
                logger?.append("User action: layer selection=${layer ?: "Auto"}")
            }
            ACTION_REFRESH_LAYER -> {
                overlay?.updateSelectedLayerFromPrefs()
            }
        }
        return START_NOT_STICKY
    }

    private fun startSampling() {
        if (samplingJob != null) return
        samplingJob = serviceScope.launch {
            // initial enable already done
            while (isActive && !stopping) {
                delay(500)
                sampleOnce()
            }
        }
    }

    private suspend fun sampleOnce() {
        if (stopping) return
        if (!ShizukuHelper.isUserServiceBound()) {
            withContext(Dispatchers.Main) { overlay?.setStatus("Shizuku disconnected") }
            return
        }
        try {
            val fixedPackage = withContext(Dispatchers.IO) { PreferencesHelper.fpsTargetPackage }
            var windowDump: String? = null
            var activityDump: String? = null
            var foreground: String? = fixedPackage
            var source = "fixed app selection"

            if (foreground.isNullOrEmpty()) {
                windowDump = runShell("dumpsys window")
                val t1 = System.currentTimeMillis()
                foreground = TimeStatsParser.foregroundPackage(windowDump)
                logger?.append("Foreground parse completed; elapsed=${System.currentTimeMillis() - t1} ms; result=$foreground")
                source = "WindowManager"
                if (foreground == null) {
                    activityDump = runShell("dumpsys activity activities")
                    val t2 = System.currentTimeMillis()
                    foreground = TimeStatsParser.foregroundPackage(activityDump)
                    logger?.append("Activity fallback parse completed; elapsed=${System.currentTimeMillis() - t2} ms; result=$foreground")
                    source = "ActivityManager fallback"
                }
            }

            if (!Objects.equals(foreground, lastLoggedForeground)) {
                logger?.append("Foreground/target app changed: $lastLoggedForeground -> $foreground; source=$source")
                lastLoggedForeground = foreground
            }

            val dump = runShell("dumpsys SurfaceFlinger --timestats -dump -clear -maxlayers 64")
            val layers = TimeStatsParser.layers(dump, foreground)

            if (logger?.isEnabled() == true) {
                recordSample(source, fixedPackage, foreground, windowDump, activityDump, dump, layers)
            }

            withContext(Dispatchers.Main) {
                overlay?.display(foreground, layers)
            }
        } catch (e: Exception) {
            val text = "Monitor error"
            logger?.append("ERROR ${e.javaClass.name}: ${e.message}")
            withContext(Dispatchers.Main) { overlay?.setStatus(text) }
        }
    }

    private fun runShell(command: String): String? {
        val started = System.currentTimeMillis()
        logger?.append("Command started: $command")
        val result = ShizukuHelper.runShellCommand(command)
        return if (result.isSuccess) {
            val out = result.getOrNull() ?: ""
            logger?.append("Command completed: $command; elapsed=${System.currentTimeMillis() - started} ms; bytes=${out.length}")
            out
        } else {
            val err = (result as? akihz.anlaki.dev.utils.Result.Error)?.message ?: result.toString()
            logger?.append("ERROR command failed: $command; elapsed=${System.currentTimeMillis() - started} ms; $err")
            null
        }
    }

    private fun recordSample(
        source: String,
        fixedPackage: String?,
        foreground: String?,
        windowDump: String?,
        activityDump: String?,
        surfaceDump: String?,
        layers: List<LayerStat>
    ) {
        val sb = StringBuilder()
        sb.append("Mode: ").append(if (fixedPackage == null) "AUTO" else "FIXED").append('\n')
        if (fixedPackage != null) sb.append("Configured package: ").append(fixedPackage).append('\n')
        sb.append("Detection source: ").append(source).append('\n')
        sb.append("Detected/target package: ").append(foreground).append('\n')
        if (windowDump != null) {
            sb.append("WindowManager bytes: ").append(windowDump.length).append('\n')
            sb.append("Window focus lines:\n").append(TimeStatsParser.diagnosticLines(windowDump, 12)).append('\n')
        }
        if (activityDump != null) {
            sb.append("ActivityManager bytes: ").append(activityDump.length).append('\n')
            sb.append("Activity focus lines:\n").append(TimeStatsParser.diagnosticLines(activityDump, 12)).append('\n')
        }
        sb.append("TimeStats bytes: ").append(surfaceDump?.length ?: 0).append('\n')
        sb.append("Layer blocks returned: ").append(TimeStatsParser.layerBlockCount(surfaceDump)).append('\n')
        sb.append("Matching candidates: ").append(layers.size).append('\n')
        for (layer in layers) {
            sb.append("  - fps=").append(String.format(Locale.US, "%.3f", layer.fps))
                .append(" frames=").append(layer.frames)
                .append(" name=").append(layer.name).append('\n')
        }
        if (layers.isEmpty()) {
            sb.append("Returned layer names (first 20):\n")
                .append(TimeStatsParser.layerNames(surfaceDump, 20)).append('\n')
        }
        logger?.append(sb.toString().trim())
    }

    override fun onDestroy() {
        stopping = true
        samplingJob?.cancel()
        samplingJob = null
        logger?.append("Monitor stopping")
        logger?.persist()
        runShell("dumpsys SurfaceFlinger --timestats -disable")
        logger?.persist()
        overlay?.detach()
        overlay = null
        ShizukuHelper.releaseUserService(OWNER)
        PreferencesHelper.fpsRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun safeUid(): Int = try { ShizukuHelper.getUid() } catch (_: Exception) { -1 }
    private fun safePermission(): Int = try {
        if (ShizukuHelper.hasPermission()) 0 else -1
    } catch (_: Exception) { -2 }

    companion object {
        const val ACTION_STOP = "akihz.fps.STOP"
        const val ACTION_NOTE = "akihz.fps.NOTE"
        const val ACTION_SET_LOGGING = "akihz.fps.SET_LOGGING"
        const val ACTION_SET_SCALE = "akihz.fps.SET_SCALE"
        const val ACTION_SET_LAYER = "akihz.fps.SET_LAYER"
        const val ACTION_REFRESH_LAYER = "akihz.fps.REFRESH_LAYER"
        const val EXTRA_NOTE = "note"
        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_SCALE = "scale"
        const val EXTRA_LAYER = "layer"
        private const val OWNER = "fps_monitor"

        fun start(context: android.content.Context) {
            val intent = Intent(context, FpsMonitorService::class.java)
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Timber.w(e, "Unable to start FPS monitor service")
            }
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, FpsMonitorService::class.java).apply { action = ACTION_STOP })
        }
    }
}
