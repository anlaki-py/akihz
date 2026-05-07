package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val WEBSITE_URL = "https://anlaki.dev"
private const val SOURCE_CODE_URL = "https://github.com/anlaki-py/akihz"
private const val DONATION_URL = "https://ko-fi.com/unluky"

/**
 * Shows app attribution and external project links.
 *
 * @param modifier layout modifier supplied by the parent screen
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = { uriHandler.openUri(WEBSITE_URL) }) {
            Text("Made by unlucky")
        }

        OutlinedButton(
            onClick = { uriHandler.openUri(SOURCE_CODE_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Source code")
        }

        Button(
            onClick = { uriHandler.openUri(DONATION_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Donate on Ko-fi")
        }
    }
}
