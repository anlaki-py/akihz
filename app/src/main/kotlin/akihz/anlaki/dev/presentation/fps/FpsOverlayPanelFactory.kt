package akihz.anlaki.dev.presentation.fps

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import akihz.anlaki.dev.data.fps.OverlayGeometry
import akihz.anlaki.dev.data.fps.OverlayStyle

/**
 * Builds the expandable options panel inside the FPS overlay.
 *
 * Owns view construction only. State and callbacks stay with the
 * controller, which receives every created view through [PanelViews].
 */
internal object FpsOverlayPanelFactory {

    /** Every view the controller must keep a reference to. */
    data class PanelViews(
        val choices: RadioGroup,
        val sizeLabel: TextView,
        val sizeSlider: SeekBar,
        val alphaLabel: TextView,
        val alphaSlider: SeekBar,
        val unitBox: CheckBox,
        val panel: LinearLayout
    )

    /**
     * Builds the panel and its controls.
     *
     * @param style initial values for sliders and checkbox
     * @param onScaleStop invoked when the size slider settles
     * @param onAlphaStop invoked when the opacity slider settles
     * @param onUnit invoked when the label checkbox flips
     */
    fun build(
        context: Context,
        style: FpsPillStyle,
        onScaleStop: (Int) -> Unit,
        onAlphaStop: (Int) -> Unit,
        onUnit: (Boolean) -> Unit
    ): PanelViews {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int): Int = Math.round(value * density)

        val choices = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
        }
        val sizeLabel = TextView(context).apply {
            setTextColor(Color.WHITE)
            text = "Overlay size: ${style.scalePercent}%"
        }
        val slider = SeekBar(context).apply {
            min = FpsOverlayController.SCALE_MIN
            max = FpsOverlayController.SCALE_MAX
            progress = style.scalePercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                    sizeLabel.text = "Overlay size: $progress%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    val p = s?.progress ?: return
                    onScaleStop(p)
                }
            })
        }

        val alphaLabel = TextView(context).apply {
            setTextColor(Color.WHITE)
            text = "Overlay opacity: ${style.alphaPercent}%"
        }
        val alphaSlider = SeekBar(context).apply {
            min = OverlayStyle.ALPHA_MIN
            max = OverlayStyle.ALPHA_MAX
            progress = style.alphaPercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                    alphaLabel.text = "Overlay opacity: $progress%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    val p = s?.progress ?: return
                    onAlphaStop(p)
                }
            })
        }
        val unitBox = CheckBox(context).apply {
            text = "Show FPS label"
            setTextColor(Color.WHITE)
            isChecked = style.showUnit
            minHeight = dp(44)
            setOnCheckedChangeListener { _, checked -> onUnit(checked) }
        }

        val optionsContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(choices)
            addView(sizeLabel)
            addView(slider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
            addView(alphaLabel)
            addView(alphaSlider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
            addView(unitBox)
        }
        val scroll = ScrollView(context).apply {
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
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(0xE6000000.toInt())
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), 0x99FFFFFF.toInt())
            }
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels
            val panelWidth = OverlayGeometry.fitPanelSize(dp(320), screenWidth, dp(24))
            val panelHeight = OverlayGeometry.fitPanelSize(dp(420), screenHeight, dp(120))
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
            this.layoutParams = LinearLayout.LayoutParams(panelWidth, panelHeight)
        }
        return PanelViews(choices, sizeLabel, slider, alphaLabel, alphaSlider, unitBox, panel)
    }
}
