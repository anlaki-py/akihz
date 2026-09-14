package akihz.anlaki.dev.data

import akihz.anlaki.dev.data.OemSettingsStrategy.KeySet
import akihz.anlaki.dev.data.OemSettingsStrategy.Namespace
import akihz.anlaki.dev.data.OemSettingsStrategy.SettingsKey

/**
 * Vendor key tables behind [OemSettingsStrategy].
 *
 * Holds the per-OEM read, write, and lock key lists in one place so the
 * strategy file keeps only models, routing, and device detection.
 */
internal object OemKeyCatalog {

    /** Names shown in the manual override list, in display order. */
    fun supportedNames(): List<String> = listOf(
        "Auto-detect",
        "Xiaomi / Redmi",
        "Samsung",
        "OnePlus",
        "OPPO",
        "vivo / iQOO",
        "realme",
        "ASUS / ROG",
        "Motorola",
        "Sony",
        "Google Pixel",
        "AOSP / Stock"
    )

    /** Refresh rate keys for Xiaomi and Redmi devices. */
    internal fun xiaomi(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SECURE, "miui_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SECURE, "miui_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "refresh_rate_mode")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SECURE, "miui_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "refresh_rate_mode")
        ),
        supportsMode = true,
        modeKey = SettingsKey(Namespace.GLOBAL, "refresh_rate_mode")
    )

    /** Refresh rate keys for Samsung devices. */
    internal fun samsung(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SECURE, "refresh_rate_mode"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SECURE, "refresh_rate_mode"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "refresh_rate_mode")
        ),
        supportsMode = true,
        modeKey = SettingsKey(Namespace.SECURE, "refresh_rate_mode")
    )

    /** Refresh rate keys for OnePlus devices. */
    internal fun onePlus(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "refresh_rate_mode"),
            SettingsKey(Namespace.SYSTEM, "user_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "refresh_rate_mode"),
            SettingsKey(Namespace.SYSTEM, "user_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "refresh_rate_mode")
        ),
        supportsMode = true,
        modeKey = SettingsKey(Namespace.SECURE, "refresh_rate_mode")
    )

    /** Refresh rate keys for OPPO devices. */
    internal fun oppo(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate")
        )
    )

    /** Refresh rate keys for vivo and iQOO devices. */
    internal fun vivo(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SECURE, "active"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "refresh_rate_mode")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SECURE, "active"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "refresh_rate_mode")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "active")
        )
    )

    /** Refresh rate keys for realme devices. */
    internal fun realme(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate")
        )
    )

    /** Refresh rate keys for ASUS and ROG devices. */
    internal fun asus(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate")
        )
    )

    /** Refresh rate keys for Motorola devices. */
    internal fun motorola(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate")
        )
    )

    /** Refresh rate keys for Sony devices. */
    internal fun sony(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate")
        )
    )

    /** Refresh rate keys for Google Pixel devices. */
    internal fun pixel(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "smooth_display_entry_point"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "smooth_display_entry_point"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate")
        )
    )

    /** Refresh rate keys for AOSP and stock Android devices. */
    internal fun aosp(): KeySet = KeySet(
        readKeys = listOf(
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        writeKeys = listOf(
            SettingsKey(Namespace.SECURE, "user_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.GLOBAL, "user_preferred_refresh_rate")
        ),
        lockKeys = listOf(
            SettingsKey(Namespace.SYSTEM, "peak_refresh_rate"),
            SettingsKey(Namespace.SYSTEM, "min_refresh_rate"),
            SettingsKey(Namespace.SECURE, "user_refresh_rate")
        )
    )
}
