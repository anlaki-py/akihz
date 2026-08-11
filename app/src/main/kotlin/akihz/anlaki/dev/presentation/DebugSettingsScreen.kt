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

/** Displays hidden controls for tuning the home screen's expressive UI. */
@Composable
fun DebugSettingsScreen(
    settings: HomeDebugSettings,
    onSettingsChanged: (HomeDebugSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PreferenceLayout(label = "Debug settings", modifier = modifier) {
        PreferenceGroup(heading = "Navigation") {
            PreferenceTemplate(
                title = "Back to settings",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack
            )
        }
        PreferenceGroup(heading = "Home screen") {
            DebugSlider("Hz text size", settings.hzTextSizeSp, "sp", 18f..48f, 29, 1f) {
                onSettingsChanged(settings.copy(hzTextSizeSp = it))
            }
            DebugSlider("Button height", settings.buttonHeightDp, "dp", 48f..140f, 45, 2f) {
                onSettingsChanged(settings.copy(buttonHeightDp = it))
            }
            DebugSlider("Button width", settings.buttonWidthPercent, "%", 60f..100f, 19, 2f) {
                onSettingsChanged(settings.copy(buttonWidthPercent = it))
            }
            DebugSlider("Button spacing", settings.buttonSpacingDp, "dp", 0f..32f, 31, 1f) {
                onSettingsChanged(settings.copy(buttonSpacingDp = it))
            }
        }
        PreferenceGroup(heading = "Expressive shape") {
            DebugSlider("Resting corners", settings.restingCornerDp, "dp", 0f..48f, 47, 1f) {
                onSettingsChanged(settings.copy(restingCornerDp = it))
            }
            DebugSlider("Selected corners", settings.selectedCornerDp, "dp", 0f..48f, 47, 1f) {
                onSettingsChanged(settings.copy(selectedCornerDp = it))
            }
            DebugSlider("Pressed corners", settings.pressedCornerDp, "dp", 0f..48f, 47, 1f) {
                onSettingsChanged(settings.copy(pressedCornerDp = it))
            }
            PreferenceTemplate(
                title = "Restore debug defaults",
                description = "Reset all home screen tuning",
                icon = Icons.Default.Restore,
                onClick = { onSettingsChanged(HomeDebugSettings.defaults()) }
            )
        }
    }
}

@Composable
private fun DebugSlider(
    title: String,
    value: Float,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    increment: Float,
    onValueChanged: (Float) -> Unit
) {
    PreferenceSlider(
        title = title,
        value = value,
        valueLabel = "${value.toInt()} $unit",
        onValueChange = onValueChanged,
        onValueChangeFinished = {},
        valueRange = range,
        steps = steps,
        increment = increment
    )
}
