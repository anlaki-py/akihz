package akihz.anlaki.dev.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Direct entry point for crash-log debugging via CLI.
 *
 * Launch with:
 * `adb shell am start -n akihz.anlaki.dev.debug/akihz.anlaki.dev.presentation.CrashLogActivity`
 * or for release: `adb shell am start -n akihz.anlaki.dev/akihz.anlaki.dev.presentation.CrashLogActivity`
 */
@AndroidEntryPoint
class CrashLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnlakiTheme {
                CrashLogScreen(onBack = ::finish)
            }
        }
    }
}
