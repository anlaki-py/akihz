package akihz.anlaki.dev.presentation

import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

private const val SOURCE_CODE_URL = "https://github.com/anlaki-py/akihz"
private const val DONATION_URL = "https://ko-fi.com/unluky"

/**
 * Displays app version information and project links.
 *
 * @param onVersionClick invoked when the version row is tapped
 */
@Composable
fun AboutSection(onVersionClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val pm = context.packageManager
    val packageInfo = remember {
        try {
            pm.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: BuildConfig.VERSION_NAME
    val versionCode = packageInfo?.longVersionCode ?: BuildConfig.VERSION_CODE.toLong()

    PreferenceGroup(heading = "About") {
        PreferenceTemplate(
            title = "akiHz",
            description = "Version $versionName",
            icon = Icons.Default.Info,
            onClick = onVersionClick
        )

        PreferenceTemplate(
            title = "Source code",
            description = "github.com/anlaki-py/akihz",
            icon = Icons.Default.Code,
            onClick = { uriHandler.openUri(SOURCE_CODE_URL) }
        )

        PreferenceTemplate(
            title = "Donate",
            description = "ko-fi.com/unluky",
            icon = Icons.Default.Favorite,
            onClick = { uriHandler.openUri(DONATION_URL) }
        )

        AppUpdateSection(currentVersionCode = versionCode)
    }
}
