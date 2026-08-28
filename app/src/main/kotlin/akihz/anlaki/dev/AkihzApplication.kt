package akihz.anlaki.dev

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import akihz.anlaki.dev.data.CrashLogStore
import akihz.anlaki.dev.data.CustomProfileManager
import akihz.anlaki.dev.data.PerformanceMonitor
import akihz.anlaki.dev.data.UpdateDownloadStore
import akihz.anlaki.dev.data.UpdateCheckScheduler
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.UpdateAvailableNotification
import akihz.anlaki.dev.utils.UpdateNotification
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
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
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        installCrashLogger()
        installLifecycleLogger()
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Synchronous write: process may die before coroutines run.
            runCatching { crashLogStore.saveSync(thread, throwable) }
                .onFailure { Timber.e(it, "saveSync failed") }
            // Best-effort also log to perf recorder if a session is active.
            runCatching {
                // Use blocking check via scope is not reliable here; fire-and-forget is ok
                // since we already persisted the crash to crash_logs.
                applicationScope.launch {
                    performanceMonitor.log(
                        tag = "uncaught_exception",
                        message = throwable.javaClass.name + ": " + (throwable.message ?: ""),
                        data = mapOf(
                            "thread" to thread.name,
                            "stack" to throwable.stackTraceToString().take(2_000)
                        )
                    )
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun installLifecycleLogger() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                applicationScope.launch {
                    performanceMonitor.log(tag = "lifecycle", message = "foreground")
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                applicationScope.launch {
                    performanceMonitor.log(tag = "lifecycle", message = "background")
                }
            }
        })
    }
}
