package akihz.anlaki.dev.presentation

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

private const val WEBSITE_URL = "https://anlaki.dev"
private const val SOURCE_CODE_URL = "https://github.com/anlaki-py/akihz"
private const val DONATION_URL = "https://ko-fi.com/unluky"
private const val LATEST_RELEASE_URL = "https://github.com/anlaki-py/akihz/releases/latest"

@Composable
fun AboutSection() {
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

    PreferenceGroup(heading = "About") {
        Text(
            text = "akihz",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            text = "Version $versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        PreferenceTemplate(
            title = "Website",
            description = "anlaki.dev",
            onClick = { uriHandler.openUri(WEBSITE_URL) }
        )

        PreferenceTemplate(
            title = "Source code",
            description = "github.com/anlaki-py/akihz",
            onClick = { uriHandler.openUri(SOURCE_CODE_URL) }
        )

        PreferenceTemplate(
            title = "Donate",
            description = "ko-fi.com/unluky",
            onClick = { uriHandler.openUri(DONATION_URL) }
        )

        PreferenceTemplate(
            title = "Check for updates",
            onClick = { uriHandler.openUri(LATEST_RELEASE_URL) }
        )
    }
}
