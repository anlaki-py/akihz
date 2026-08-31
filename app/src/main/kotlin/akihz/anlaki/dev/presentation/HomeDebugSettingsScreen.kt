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
                description = "Fill Home with preview-only rates to test scrolling and layout.",
                checked = settings.showFakeRefreshRates,
                onCheckedChange = {
                    onSettingsChanged(settings.copy(showFakeRefreshRates = it))
                }
            )
        }
        PreferenceGroup(heading = "Refresh-rate buttons") {
            HomeDebugSlider(
                "Hz text size", "Changes the refresh-rate label size inside every button.",
                settings.hzTextSizeSp, "sp", 18f..48f, defaults.hzTextSizeSp, 29, 1f
            ) { onSettingsChanged(settings.copy(hzTextSizeSp = it)) }
            HomeDebugSlider(
                "Button height", "Sets each fixed height; the list scrolls if it no longer fits.",
                settings.buttonHeightDp, "dp", 48f..160f, defaults.buttonHeightDp, 55, 2f
            ) { onSettingsChanged(settings.copy(buttonHeightDp = it)) }
            HomeDebugSlider(
                "Button width", "Controls how much of the available width each button uses.",
                settings.buttonWidthPercent, "%", 60f..100f,
                defaults.buttonWidthPercent, 19, 2f
            ) { onSettingsChanged(settings.copy(buttonWidthPercent = it)) }
            HomeDebugSlider(
                "Button spacing", "Adjusts the vertical gap between neighboring buttons.",
                settings.buttonSpacingDp, "dp", 0f..32f, defaults.buttonSpacingDp, 31, 1f
            ) { onSettingsChanged(settings.copy(buttonSpacingDp = it)) }
        }
        PreferenceGroup(heading = "Expressive shape") {
            HomeDebugSlider(
                "Resting corners", "Corner radius used by an unselected button at rest.",
                settings.restingCornerDp, "dp", 0f..48f, defaults.restingCornerDp, 47, 1f
            ) { onSettingsChanged(settings.copy(restingCornerDp = it)) }
            HomeDebugSlider(
                "Selected corners", "Corner radius used by the active refresh-rate button.",
                settings.selectedCornerDp, "dp", 0f..48f, defaults.selectedCornerDp, 47, 1f
            ) { onSettingsChanged(settings.copy(selectedCornerDp = it)) }
            HomeDebugSlider(
                "Pressed corners", "Corner radius animated to while a button is held.",
                settings.pressedCornerDp, "dp", 0f..48f, defaults.pressedCornerDp, 47, 1f
            ) { onSettingsChanged(settings.copy(pressedCornerDp = it)) }
            PreferenceTemplate(
                title = "Restore home-screen defaults",
                description = "Reset every option on this page",
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
        description = "$description Default ${default.toInt()} $unit, range " +
            "${range.start.toInt()} to ${range.endInclusive.toInt()} $unit.",
        onValueChange = onValueChanged,
        onValueChangeFinished = {},
        valueRange = range,
        steps = steps,
        increment = increment
    )
}
