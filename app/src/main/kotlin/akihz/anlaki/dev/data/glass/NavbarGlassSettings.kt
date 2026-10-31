package akihz.anlaki.dev.data.glass

import org.json.JSONObject

/** Shader controls in density-independent units, with safe ranges for persisted presets. */
internal enum class GlassParameter(
    val label: String,
    val unit: String,
    val range: ClosedFloatingPointRange<Float>,
    val outerDefault: Float,
    val pillDefault: Float = outerDefault
) {
    Refraction("Refraction", "dp", 0f..24f, 7f, 5f),
    EdgeWidth("Lens edge width", "dp", 1f..32f, 14f),
    Dispersion("Color separation", "dp", 0f..6f, 0.5f),
    Blur("Blur radius", "dp", 0f..12f, 0f, 2f),
    Tint("Tint opacity", "", 0f..1f, 0.25f, 0.5f),
    Hue("Custom tint hue", "°", 0f..360f, 20f),
    TintSaturation("Custom tint saturation", "", 0f..1f, 0.65f),
    TintBrightness("Custom tint brightness", "", 0f..1f, 0.85f, 0.1f),
    Saturation("Background saturation", "", 0f..2f, 1f),
    Brightness("Background brightness", "", 0.25f..2f, 1f),
    Highlight("Edge reflection", "", 0f..1f, 0.35f),
    LightAngle("Light angle", "°", 0f..360f, 225f);

    /** Returns a bounded [value], replacing invalid numbers with [fallback]. */
    fun sanitize(value: Float, fallback: Float): Float =
        if (value.isFinite()) value.coerceIn(range) else fallback
}

/** Independent optical controls for one navbar layer. */
internal data class GlassLayerSettings(
    val pill: Boolean = false,
    val useThemeTint: Boolean = true,
    val values: Map<GlassParameter, Float> = emptyMap()
) {
    /** Returns the validated value for [parameter], using this layer's default if absent. */
    operator fun get(parameter: GlassParameter): Float {
        val default = if (pill) parameter.pillDefault else parameter.outerDefault
        return parameter.sanitize(values[parameter] ?: default, default)
    }

    /** Returns a copy with [parameter] set to a bounded [value]. */
    fun withValue(parameter: GlassParameter, value: Float): GlassLayerSettings =
        copy(values = values + (parameter to parameter.sanitize(value, get(parameter))))

    /** Encodes all controls, including defaults, for a reproducible preset. */
    fun toJson(): JSONObject = JSONObject().apply {
        put("useThemeTint", useThemeTint)
        GlassParameter.entries.forEach { put(it.name, get(it).toDouble()) }
    }
}

/** Opt-in navbar experiment. Outer glass starts clear; the selected pill has a slight blur. */
internal data class NavbarGlassSettings(
    val enabled: Boolean = false,
    val outer: GlassLayerSettings = GlassLayerSettings(),
    val pill: GlassLayerSettings = GlassLayerSettings(pill = true)
) {
    /** Returns the complete versioned JSON preset for storage, clipboard, or sharing. */
    fun toJson(): String = JSONObject().apply {
        put("format", "akihz-navbar-glass")
        put("version", 1)
        put("enabled", enabled)
        put("outer", outer.toJson())
        put("pill", pill.toJson())
    }.toString(2)

    companion object {
        /** Decodes [json], rejecting unsupported formats and bounding every shader control. */
        fun fromJson(json: String): NavbarGlassSettings {
            val root = JSONObject(json)
            require(root.getString("format") == "akihz-navbar-glass") { "Unknown glass preset format" }
            require(root.getInt("version") == 1) { "Unsupported glass preset version" }
            return NavbarGlassSettings(
                enabled = root.optBoolean("enabled", false),
                outer = decodeLayer(root.optJSONObject("outer"), false),
                pill = decodeLayer(root.optJSONObject("pill"), true)
            )
        }

        private fun decodeLayer(json: JSONObject?, pill: Boolean): GlassLayerSettings {
            val defaults = GlassLayerSettings(pill = pill)
            return defaults.copy(
                useThemeTint = json?.optBoolean("useThemeTint", true) ?: true,
                values = GlassParameter.entries.associateWith {
                    it.sanitize(json?.optDouble(it.name, defaults[it].toDouble())?.toFloat()
                        ?: defaults[it], defaults[it])
                }
            )
        }
    }
}
