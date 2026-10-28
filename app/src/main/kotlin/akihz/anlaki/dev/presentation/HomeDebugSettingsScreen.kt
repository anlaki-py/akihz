package akihz.anlaki.dev.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceSlider
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/** Displays controls for tuning the home screen's refresh-rate interface. */
@Composable
internal fun HomeDebugSettingsScreen(
    settings: HomeDebugSettings,
    onSettingsChanged: (HomeDebugSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaults = HomeDebugSettings.defaults()
    PreferenceLayout(
        label = "Home screen",
        modifier = modifier,
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationContentDescription = "Back to debug options",
        onNavigationClick = onBack
    ) {
        PreferenceGroup(heading = "Preview data") {
            PreferenceTemplate(
                title = "Show fake refresh rates",
                description = "Preview extra rates without applying them",
                checked = settings.showFakeRefreshRates,
                onCheckedChange = {
                    onSettingsChanged(settings.copy(showFakeRefreshRates = it))
                }
            )
        }
        PreferenceGroup(heading = "Refresh-rate buttons") {
            formItem {
                HomeDebugSlider(
                    "Hz text size", "Resize rate labels.",
                    settings.hzTextSizeSp, "sp", 18f..48f, defaults.hzTextSizeSp, 29, 1f
                ) { onSettingsChanged(settings.copy(hzTextSizeSp = it)) }
            }
            formItem {
                HomeDebugSlider(
                    "Button height", "Resize button height.",
                    settings.buttonHeightDp, "dp", 48f..160f, defaults.buttonHeightDp, 55, 2f
                ) { onSettingsChanged(settings.copy(buttonHeightDp = it)) }
            }
            formItem {
                HomeDebugSlider(
                    "Button width", "Resize button groups.",
                    settings.buttonWidthPercent, "%", 60f..100f,
                    defaults.buttonWidthPercent, 19, 2f
                ) { onSettingsChanged(settings.copy(buttonWidthPercent = it)) }
            }
            formItem {
                HomeDebugSlider(
                    "Button spacing", "Space between buttons.",
                    settings.buttonSpacingDp, "dp", 0f..32f, defaults.buttonSpacingDp, 31, 1f
                ) { onSettingsChanged(settings.copy(buttonSpacingDp = it)) }
            }
            PreferenceTemplate(
                title = "Restore home-screen defaults",
                description = "Reset this page",
                icon = Icons.Default.Restore,
                onClick = { onSettingsChanged(defaults) }
            )
        }
    }
}

@Composable
private fun HomeDebugSlider(
    title: String,
    description: String,
    value: Float,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    default: Float,
    steps: Int,
    increment: Float,
    onValueChanged: (Float) -> Unit
) {
    PreferenceSlider(
        title = title,
        value = value,
        valueLabel = "${value.toInt()} $unit",
        description = "$description Default: ${default.toInt()} $unit",
        onValueChange = onValueChanged,
        onValueChangeFinished = {},
        valueRange = range,
        steps = steps,
        increment = increment
    )
}
