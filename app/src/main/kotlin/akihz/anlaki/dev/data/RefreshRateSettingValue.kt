package akihz.anlaki.dev.data

/**
 * Converts a refresh rate into the value expected by a system settings key.
 */
internal object RefreshRateSettingValue {

    /**
     * Returns the value for a refresh-rate related settings key.
     *
     * Mode keys use the OEM mode convention instead of a literal Hz value,
     * while boolean feature keys are enabled with `1`.
     */
    fun forKey(key: String, hz: Int): Int = when (key) {
        "refresh_rate_mode" -> if (hz <= STANDARD_RATE_HZ) MODE_STANDARD else MODE_HIGH
        "active", "smooth_display_entry_point" -> ENABLED
        else -> hz
    }

    private const val STANDARD_RATE_HZ = 60
    private const val MODE_STANDARD = 2
    private const val MODE_HIGH = 3
    private const val ENABLED = 1
}
