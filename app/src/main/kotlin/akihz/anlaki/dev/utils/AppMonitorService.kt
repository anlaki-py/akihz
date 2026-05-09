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
 * Requires user to enable this service in Accessibility settings.
 * When enabled, it detects app switches and applies the configured
 * refresh rate for each app.
 */
class AppMonitorService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var refreshRateRepository: RefreshRateRepository

    private var currentPackage: String? = null
    private var isApplying = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        PreferencesHelper.init(applicationContext)
        refreshRateRepository = RefreshRateRepository(DisplayManagerDataSource(applicationContext))
        Log.d(TAG, "AppMonitorService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!PreferencesHelper.appMonitorEnabled) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentPackage) return
        if (packageName == "akihz.anlaki.dev") return // Skip self

        currentPackage = packageName
        applyProfileForApp(packageName)
    }

    override fun onInterrupt() {
        // No-op
    }

    private fun applyProfileForApp(packageName: String) {
        if (isApplying) return
        if (!ShizukuHelper.isBinderReady() || !ShizukuHelper.hasPermission()) {
            // Try to bind if not connected
            if (!ShizukuHelper.isUserServiceBound()) {
                ShizukuHelper.bindUserService(
                    onConnected = { applyProfileForApp(packageName) },
                    onFailed = { _, _ -> }
                )
            }
            return
        }

        val profileRate = PreferencesHelper.getAppProfile(packageName)
        val rateToApply = profileRate ?: PreferencesHelper.defaultRate

        if (rateToApply <= 0) return

        isApplying = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(rateToApply)
            }

            result.onSuccess {
                Log.d(TAG, "Applied ${rateToApply.toInt()} Hz for $packageName")
            }.onError { _, message ->
                Log.w(TAG, "Failed to apply rate for $packageName: $message")
            }

            handler.postDelayed({ isApplying = false }, 500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "AppMonitorService"

        /**
         * Checks if the accessibility service is enabled.
         */
        fun isEnabled(context: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val componentName = "${context.packageName}/${AppMonitorService::class.java.name}"
            return enabledServices.contains(componentName)
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