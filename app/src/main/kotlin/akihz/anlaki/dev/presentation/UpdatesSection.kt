package akihz.anlaki.dev.presentation

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.presentation.components.PreferenceGroup

/**
 * Update management preferences.
 *
 * @param autoCheckRequest changes when an update notification requests a fresh check
 */
@Composable
fun UpdatesSection(autoCheckRequest: Int = 0) {
    val context = LocalContext.current
    val pm = context.packageManager
    val packageInfo = remember {
        try {
            pm.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
    val currentVersionCode = packageInfo?.longVersionCode ?: BuildConfig.VERSION_CODE.toLong()

    PreferenceGroup(heading = "Updates") {
        AppUpdateSection(
            currentVersionCode = currentVersionCode,
            autoCheckRequest = autoCheckRequest
        )
    }
}
