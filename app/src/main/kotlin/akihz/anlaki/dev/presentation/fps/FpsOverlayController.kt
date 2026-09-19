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
    // Plain flags, not Compose state: flipping Compose state here would
    // recompose the pill on every drag frame and defeat the pause below.
    private var isDragging = false
    private var pendingForeground: String? = null
    private var pendingLayers: List<LayerStat>? = null
    private var hasPendingDisplay = false
    private var pendingStatus: String? = null
    private var cachedRefreshRate = 0.0
    private var cachedRateAtMs = 0L
    private var positionBeforePanel: Pair<Int, Int>? = null

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
        isDragging = false
        hasPendingDisplay = false
        pendingForeground = null
        pendingLayers = null
        pendingStatus = null
        positionBeforePanel = null

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
                    onDragEnd = { onDragEnd() },
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
        isDragging = false
        hasPendingDisplay = false
        pendingForeground = null
        pendingLayers = null
        pendingStatus = null
        positionBeforePanel = null
        window.hide()
        composeView?.disposeComposition()
        composeView = null
        overlayLifecycle?.destroy()
        overlayLifecycle = null
        expanded = false
    }

    /**
     * Shows a status string in the pill.
     *
     * Deferred while dragging so the text change does not recompose
     * the pill mid-gesture and stutter the move.
     * @param text status to display
     */
    fun setStatus(text: String) {
        Timber.d("FPS overlay status: $text")
        if (isDragging) {
            pendingStatus = text
            return
        }
        handler.post { pillText = text }
    }

    /**
     * Shows FPS for the foreground app and refreshes layer choices.
     *
     * While dragging, the latest sample is stashed and applied on drag
     * end. Updating pillText and rows every 500 ms mid-drag recomposes
     * the pill under the finger, which is the lag source.
     */
    fun display(foreground: String?, layers: List<LayerStat>) {
        if (isDragging) {
            pendingForeground = foreground
            pendingLayers = layers
            hasPendingDisplay = true
            return
        }
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
        if (!expanded) {
            positionBeforePanel = window.position()
            expanded = true
            Timber.i("FPS overlay options opened")
            window.refit()
            handler.postDelayed({ keepOnScreen() }, 100)
            return
        }
        expanded = false
        Timber.i("FPS overlay options closed")
        window.refit()
        val restore = positionBeforePanel
        positionBeforePanel = null
        if (restore != null) {
            handler.postDelayed({
                window.moveTo(restore.first, restore.second)
                keepOnScreen()
            }, 100)
        } else {
            handler.postDelayed({ keepOnScreen() }, 100)
        }
    }

    private fun onDragFrame(dxPx: Float, dyPx: Float) {
        // First drag frame pauses text updates until onDragEnd flushes them.
        // Deltas arrive in physical pixels, same units as window params.
        isDragging = true
        if (expanded) {
            positionBeforePanel = null
        }
        val pos = window.position() ?: return
        dragRemainderX += dxPx
        dragRemainderY += dyPx
        val dx = Math.round(dragRemainderX)
        val dy = Math.round(dragRemainderY)
        dragRemainderX -= dx
        dragRemainderY -= dy
        if (dx != 0 || dy != 0) {
            window.moveTo(pos.first + dx, pos.second + dy)
        }
    }

    private fun onDragEnd() {
        isDragging = false
        dragRemainderX = 0f
        dragRemainderY = 0f
        keepOnScreen()
        // Apply the latest sample skipped mid-drag, so the pill is fresh
        // but never recomposed under the finger.
        val status = pendingStatus
        pendingStatus = null
        if (status != null) {
            handler.post { pillText = status }
        }
        if (hasPendingDisplay) {
            hasPendingDisplay = false
            val foreground = pendingForeground
            val layers = pendingLayers
            pendingForeground = null
            pendingLayers = null
            if (layers != null) display(foreground, layers)
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

    private fun displayRefreshRate(): Double {
        // DisplayManager lookup on every 500 ms sample runs on the main
        // thread, so cache it briefly to keep drag frames light.
        val now = android.os.SystemClock.uptimeMillis()
        if (now - cachedRateAtMs < RATE_CACHE_MS && cachedRefreshRate > 0) {
            return cachedRefreshRate
        }
        val rate = FpsPillRenderer.displayRefreshRate(appContext)
        if (rate > 0) {
            cachedRefreshRate = rate
            cachedRateAtMs = now
        }
        return rate
    }

    private fun keepOnScreen() = window.keepOnScreen()

    companion object {
        const val SCALE_MIN = 50
        const val SCALE_MAX = 200
        private const val RATE_CACHE_MS = 5_000L
    }
}
