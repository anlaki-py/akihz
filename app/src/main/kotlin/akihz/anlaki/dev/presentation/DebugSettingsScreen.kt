package akihz.anlaki.dev.presentation

import androidx.activity.compose.BackHandler
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
    val defaults = HomeDebugSettings.defaults()
    BackHandler(onBack = onBack)
    PreferenceLayout(
        label = "Debug options",
        modifier = modifier,
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationContentDescription = "Back to settings",
        onNavigationClick = onBack
    ) {
        PreferenceGroup(heading = "Home screen") {
            DebugSlider(
                title = "Hz text size",
                description = "Changes the size of the refresh-rate label inside every button.",
                value = settings.hzTextSizeSp, unit = "sp", range = 18f..48f,
                default = defaults.hzTextSizeSp, steps = 29, increment = 1f
            ) {
                onSettingsChanged(settings.copy(hzTextSizeSp = it))
            }
            DebugSlider(
                title = "Button height",
                description = "Sets each button's fixed height; the list scrolls if it no longer fits.",
                value = settings.buttonHeightDp, unit = "dp", range = 48f..140f,
                default = defaults.buttonHeightDp, steps = 45, increment = 2f
            ) {
                onSettingsChanged(settings.copy(buttonHeightDp = it))
            }
            DebugSlider(
                title = "Button width",
                description = "Controls how much of the available home-screen width buttons use.",
                value = settings.buttonWidthPercent, unit = "%", range = 60f..100f,
                default = defaults.buttonWidthPercent, steps = 19, increment = 2f
            ) {
                onSettingsChanged(settings.copy(buttonWidthPercent = it))
            }
            DebugSlider(
                title = "Button spacing",
                description = "Adjusts the vertical gap between neighboring refresh-rate buttons.",
                value = settings.buttonSpacingDp, unit = "dp", range = 0f..32f,
                default = defaults.buttonSpacingDp, steps = 31, increment = 1f
            ) {
                onSettingsChanged(settings.copy(buttonSpacingDp = it))
            }
        }
        PreferenceGroup(heading = "Expressive shape") {
            DebugSlider(
                title = "Resting corners",
                description = "Corner radius used by an unselected button at rest.",
                value = settings.restingCornerDp, unit = "dp", range = 0f..48f,
                default = defaults.restingCornerDp, steps = 47, increment = 1f
            ) {
                onSettingsChanged(settings.copy(restingCornerDp = it))
            }
            DebugSlider(
                title = "Selected corners",
                description = "Corner radius used by the active refresh-rate button.",
                value = settings.selectedCornerDp, unit = "dp", range = 0f..48f,
                default = defaults.selectedCornerDp, steps = 47, increment = 1f
            ) {
                onSettingsChanged(settings.copy(selectedCornerDp = it))
            }
            DebugSlider(
                title = "Pressed corners",
                description = "Corner radius animated to while your finger holds a button.",
                value = settings.pressedCornerDp, unit = "dp", range = 0f..48f,
                default = defaults.pressedCornerDp, steps = 47, increment = 1f
            ) {
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
        description = "$description Default ${default.toInt()} $unit • range " +
            "${range.start.toInt()}–${range.endInclusive.toInt()} $unit.",
        onValueChange = onValueChanged,
        onValueChangeFinished = {},
        valueRange = range,
        steps = steps,
        increment = increment
    )
}
