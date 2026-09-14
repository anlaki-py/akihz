package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.PreferencesHelper
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import akihz.anlaki.dev.data.fps.LayerStat
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
import akihz.anlaki.dev.data.fps.TimeStatsParser
import timber.log.Timber

/**
 * Floating FPS pill with expandable options panel.
 *
 * Coordinates three collaborators: [FpsPillRenderer] paints the pill,
 * [FpsLayerChoices] owns the layer pick list, and [FpsOverlayPanelFactory]
 * builds the options views. Window hosting and drag handling stay here.
 * Style values persist via prefs.
 */
class FpsOverlayController(context: Context) {
    private val appContext = context.applicationContext
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val window = FpsOverlayWindow(appContext)
    private val layerChoices = FpsLayerChoices(appContext)

    private var fpsView: TextView? = null
    private var optionsPanel: LinearLayout? = null
    private var layerGroup: RadioGroup? = null
    private var overlaySizeLabel: TextView? = null
    private var overlaySizeSlider: SeekBar? = null
    private var overlayAlphaLabel: TextView? = null
    private var overlayAlphaSlider: SeekBar? = null
    private var showUnitBox: CheckBox? = null
    private var currentPackage: String? = null
    private var selectedLayer: String? = null
    private var optionsExpanded = false
    private var style = FpsPillStyle.load()
    private var pillBackground: GradientDrawable? = null

    /** Builds the pill and panel, then shows the overlay window. */
    fun attach() {
        if (window.root != null) {
            Timber.d("FPS overlay already attached")
            return
        }
        Timber.i("Attaching FPS overlay")

        val layout = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
        }

        val pillView = TextView(appContext).apply {
            text = "Connecting…"
            gravity = Gravity.CENTER
            includeFontPadding = false
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().also { pillBackground = it }
        }
        pillView.setOnClickListener { toggleOptionsPanel() }
        pillView.setOnTouchListener(window.dragListener { keepOnScreen() })
        layout.addView(pillView)

        style = FpsPillStyle.load()
        selectedLayer = PreferencesHelper.fpsSelectedLayer
        val views = FpsOverlayPanelFactory.build(
            context = appContext,
            style = style,
            onScaleStop = { setScale(it) },
            onAlphaStop = { setAlpha(it) },
            onUnit = { setShowUnit(it) }
        )
        layout.addView(views.panel)

        fpsView = pillView
        optionsPanel = views.panel
        layerGroup = views.choices
        overlaySizeLabel = views.sizeLabel
        overlaySizeSlider = views.sizeSlider
        overlayAlphaLabel = views.alphaLabel
        overlayAlphaSlider = views.alphaSlider
        showUnitBox = views.unitBox

        // Apply scale and style after views are assigned
        fpsView?.let { FpsPillRenderer.applyScale(appContext, it, style) }
        applyStyle()

