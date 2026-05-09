package akihz.anlaki.dev.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import akihz.anlaki.dev.data.DisplayManagerDataSource
import akihz.anlaki.dev.data.RefreshRateRepository
import akihz.anlaki.dev.data.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accessibility service that monitors foreground app changes
 * and applies per-app refresh rate profiles.
 *
 * When switching to an app with a profile, it applies that profile's rate
 * and tells the watchdog "this is intentional, don't fight it."
 * When switching away from a profiled app, it restores the global rate
 * and clears the override so the watchdog resumes normal operation.
 *
 * Requires user to enable this service in Accessibility settings.
 */
class AppMonitorService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var refreshRateRepository: RefreshRateRepository

    private var currentPackage: String? = null
    private var isApplying = false
    private var lastProfiledPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        PreferencesHelper.init(applicationContext)
        refreshRateRepository = RefreshRateRepository(DisplayManagerDataSource(applicationContext))
        Log.d(TAG, "AppMonitorService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!PreferencesHelper.appMonitorEnabled) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentPackage) return
        if (shouldIgnorePackage(packageName)) return

        currentPackage = packageName
        handleAppSwitch(packageName)
    }

    override fun onInterrupt() {
        // No-op
    }

    /**
     * Packages to ignore — system UI, launchers, and our own app.
     * These should never trigger rate changes or restore logic.
     */
    private fun shouldIgnorePackage(packageName: String): Boolean {
        return when {
            packageName == "akihz.anlaki.dev" -> true
            packageName == "com.android.systemui" -> true
            packageName == "com.google.android.apps.nexuslauncher" -> true
            packageName == "com.google.android.launcher" -> true
            packageName.startsWith("com.android.launcher") -> true
            packageName.startsWith("com.miui.home") -> true
            packageName.startsWith("com.samsung.android.launcher") -> true
            packageName.startsWith("com.oppo.launcher") -> true
            packageName.startsWith("com.coloros.launcher") -> true
            packageName.startsWith("com.oneplus.launcher") -> true
            packageName.startsWith("com.huawei.android.launcher") -> true
            packageName.startsWith("com.hihonor.android.launcher") -> true
            packageName.startsWith("com.asus.launcher") -> true
            packageName.startsWith("com.sonyericsson.home") -> true
            packageName.startsWith("com.htc.launcher") -> true
            packageName.startsWith("com.lge.launcher") -> true
            packageName.startsWith("com.motorola.launcher") -> true
            packageName.startsWith("com.nokia.z.launcher") -> true
            packageName.startsWith("com.microsoft.launcher") -> true
            packageName.startsWith("com.teslacoilsw.launcher") -> true
            packageName.startsWith("com.actiondash.playstore") -> true
            packageName.startsWith("com.google.android.apps.wallpaper") -> true
            packageName.startsWith("com.android.settings") -> true
            packageName.startsWith("com.google.android.apps.wellbeing") -> true
            else -> false
        }
    }

    private fun handleAppSwitch(packageName: String) {
        val profileRate = PreferencesHelper.getAppProfile(packageName)

        if (profileRate != null) {
            // Switching TO an app with a profile — apply its rate and register override
            lastProfiledPackage = packageName
            registerOverride(profileRate, packageName)
            applyRate(profileRate, packageName)
        } else if (lastProfiledPackage != null && !isSameAppFamily(packageName, lastProfiledPackage!!)) {
            // Switching AWAY from a profiled app to a different app — restore global rate
            lastProfiledPackage = null
            clearOverride()
            restoreGlobalRate()
        }
    }

    /**
     * Checks if two packages belong to the same app family.
     * Used to avoid restoring global rate when switching between
     * activities of the same app (e.g., Instagram camera vs feed).
     */
    private fun isSameAppFamily(a: String, b: String): Boolean {
        // Exact match
        if (a == b) return true

        // Same app but different process (e.g., com.instagram.android vs com.instagram.android:x)
        val baseA = a.split(":").first()
        val baseB = b.split(":").first()
        if (baseA == baseB) return true

        // Known split-screen / multi-window pairs
        return false
    }

    /**
     * Registers an active override so the watchdog knows not to fight it.
     */
    private fun registerOverride(rate: Float, packageName: String) {
        PreferencesHelper.activeOverrideRate = rate
        PreferencesHelper.activeOverridePackage = packageName
        Log.d(TAG, "Registered override: $packageName @ ${rate.toInt()}Hz")
    }

    /**
     * Clears the active override so the watchdog resumes normal operation.
     */
    private fun clearOverride() {
        PreferencesHelper.activeOverrideRate = 0f
        PreferencesHelper.activeOverridePackage = ""
        Log.d(TAG, "Cleared override")
    }

    private fun applyRate(rate: Float, packageName: String) {
        if (isApplying) return
        if (!ShizukuHelper.isBinderReady() || !ShizukuHelper.hasPermission()) {
            Log.w(TAG, "Shizuku not available, cannot apply rate for $packageName")
            return
        }

        isApplying = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(rate)
            }

            result.onSuccess {
                Log.d(TAG, "Applied ${rate.toInt()} Hz for $packageName")
            }.onError { _, message ->
                Log.w(TAG, "Failed to apply rate for $packageName: $message")
            }

            handler.postDelayed({ isApplying = false }, DEBOUNCE_MS)
        }
    }

    private fun restoreGlobalRate() {
        val globalRate = PreferencesHelper.globalRate
        if (globalRate <= 0) {
            Log.d(TAG, "No global rate set, skipping restore")
            return
        }

        if (!ShizukuHelper.isBinderReady() || !ShizukuHelper.hasPermission()) {
            Log.w(TAG, "Shizuku not available, cannot restore global rate")
            return
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(globalRate)
            }

            result.onSuccess {
                Log.d(TAG, "Restored global rate ${globalRate.toInt()} Hz")
            }.onError { _, message ->
                Log.w(TAG, "Failed to restore global rate: $message")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "AppMonitorService"
        private const val DEBOUNCE_MS = 500L

        /**
         * Checks if the accessibility service is enabled.
         */
        fun isEnabled(context: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedComponent = "${context.packageName}/${AppMonitorService::class.java.canonicalName}"
            return enabledServices.split(":").any { it.trim() == expectedComponent }
        }

        /**
         * Opens Accessibility settings so the user can enable this service.
         */
        fun openSettings(context: Context) {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}