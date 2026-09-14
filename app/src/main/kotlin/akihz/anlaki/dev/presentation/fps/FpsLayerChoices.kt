package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.PreferencesHelper
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import akihz.anlaki.dev.data.fps.LayerStat
import akihz.anlaki.dev.data.fps.TimeStatsParser
import java.util.Locale
import timber.log.Timber

/**
 * Layer pick list inside the overlay options panel.
 *
 * Owns layer choice and the radio group content. Selection persists via
 * prefs, matching the old controller behavior. Stateless apart from the
 * last shown keys used to skip rebuilds.
 */
internal class FpsLayerChoices(private val context: Context) {

    private var shownLayerKeys: List<String> = emptyList()

    /** Picks the layer to show: user choice when present, else the first. */
    fun choose(layers: List<LayerStat>, selectedLayer: String?): LayerStat {
        val sel = selectedLayer ?: PreferencesHelper.fpsSelectedLayer
        if (sel != null) {
            for (layer in layers) {
                if (layer.stableName == sel) return layer
            }
        }
        return layers.first()
    }

    /** Clears cached keys, forcing a full rebuild on the next update. */
    fun reset() {
        shownLayerKeys = emptyList()
    }

    /**
     * Rebuilds or refreshes the radio group for [layers].
     *
     * @param onSelect invoked with the picked stable name, null for Auto
     * @param refreshRate current display rate used to format FPS values
     * @param onChanged invoked when the group content changed and the window should re-clamp
     */
    fun updateChoices(
        group: RadioGroup,
        layers: List<LayerStat>,
        selectedLayer: String?,
        onSelect: (String?) -> Unit,
        refreshRate: () -> Double,
        onChanged: () -> Unit
    ) {
        val keys = layers.map { it.stableName }
        if (keys == shownLayerKeys && group.childCount == keys.size + 1) {
            // Update FPS values in place
            for (i in 1 until group.childCount) {
                val item = group.getChildAt(i) as? RadioButton ?: continue
                val layer = layers.getOrNull(i - 1) ?: continue
                val fps = TimeStatsParser.displayFps(layer.fps, refreshRate())
                item.text = layer.shortName() + String.format(Locale.US, "  %.1f", fps)
            }
            return
        }
        shownLayerKeys = keys
        group.setOnCheckedChangeListener(null)
        group.removeAllViews()

        val automatic = RadioButton(context).apply {
            text = "Auto"
            id = View.generateViewId()
            tag = null
            styleChoice(this)
        }
        group.addView(automatic)
        if (selectedLayer == null) automatic.isChecked = true

        for (layer in layers) {
            val item = RadioButton(context).apply {
                id = View.generateViewId()
                tag = layer.stableName
                val fps = TimeStatsParser.displayFps(layer.fps, refreshRate())
                text = layer.shortName() + String.format(Locale.US, "  %.1f", fps)
                styleChoice(this)
                if (layer.stableName == selectedLayer) isChecked = true
            }
            group.addView(item)
        }
        group.setOnCheckedChangeListener { g, checkedId ->
            val checked = g.findViewById<View>(checkedId)
            val tag = checked?.tag as? String
            onSelect(tag)
            Timber.i("FPS layer selection: ${tag ?: "Auto"}")
        }
        onChanged()
    }

    /** Checks the radio matching [selectedLayer] without rebuilding. */
    fun updateSelection(group: RadioGroup, selectedLayer: String?) {
        for (i in 0 until group.childCount) {
            val item = group.getChildAt(i) as? RadioButton ?: continue
            val tag = item.tag as? String
            item.isChecked = tag == selectedLayer
            if (i == 0 && selectedLayer == null) item.isChecked = true
        }
    }

    private fun styleChoice(item: RadioButton) {
        item.setTextColor(Color.WHITE)
        item.textSize = 14f
        val density = context.resources.displayMetrics.density
        item.minHeight = Math.round(44 * density)
        item.setPadding(
            Math.round(10 * density),
            Math.round(4 * density),
            Math.round(10 * density),
            Math.round(4 * density)
        )
    }
}
