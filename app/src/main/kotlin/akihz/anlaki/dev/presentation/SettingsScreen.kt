package akihz.anlaki.dev.presentation

import androidx.compose.runtime.Composable
import akihz.anlaki.dev.presentation.components.PreferenceLayout

@Composable
fun SettingsScreen(onResetToDefaults: () -> Unit) {
    PreferenceLayout(label = "Settings") {
        WatchdogSection()
        AdvancedSection(onResetToDefaults = onResetToDefaults)
        AboutSection()
    }
}
