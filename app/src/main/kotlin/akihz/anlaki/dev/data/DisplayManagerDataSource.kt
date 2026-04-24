package akihz.anlaki.dev.data

import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * Hardware-level refresh rate queries using the official Android [DisplayManager] API.
 * This works across all OEMs and does not require elevated permissions.
 */
open class DisplayManagerDataSource(private val context: Context) {

    /**
     * Queries the default display for supported modes and returns
     * a sorted list of unique refresh rates (Hz).
     *
     * @return [Result.Success] with distinct sorted rates, or [Result.Error] on failure.
     */
    open fun getSupportedRefreshRates(): Result<List<Float>> {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                ?: return Result.error(ErrorType.DISPLAY_NOT_FOUND, "Default display not found")

            val modes = display.supportedModes
            val rates = modes
                ?.map { it.refreshRate }
                ?.distinct()
                ?.sorted()
                ?: emptyList()

            if (rates.isEmpty()) {
                Result.error(ErrorType.NO_REFRESH_RATES, "Device reports no supported refresh rates")
            } else {
                Result.success(rates)
            }
        } catch (e: Exception) {
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, e.message ?: "Failed to query display modes")
        }
    }

    /**
     * Returns the currently active refresh rate from the display subsystem.
     *
     * @return [Result.Success] with the active refresh rate in Hz.
     */
    open fun getCurrentRefreshRate(): Result<Float> {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                ?: return Result.error(ErrorType.DISPLAY_NOT_FOUND, "Default display not found")

            Result.success(display.refreshRate)
        } catch (e: Exception) {
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, e.message ?: "Failed to read current refresh rate")
        }
    }
}
