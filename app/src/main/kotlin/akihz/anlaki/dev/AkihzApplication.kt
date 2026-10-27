package akihz.anlaki.dev

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import akihz.anlaki.dev.data.CrashLogStore
import akihz.anlaki.dev.data.DiagnosticLogTree
import akihz.anlaki.dev.data.ProcessExitLogCollector
import akihz.anlaki.dev.data.CustomProfileManager
import akihz.anlaki.dev.data.PerformanceMonitor
import akihz.anlaki.dev.data.UpdateDownloadStore
import akihz.anlaki.dev.data.UpdateCheckScheduler
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.presentation.theme.LauncherIconLifecycleObserver
import akihz.anlaki.dev.presentation.notifications.UpdateAvailableNotification
import akihz.anlaki.dev.presentation.notifications.UpdateNotification
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AkihzApplication : Application() {

    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var crashLogStore: CrashLogStore
    @Inject lateinit var processExitLogCollector: ProcessExitLogCollector

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Sets up prefs, crash logging, lifecycle logging, and update checks. */
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DiagnosticLogTree(crashLogStore))
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        installCrashLogger()
        Timber.tag("AppLifecycle").i("Process started, pid=%d", android.os.Process.myPid())
        applicationScope.launch { processExitLogCollector.collect() }
        PreferencesHelper.init(this)
        CustomProfileManager.init(this)
        if (PreferencesHelper.latestAvailableVersionCode <= BuildConfig.VERSION_CODE) {
            PreferencesHelper.clearAvailableUpdate()
            UpdateAvailableNotification.cancel(this)
        }
        UpdateCheckScheduler.sync(this)
        UpdateDownloadStore(this).load()
            ?.takeIf { it.update.versionCode <= BuildConfig.VERSION_CODE }
            ?.let {
                UpdateDownloadStore(this).clear()
                UpdateNotification.cancelReady(this)
            }
        ProcessLifecycleOwner.get().lifecycle.addObserver(LauncherIconLifecycleObserver(this))
        installLifecycleLogger()
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // The disk write completes before Android's handler terminates the process.
                crashLogStore.saveSync(thread, throwable)
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun installLifecycleLogger() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            /** Logs a foreground marker to the perf recorder. */
            override fun onStart(owner: LifecycleOwner) {
                Timber.tag("AppLifecycle").i("App foreground")
                applicationScope.launch {
                    performanceMonitor.log(tag = "lifecycle", message = "foreground")
                }
            }

            /** Logs a background marker to the perf recorder. */
            override fun onStop(owner: LifecycleOwner) {
                Timber.tag("AppLifecycle").i("App background")
                applicationScope.launch {
                    performanceMonitor.log(tag = "lifecycle", message = "background")
                }
            }
        })
    }
}
