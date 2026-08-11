package akihz.anlaki.dev.data

/**
 * User-defined refresh-rate configuration for an unsupported device.
 */
data class CustomRefreshProfile(
    val rates: List<Float> = emptyList(),
    val keys: List<CustomSettingsKey> = emptyList(),
    val tested: Boolean = false,
    val enabled: Boolean = false,
    val originals: Map<String, OriginalSetting> = emptyMap()
) {
    /** Returns a validation error, or null when this profile can be tested. */
    fun validationError(): String? {
        if (rates.isEmpty()) return "Select at least one refresh rate."
        if (keys.none { it.canWrite }) return "Select at least one writable key."
        if (keys.map { it.id }.distinct().size != keys.size) return "Each key can only be selected once."
        keys.filter { it.canWrite }.forEach { setting ->
            rates.forEach { rate ->
                if (setting.values[rateKey(rate)].isNullOrBlank()) {
                    return "Add a ${rateLabel(rate)} value for ${setting.id}."
                }
            }
        }
        return null
    }

    companion object {
        /** Produces a stable map key for a display rate. */
        fun rateKey(rate: Float): String = rate.toString()

        /** Produces a concise user-facing display rate. */
        fun rateLabel(rate: Float): String =
            if (rate % 1f == 0f) rate.toInt().toString() else rate.toString()
    }
}

/** A selected setting and its per-rate values. */
data class CustomSettingsKey(
    val namespace: OemSettingsStrategy.Namespace,
    val name: String,
    val canRead: Boolean = true,
    val canWrite: Boolean = true,
    val values: Map<String, String> = emptyMap()
) {
    val id: String get() = "${namespace.name.lowercase()}/$name"
}

/** Value captured before a custom profile was enabled. */
data class OriginalSetting(val existed: Boolean, val value: String?)

/** A complete baseline or sparse rate difference used for candidate discovery. */
data class SettingsSnapshot(
    val label: String,
    val values: Map<String, String>,
    val isDiff: Boolean = false
)

/** A ranked setting found during scanning or snapshot comparison. */
data class SettingsCandidate(
    val namespace: OemSettingsStrategy.Namespace,
    val name: String,
    val value: String,
    val score: Int,
    val reason: String
) {
    val id: String get() = "${namespace.name.lowercase()}/$name"
}
