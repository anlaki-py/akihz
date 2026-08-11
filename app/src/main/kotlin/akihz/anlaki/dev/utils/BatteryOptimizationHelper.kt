package akihz.anlaki.dev.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Opens Android settings used to permit reliable background operation. */
object BatteryOptimizationHelper {
    /**
     * Requests a battery-optimization exemption for this app.
     *
     * Falls back to the battery optimization app list when a device does not
     * expose the direct system approval screen.
     *
     * @param context context used to open Android settings
     */
    fun requestExemption(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val directRequest = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        runCatching {
            context.startActivity(directRequest)
        }.onFailure {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
