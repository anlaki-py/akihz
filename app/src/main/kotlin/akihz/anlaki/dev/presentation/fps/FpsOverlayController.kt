package akihz.anlaki.dev.presentation.fps

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.data.fps.LayerStat
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
import akihz.anlaki.dev.data.fps.TimeStatsParser
import java.util.Locale
import timber.log.Timber

/**
 * Floating FPS pill with expandable options panel.
 *
 * Same public API as the old View version. Content is Compose hosted in
 * a ComposeView, so the app uses one UI toolkit everywhere. Style and
 * selection state live here as Compose state and persist via prefs.
 */
class FpsOverlayController(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val window = FpsOverlayWindow(appContext)
    private val density = appContext.resources.displayMetrics.density

    private var composeView: ComposeView? = null
    private var overlayLifecycle: FpsOverlayLifecycle? = null
    private var pillText by mutableStateOf("Connecting…")
    private var rows by mutableStateOf(listOf(FpsLayerRow(null, "Auto", true)))
    private var style by mutableStateOf(FpsPillStyle.load())
    private var expanded by mutableStateOf(false)
    private var currentPackage: String? = null
    private var selectedLayer: String? = null
    private var dragRemainderX = 0f
    private var dragRemainderY = 0f

    /** Builds the Compose content, then shows the overlay window. */
    fun attach() {
        if (composeView != null) {
            Timber.d("FPS overlay already attached")
            return
        }
        Timber.i("Attaching FPS overlay")
        style = FpsPillStyle.load()
        selectedLayer = PreferencesHelper.fpsSelectedLayer
        pillText = "Connecting…"
        rows = listOf(FpsLayerRow(null, "Auto", selectedLayer == null))
        expanded = false

        val lifecycle = FpsOverlayLifecycle().also { overlayLifecycle = it }
        lifecycle.resume()
        val view = ComposeView(appContext).apply {
            FpsOverlayViewTrees.install(this, lifecycle, lifecycle, lifecycle)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                FpsOverlayContent(
                    text = pillText,
                    style = style,
                    expanded = expanded,
                    layers = rows,
                    onTapPill = { toggleOptionsPanel() },
                    onDrag = { dxPx, dyPx -> onDragFrame(dxPx, dyPx) },
                    onDragEnd = { keepOnScreen() },
                    onScaleChange = { style = style.copy(scalePercent = it) },
                    onScaleDone = { setScale(it) },
                    onAlphaChange = { style = style.copy(alphaPercent = it) },
                    onAlphaDone = { setAlpha(it) },
                    onUnitChange = { setShowUnit(it) },
                    onLayerSelect = { setSelectedLayer(it) }
                )
            }
        }
        composeView = view
        if (!window.show(view)) {
            composeView = null
            lifecycle.destroy()
            overlayLifecycle = null
            return
        }
        // Also ensure visible after a short delay for first draw
        handler.postDelayed({ keepOnScreen() }, 300)
    }

    /** Hides the overlay and disposes the composition. */
    fun detach() {
        if (composeView == null) return
        window.hide()
        composeView?.disposeComposition()
        composeView = null
        overlayLifecycle?.destroy()
        overlayLifecycle = null
        expanded = false
    }

    /**
     * Shows a status string in the pill.
     * @param text status to display
     */
    fun setStatus(text: String) {
        Timber.d("FPS overlay status: $text")
        handler.post { pillText = text }
    }

    /** Shows FPS for the foreground app and refreshes layer choices. */
    fun display(foreground: String?, layers: List<LayerStat>) {
        if (foreground == null) {
            setStatus(FpsPillRenderer.formatFps(0.0, style.showUnit))
            handler.post { rows = autoRow() }
            return
        }
        if (foreground != currentPackage) {
            currentPackage = foreground
            // Do not clear selectedLayer on package change; keep user choice
        }
        if (layers.isEmpty()) {
            setStatus(FpsPillRenderer.formatFps(0.0, style.showUnit))
            handler.post { rows = autoRow() }
            return
        }
        val chosen = chooseOverlayLayer(layers, selectedLayer)
        val shownFps = TimeStatsParser.displayFps(chosen.fps, displayRefreshRate())
        val text = FpsPillRenderer.formatFps(shownFps, style.showUnit)
        handler.post {
            pillText = text
            rows = layerRows(layers)
        }
    }

    /**
     * Saves overlay scale and resizes the pill.
     * @param percent size percent from 50 to 200
     */
    fun setScale(percent: Int) {
        val clamped = percent.coerceIn(SCALE_MIN, SCALE_MAX)
        style = style.copy(scalePercent = clamped)
        PreferencesHelper.fpsOverlayScale = clamped
        Timber.i("FPS overlay scale $clamped%")
        handler.post {
            style = style.copy(scalePercent = clamped)
            keepOnScreen()
        }
    }

    /**
     * Saves the picked layer and updates the radio state.
     * @param stableName layer name, or null for Auto
     */
    fun setSelectedLayer(stableName: String?) {
        selectedLayer = stableName
        PreferencesHelper.fpsSelectedLayer = stableName
        Timber.i("FPS layer selected: ${stableName ?: "Auto"}")
        handler.post {
            rows = rows.map { it.copy(selected = it.stableName == selectedLayer) }
        }
    }

    /** Reloads the picked layer from prefs and updates the radio state. */
    fun updateSelectedLayerFromPrefs() {
        selectedLayer = PreferencesHelper.fpsSelectedLayer
        handler.post {
            rows = rows.map { it.copy(selected = it.stableName == selectedLayer) }
        }
    }

    /**
     * Reloads every overlay style value from prefs and applies it to the pill.
     *
     * Called when the app process changes style settings while the service runs.
     */
    fun applyStyleFromPrefs() {
        style = FpsPillStyle.load()
        Timber.i("FPS overlay style alpha=${style.alphaPercent} unit=${style.showUnit} color=${style.pillColor}")
    }

    /**
     * Updates the pill fill opacity, keeping text fully opaque.
     *
     * @param percent opacity from [OverlayStyle.ALPHA_MIN] to [OverlayStyle.ALPHA_MAX]
     */
    fun setAlpha(percent: Int) {
        val clamped = percent.coerceIn(OverlayStyle.ALPHA_MIN, OverlayStyle.ALPHA_MAX)
        style = style.copy(alphaPercent = clamped)
        PreferencesHelper.fpsOverlayAlpha = clamped
        Timber.i("FPS overlay alpha $clamped%")
    }

    /**
     * Shows or hides the FPS label next to the value.
     *
     * @param enabled true for "60.0 FPS", false for "60.0"
     */
    fun setShowUnit(enabled: Boolean) {
        style = style.copy(showUnit = enabled)
        PreferencesHelper.fpsShowUnit = enabled
    }

    /**
     * Changes the pill background color.
     *
     * @param color new pill color
     */
    fun setPillColor(color: OverlayPillColor) {
        style = style.copy(pillColor = color)
        PreferencesHelper.fpsPillColor = color
    }

    /**
     * Switches the pill between pill and rectangle shape.
     *
     * @param rectangular true for rectangle, false for pill
     */
    fun setRectShape(rectangular: Boolean) {
        style = style.copy(rectangularShape = rectangular)
        PreferencesHelper.fpsRectShape = rectangular
    }

    /**
     * Shows or hides the pill outline.
     *
     * @param enabled true to draw the edge, false for a flat pill
     */
    fun setPillOutline(enabled: Boolean) {
        style = style.copy(pillOutline = enabled)
        PreferencesHelper.fpsPillOutline = enabled
    }

    private fun toggleOptionsPanel() {
        expanded = !expanded
        Timber.i("FPS overlay options ${if (expanded) "opened" else "closed"}")
        window.refit()
        handler.postDelayed({ keepOnScreen() }, 100)
    }

    private fun onDragFrame(dxPx: Float, dyPx: Float) {
        val pos = window.position() ?: return
        dragRemainderX += dxPx / density
        dragRemainderY += dyPx / density
        val dx = dragRemainderX.toInt()
        val dy = dragRemainderY.toInt()
        dragRemainderX -= dx
        dragRemainderY -= dy
        if (dx != 0 || dy != 0) {
            window.moveTo(pos.first + dx, pos.second + dy)
        }
    }

    private fun autoRow(): List<FpsLayerRow> =
        listOf(FpsLayerRow(null, "Auto", selectedLayer == null))

    private fun layerRows(layers: List<LayerStat>): List<FpsLayerRow> {
        val rate = displayRefreshRate()
        val out = ArrayList<FpsLayerRow>(layers.size + 1)
        out += FpsLayerRow(null, "Auto", selectedLayer == null)
        for (layer in layers) {
            val fps = TimeStatsParser.displayFps(layer.fps, rate)
            out += FpsLayerRow(
                stableName = layer.stableName,
                label = layer.shortName() + String.format(Locale.US, "  %.1f", fps),
                selected = layer.stableName == selectedLayer
            )
        }
        return out
    }

    private fun displayRefreshRate(): Double =
        FpsPillRenderer.displayRefreshRate(appContext)

    private fun keepOnScreen() = window.keepOnScreen()

    companion object {
        const val SCALE_MIN = 50
        const val SCALE_MAX = 200
    }
}