        if (!window.show(layout)) {
            fpsView = null
            optionsPanel = null
            layerGroup = null
            return
        }
        // Also ensure visible after a short delay for first draw
        handler.postDelayed({ keepOnScreen() }, 300)
    }

    /** Hides the overlay and clears cached views. */
    fun detach() {
        if (window.root == null) return
        window.hide()
        fpsView = null
        pillBackground = null
        optionsPanel = null
        layerGroup = null
        overlaySizeLabel = null
        overlaySizeSlider = null
        overlayAlphaLabel = null
        overlayAlphaSlider = null
        showUnitBox = null
        layerChoices.reset()
        optionsExpanded = false
    }

    /**
     * Shows a status string in the pill.
     * @param text status to display
     */
    fun setStatus(text: String) {
        Timber.d("FPS overlay status: $text")
        handler.post {
            fpsView?.text = text
            refitPillToText()
        }
    }

    /** Shows FPS for the foreground app and refreshes layer choices. */
    fun display(foreground: String?, layers: List<LayerStat>) {
        if (foreground == null) {
            setStatus(FpsPillRenderer.formatFps(0.0, style.showUnit))
            handler.post { updateChoices(emptyList()) }
            return
        }
        if (foreground != currentPackage) {
            currentPackage = foreground
            // Do not clear selectedLayer on package change; keep user choice
        }
        if (layers.isEmpty()) {
            setStatus(FpsPillRenderer.formatFps(0.0, style.showUnit))
            handler.post { updateChoices(emptyList()) }
            return
        }
        val chosen = layerChoices.choose(layers, selectedLayer)
        val refreshRate = displayRefreshRate()
        val shownFps = TimeStatsParser.displayFps(chosen.fps, refreshRate)
        val text = FpsPillRenderer.formatFps(shownFps, style.showUnit)
        handler.post {
            fpsView?.text = text
            refitPillToText()
            updateChoices(layers)
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
            overlaySizeSlider?.progress = clamped
            overlaySizeLabel?.text = "Overlay size: $clamped%"
            fpsView?.let { FpsPillRenderer.applyScale(appContext, it, style) }
            refitPillToText()
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
        // Update radio check state
        handler.post {
            layerGroup?.let { layerChoices.updateSelection(it, selectedLayer) }
        }
    }

    /** Reloads the picked layer from prefs and updates the radio state. */
    fun updateSelectedLayerFromPrefs() {
        selectedLayer = PreferencesHelper.fpsSelectedLayer
        handler.post {
            layerGroup?.let { layerChoices.updateSelection(it, selectedLayer) }
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
        handler.post {
            overlayAlphaSlider?.progress = style.alphaPercent
            overlayAlphaLabel?.text = "Overlay opacity: ${style.alphaPercent}%"
            val box = showUnitBox
            if (box != null && box.isChecked != style.showUnit) box.isChecked = style.showUnit
            applyStyle()
        }
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
        handler.post {
            overlayAlphaSlider?.progress = clamped
            overlayAlphaLabel?.text = "Overlay opacity: $clamped%"
            applyStyle()
        }
    }

    /**
     * Shows or hides the FPS label next to the value.
     *
     * @param enabled true for "60.0 FPS", false for "60.0"
     */
    fun setShowUnit(enabled: Boolean) {
        style = style.copy(showUnit = enabled)
        PreferencesHelper.fpsShowUnit = enabled
        handler.post {
            val box = showUnitBox
            if (box != null && box.isChecked != enabled) box.isChecked = enabled
            applyStyle()
            refitPillToText()
        }
    }

    /**
     * Changes the pill background color.
     *
     * @param color new pill color
     */
    fun setPillColor(color: OverlayPillColor) {
        style = style.copy(pillColor = color)
        PreferencesHelper.fpsPillColor = color
        handler.post { applyStyle() }
    }

    /**
     * Switches the pill between pill and rectangle shape.
     *
     * @param rectangular true for rectangle, false for pill
     */
    fun setRectShape(rectangular: Boolean) {
        style = style.copy(rectangularShape = rectangular)
        PreferencesHelper.fpsRectShape = rectangular
        handler.post { applyStyle() }
    }

    /**
     * Shows or hides the pill outline.
     *
     * @param enabled true to draw the edge, false for a flat pill
     */
    fun setPillOutline(enabled: Boolean) {
        style = style.copy(pillOutline = enabled)
        PreferencesHelper.fpsPillOutline = enabled
        handler.post { applyStyle() }
    }

    private fun toggleOptionsPanel() {
        optionsExpanded = !optionsExpanded
        Timber.i("FPS overlay options ${if (optionsExpanded) "opened" else "closed"}")
        optionsPanel?.visibility = if (optionsExpanded) View.VISIBLE else View.GONE
        window.root?.post { keepOnScreen() }
        handler.postDelayed({ keepOnScreen() }, 100)
    }

    private fun updateChoices(layers: List<LayerStat>) {
        val group = layerGroup ?: return
        layerChoices.updateChoices(
            group = group,
            layers = layers,
            selectedLayer = selectedLayer,
            onSelect = {
                selectedLayer = it
                PreferencesHelper.fpsSelectedLayer = it
            },
            refreshRate = { displayRefreshRate() },
            onChanged = { keepOnScreen() }
        )
    }

    private fun applyStyle() {
        val view = fpsView ?: return
        val background = pillBackground ?: return
        FpsPillRenderer.applyStyle(appContext, view, background, style)
    }

    /**
     * Forces the pill background to fit its text, then re-runs window layout
     * so a shrink actually reaches the screen.
     */
    private fun refitPillToText() {
        val pill = fpsView ?: return
        if (FpsPillRenderer.refitPillToText(pill)) refitWindow()
    }

    /** Re-runs window layout so a shrink actually reaches the screen. */
    private fun refitWindow() = window.refit()

    private fun displayRefreshRate(): Double =
        FpsPillRenderer.displayRefreshRate(appContext)

    private fun keepOnScreen() = window.keepOnScreen()

    private fun dp(value: Int): Int = window.dp(value)

    companion object {
        const val SCALE_MIN = 50
        const val SCALE_MAX = 200
    }
}
