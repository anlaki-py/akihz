package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable

/**
 * Legacy advanced preferences - preserved for backward compatibility.
 *
 * Previously hosted keep-alive, background, custom keys, OEM, reset and diagnostics
 * in a single "Advanced" card. Now delegates to the split semantic groups.
 *
 * @param onResetToDefaults restores adaptive system refresh-rate settings
 * @param onOpenCustomKeys opens experimental custom-key configuration
 * @param keepAliveEnabled whether the persistent keep-alive notification/service is enabled
 * @param onKeepAliveEnabledChanged invoked when the keep-alive toggle changes
 */
@Deprecated(
    "Use BackgroundSection, RefreshRateEngineSection and DiagnosticsSection directly",
    ReplaceWith(
        "Column { BackgroundSection(keepAliveEnabled, onKeepAliveEnabledChanged); RefreshRateEngineSection(onResetToDefaults, onOpenCustomKeys); DiagnosticsSection() }",
        imports = [
            "akihz.anlaki.dev.presentation.BackgroundSection",
            "akihz.anlaki.dev.presentation.RefreshRateEngineSection",
            "akihz.anlaki.dev.presentation.DiagnosticsSection"
        ]
    )
)
@Composable
fun AdvancedSection(
    onResetToDefaults: () -> Unit,
    onOpenCustomKeys: () -> Unit,
    keepAliveEnabled: Boolean,
    onKeepAliveEnabledChanged: (Boolean) -> Unit
) {
    Column {
        BackgroundSection(
            keepAliveEnabled = keepAliveEnabled,
            onKeepAliveEnabledChanged = onKeepAliveEnabledChanged
        )
        RefreshRateEngineSection(
            onResetToDefaults = onResetToDefaults,
            onOpenCustomKeys = onOpenCustomKeys
        )
        DiagnosticsSection()
    }
}
