package akihz.anlaki.dev.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import akihz.anlaki.dev.data.fps.LayerStat
import akihz.anlaki.dev.data.fps.OverlayGeometry
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
import akihz.anlaki.dev.data.fps.TimeStatsParser
import timber.log.Timber
import java.util.Locale

/**
 * Floating FPS pill with expandable options panel.
 *
 * Matches the original Surface FPS Monitor behavior: drag to move,
 * tap to expand layer, size, opacity, and label options persisted via prefs.
 */
class FpsOverlayController(private val context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var overlay: LinearLayout? = null
    private var fpsView: TextView? = null
    private var optionsPanel: LinearLayout? = null
    private var layerChoices: RadioGroup? = null
    private var overlaySizeLabel: TextView? = null
    private var overlaySizeSlider: SeekBar? = null
    private var overlayAlphaLabel: TextView? = null
    private var overlayAlphaSlider: SeekBar? = null
    private var showUnitBox: CheckBox? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var currentPackage: String? = null
    private var selectedLayer: String? = null
    private var shownLayerKeys: List<String> = emptyList()
    private var optionsExpanded = false
    private var scalePercent = PreferencesHelper.fpsOverlayScale
    private var alphaPercent = OverlayStyle.ALPHA_DEFAULT
    private var showUnit = true
    private var pillColor = OverlayPillColor.Black
    private var rectangularShape = false
    private var cornerRadius = OverlayStyle.RECT_RADIUS_DP
    private var pillOutline = true
    private var pillBackground: GradientDrawable? = null

    fun attach() {
        if (overlay != null) {
            Timber.d("FPS overlay already attached")
            return
        }
        Timber.i("Attaching FPS overlay")
        val density = appContext.resources.displayMetrics.density

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
        pillView.setOnTouchListener(DragTouchListener())
        layout.addView(pillView)

        val choices = RadioGroup(appContext).apply {
            orientation = RadioGroup.VERTICAL
        }
        val sizeLabel = TextView(appContext).apply {
            setTextColor(Color.WHITE)
            text = "Overlay size: ${PreferencesHelper.fpsOverlayScale}%"
        }
        val slider = SeekBar(appContext).apply {
            min = SCALE_MIN
            max = SCALE_MAX
            progress = PreferencesHelper.fpsOverlayScale
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                    sizeLabel.text = "Overlay size: $progress%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    val p = s?.progress ?: return
                    setScale(p)
                }
            })
        }

        val alphaLabel = TextView(appContext).apply {
            setTextColor(Color.WHITE)
            text = "Overlay opacity: ${PreferencesHelper.fpsOverlayAlpha}%"
        }
        val alphaSlider = SeekBar(appContext).apply {
            min = OverlayStyle.ALPHA_MIN
            max = OverlayStyle.ALPHA_MAX
            progress = PreferencesHelper.fpsOverlayAlpha
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                    alphaLabel.text = "Overlay opacity: $progress%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    val p = s?.progress ?: return
                    setAlpha(p)
                }
            })
        }
        val unitBox = CheckBox(appContext).apply {
            text = "Show FPS label"
            setTextColor(Color.WHITE)
            isChecked = PreferencesHelper.fpsShowUnit
            minHeight = dp(44)
            setOnCheckedChangeListener { _, checked -> setShowUnit(checked) }
        }

        val optionsContent = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            addView(choices)
            addView(sizeLabel)
            addView(slider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
            addView(alphaLabel)
            addView(alphaSlider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
            addView(unitBox)
        }
        val scroll = ScrollView(appContext).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            addView(
                optionsContent,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val panel = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(0xE6000000.toInt())
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), 0x99FFFFFF.toInt())
            }
            val screenWidth = appContext.resources.displayMetrics.widthPixels
            val screenHeight = appContext.resources.displayMetrics.heightPixels
            val panelWidth = OverlayGeometry.fitPanelSize(dp(320), screenWidth, dp(24))
            val panelHeight = OverlayGeometry.fitPanelSize(dp(420), screenHeight, dp(120))
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
            this.layoutParams = LinearLayout.LayoutParams(panelWidth, panelHeight)
        }
        layout.addView(panel)

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(80)
        }

        overlay = layout
        fpsView = pillView
        optionsPanel = panel
        layerChoices = choices
        overlaySizeLabel = sizeLabel
        overlaySizeSlider = slider
        overlayAlphaLabel = alphaLabel
        overlayAlphaSlider = alphaSlider
        showUnitBox = unitBox
        scalePercent = PreferencesHelper.fpsOverlayScale
        selectedLayer = PreferencesHelper.fpsSelectedLayer
        alphaPercent = PreferencesHelper.fpsOverlayAlpha
        showUnit = PreferencesHelper.fpsShowUnit
        pillColor = PreferencesHelper.fpsPillColor
        rectangularShape = PreferencesHelper.fpsRectShape
        cornerRadius = PreferencesHelper.fpsCornerRadius
        pillOutline = PreferencesHelper.fpsPillOutline

        // Apply scale and style after views are assigned
        applyScale(scalePercent)
        applyStyle()

        try {
            windowManager.addView(layout, windowParams)
            Timber.i("FPS overlay added at ${windowParams?.x},${windowParams?.y}")
        } catch (e: Exception) {
            Timber.w(e, "Failed to add FPS overlay")
            overlay = null
            fpsView = null
            optionsPanel = null
            layerChoices = null
            return
        }
        // Post keepOnScreen after layout pass
        layout.post { keepOnScreen() }
        // Also ensure visible after a short delay for first draw
        handler.postDelayed({ keepOnScreen() }, 300)
    }

    fun detach() {
        val view = overlay ?: return
        Timber.i("Detaching FPS overlay")
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {}
        overlay = null
        fpsView = null
        pillBackground = null
        optionsPanel = null
        layerChoices = null
        overlaySizeLabel = null
        overlaySizeSlider = null
        overlayAlphaLabel = null
        overlayAlphaSlider = null
        showUnitBox = null
        windowParams = null
        shownLayerKeys = emptyList()
        optionsExpanded = false
    }

    fun setStatus(text: String) {
        Timber.d("FPS overlay status: $text")
        handler.post {
            fpsView?.text = text
            refitPillToText()
        }
    }

    fun display(foreground: String?, layers: List<LayerStat>) {
        if (foreground == null) {
            setStatus(OverlayStyle.formatFps(0.0, showUnit))
            handler.post { updateChoices(emptyList()) }
            return
        }
        if (foreground != currentPackage) {
            currentPackage = foreground
            // Do not clear selectedLayer on package change; keep user choice
        }
        if (layers.isEmpty()) {
            setStatus(OverlayStyle.formatFps(0.0, showUnit))
            handler.post { updateChoices(emptyList()) }
            return
        }
        var chosen = layers.first()
        val sel = selectedLayer ?: PreferencesHelper.fpsSelectedLayer
        if (sel != null) {
            for (layer in layers) {
                if (layer.stableName == sel) {
                    chosen = layer
                    break
                }
            }
        }
        val refreshRate = displayRefreshRate()
        val shownFps = TimeStatsParser.displayFps(chosen.fps, refreshRate)
        val text = OverlayStyle.formatFps(shownFps, showUnit)
        handler.post {
            fpsView?.text = text
            refitPillToText()
            updateChoices(layers)
        }
    }

    fun setScale(percent: Int) {
        val clamped = percent.coerceIn(SCALE_MIN, SCALE_MAX)
        scalePercent = clamped
        PreferencesHelper.fpsOverlayScale = clamped
        Timber.i("FPS overlay scale $clamped%")
        handler.post {
            overlaySizeSlider?.progress = clamped
            overlaySizeLabel?.text = "Overlay size: $clamped%"
            applyScale(clamped)
            refitPillToText()
            keepOnScreen()
        }
    }

    fun setSelectedLayer(stableName: String?) {
        selectedLayer = stableName
        PreferencesHelper.fpsSelectedLayer = stableName
        Timber.i("FPS layer selected: ${stableName ?: "Auto"}")
        // Update radio check state
        handler.post { updateChoicesForSelection() }
    }

    fun updateSelectedLayerFromPrefs() {
        selectedLayer = PreferencesHelper.fpsSelectedLayer
        handler.post { updateChoicesForSelection() }
    }

    /**
     * Reloads every overlay style value from prefs and applies it to the pill.
     *
     * Called when the app process changes style settings while the service runs.
     */
    fun applyStyleFromPrefs() {
        alphaPercent = PreferencesHelper.fpsOverlayAlpha
        showUnit = PreferencesHelper.fpsShowUnit
        pillColor = PreferencesHelper.fpsPillColor
        rectangularShape = PreferencesHelper.fpsRectShape
        cornerRadius = PreferencesHelper.fpsCornerRadius
        pillOutline = PreferencesHelper.fpsPillOutline
        Timber.i("FPS overlay style alpha=$alphaPercent unit=$showUnit color=$pillColor")
        handler.post {
            overlayAlphaSlider?.progress = alphaPercent
            overlayAlphaLabel?.text = "Overlay opacity: $alphaPercent%"
            val box = showUnitBox
            if (box != null && box.isChecked != showUnit) box.isChecked = showUnit
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
        alphaPercent = clamped
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
        showUnit = enabled
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
        pillColor = color
        PreferencesHelper.fpsPillColor = color
        handler.post { applyStyle() }
    }

    /**
     * Switches the pill between pill and rectangle shape.
     *
     * @param rectangular true for rectangle, false for pill
     */
    fun setRectShape(rectangular: Boolean) {
        rectangularShape = rectangular
        PreferencesHelper.fpsRectShape = rectangular
        handler.post { applyStyle() }
    }

    /**
     * Shows or hides the pill outline.
     *
     * @param enabled true to draw the edge, false for a flat pill
     */
    fun setPillOutline(enabled: Boolean) {
        pillOutline = enabled
        PreferencesHelper.fpsPillOutline = enabled
        handler.post { applyStyle() }
    }

    private fun toggleOptionsPanel() {
        optionsExpanded = !optionsExpanded
        Timber.i("FPS overlay options ${if (optionsExpanded) "opened" else "closed"}")
        optionsPanel?.visibility = if (optionsExpanded) View.VISIBLE else View.GONE
        overlay?.post { keepOnScreen() }
        handler.postDelayed({ keepOnScreen() }, 100)
    }

    private fun updateChoices(layers: List<LayerStat>) {
        val group = layerChoices ?: return
        val keys = layers.map { it.stableName }
        if (keys == shownLayerKeys && group.childCount == keys.size + 1) {
            // Update FPS values in place
            for (i in 1 until group.childCount) {
                val item = group.getChildAt(i) as? RadioButton ?: continue
                val layer = layers.getOrNull(i - 1) ?: continue
                val fps = TimeStatsParser.displayFps(layer.fps, displayRefreshRate())
                item.text = layer.shortName() + String.format(Locale.US, "  %.1f", fps)
            }
            return
        }
        shownLayerKeys = keys
        group.setOnCheckedChangeListener(null)
        group.removeAllViews()

        val automatic = RadioButton(appContext).apply {
            text = "Auto"
            id = View.generateViewId()
            tag = null
            styleLayerChoice(this)
        }
        group.addView(automatic)
        if (selectedLayer == null) automatic.isChecked = true

        for (layer in layers) {
            val item = RadioButton(appContext).apply {
                id = View.generateViewId()
                tag = layer.stableName
                val fps = TimeStatsParser.displayFps(layer.fps, displayRefreshRate())
                text = layer.shortName() + String.format(Locale.US, "  %.1f", fps)
                styleLayerChoice(this)
                if (layer.stableName == selectedLayer) isChecked = true
            }
            group.addView(item)
        }
        group.setOnCheckedChangeListener { g, checkedId ->
            val checked = g.findViewById<View>(checkedId)
            val tag = checked?.tag as? String
            selectedLayer = tag
            PreferencesHelper.fpsSelectedLayer = tag
            Timber.i("FPS layer selection: ${tag ?: "Auto"}")
        }
        keepOnScreen()
    }

    private fun updateChoicesForSelection() {
        val group = layerChoices ?: return
        for (i in 0 until group.childCount) {
            val item = group.getChildAt(i) as? RadioButton ?: continue
            val tag = item.tag as? String
            item.isChecked = tag == selectedLayer
            if (i == 0 && selectedLayer == null) item.isChecked = true
        }
    }

    private fun styleLayerChoice(item: RadioButton) {
        item.setTextColor(Color.WHITE)
        item.textSize = 14f
        item.minHeight = dp(44)
        item.setPadding(dp(10), dp(4), dp(10), dp(4))
    }

    /**
     * Forces the pill background to fit its text.
     *
     * The overlay window can keep a width measured for an earlier, longer
     * text such as "Connecting...", leaving a stretched pill behind short
     * readings like "0.0". Measuring the pill unconstrained and pinning the
     * exact width makes the background track the text on every update.
     */
    private fun refitPillToText() {
        val pill = fpsView ?: return
        pill.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val fitted = pill.measuredWidth.coerceAtLeast(1)
        val lp = pill.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        if (lp.width != fitted) {
            lp.width = fitted
            pill.layoutParams = lp
        }
        refitWindow()
    }

    /** Re-runs window layout so a shrink actually reaches the screen. */
    private fun refitWindow() {
        val root = overlay ?: return
        val params = windowParams ?: return
        if (!root.isAttachedToWindow) return
        try {
            windowManager.updateViewLayout(root, params)
        } catch (_: Exception) {}
    }

    private fun applyScale(percent: Int) {
        val view = fpsView ?: return
        val factor = percent / 100f
        view.textSize = 14f * factor
        view.setPadding(
            dp(OverlayGeometry.scaleDimension(16, percent)),
            dp(OverlayGeometry.scaleDimension(12, percent)),
            dp(OverlayGeometry.scaleDimension(16, percent)),
            dp(OverlayGeometry.scaleDimension(12, percent))
        )
    }

    private fun applyStyle() {
        val view = fpsView ?: return
        val background = pillBackground ?: return
        background.setColor(OverlayStyle.pillColor(pillColor, alphaPercent))
        background.cornerRadius = dp(OverlayStyle.pillRadiusDp(rectangularShape, cornerRadius)).toFloat()
        if (pillOutline) {
            background.setStroke(dp(1), OverlayStyle.strokeColor(pillColor))
        } else {
            background.setStroke(0, OverlayStyle.strokeColor(pillColor))
        }
        view.setTextColor(OverlayStyle.textColor(pillColor))
    }

    private fun displayRefreshRate(): Double {
        val manager = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = manager?.getDisplay(Display.DEFAULT_DISPLAY)
        return display?.refreshRate?.toDouble() ?: 0.0
    }

    private fun keepOnScreen() {
        val view = overlay ?: return
        val params = windowParams ?: return
        if (!view.isAttachedToWindow) return
        val screenWidth = appContext.resources.displayMetrics.widthPixels
        val screenHeight = appContext.resources.displayMetrics.heightPixels
        val w = if (view.width > 0) view.width else view.measuredWidth
        val h = if (view.height > 0) view.height else view.measuredHeight
        if (w == 0 || h == 0) {
            view.post { keepOnScreen() }
            return
        }
        val clampedX = OverlayGeometry.clampPosition(params.x, w, screenWidth)
        val clampedY = OverlayGeometry.clampPosition(params.y, h, screenHeight)
        if (clampedX != params.x || clampedY != params.y) {
            params.x = clampedX
            params.y = clampedY
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    private fun dp(value: Int): Int =
        Math.round(value * appContext.resources.displayMetrics.density)

    private inner class DragTouchListener : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var downX = 0f
        private var downY = 0f
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = windowParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - downX
                    val deltaY = event.rawY - downY
                    if (kotlin.math.hypot(deltaX.toDouble(), deltaY.toDouble()) > touchSlop) moved = true
                    params.x = startX + Math.round(event.rawX - downX)
                    params.y = startY + Math.round(event.rawY - downY)
                    try {
                        overlay?.let { windowManager.updateViewLayout(it, params) }
                    } catch (_: Exception) {}
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        keepOnScreen()
                    } else {
                        view.performClick()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> return true
            }
            return false
        }
    }

    companion object {
        const val SCALE_MIN = 50
        const val SCALE_MAX = 200
    }
}
