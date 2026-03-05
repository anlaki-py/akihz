package akihz.anlaki.dev.presentation

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import akihz.anlaki.dev.utils.Constants

class RefreshRateUI(
    activity: Activity,
    private val onRateSelected: (Float) -> Unit
) {
    private val context: Context = activity.applicationContext
    private val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val defaultBgColor = if (isDark) "#2D2D2D" else "#E0E0E0"
    private val selectedBgColor = if (isDark) "#1E3A5F" else "#3B82F6"
    private val textColor = if (isDark) Color.WHITE else Color.BLACK

    private val rateButtons = mutableMapOf<Float, Button>()
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    lateinit var currentRateText: TextView
        private set
    lateinit var rootLayout: LinearLayout
        private set

    fun createUI(): LinearLayout {
        rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(if (isDark) Color.BLACK else Color.WHITE)
        }

        currentRateText = createCurrentRateText()
        rootLayout.addView(currentRateText)

        Constants.REFRESH_RATES.forEach { hz ->
            val button = createRateButton(hz)
            rateButtons[hz] = button
            rootLayout.addView(button)
        }

        return rootLayout
    }

    private fun createCurrentRateText(): TextView {
        return TextView(context).apply {
            text = "RefreshRate Manager"
            textSize = 24f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
    }

    private fun createRateButton(hz: Float): Button {
        return Button(context).apply {
            text = "${hz.toInt()} Hz"
            textSize = 32f
            setAllCaps(false)
            setTextColor(textColor)
            setPadding(dp(16), dp(48), dp(16), dp(48))
            background = createRoundedBackground(defaultBgColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                performHapticFeedback()
                onRateSelected(hz)
            }
        }
    }

    fun updateCurrentRate(hz: Float) {
        currentRateText.text = "Current: ${hz.toInt()} Hz"
        currentRateText.textSize = 20f
    }

    fun highlightButton(hz: Float) {
        rateButtons.forEach { (_, btn) ->
            btn.background = createRoundedBackground(defaultBgColor)
        }
        val closest = rateButtons.keys.minByOrNull { kotlin.math.abs(it - hz) }
        closest?.let { rateButtons[it]?.background = createRoundedBackground(selectedBgColor) }
    }

    private fun createRoundedBackground(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24).toFloat()
            setColor(Color.parseColor(colorHex))
        }
    }

    private fun performHapticFeedback() {
        vibrator?.let {
            if (!it.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(50)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
