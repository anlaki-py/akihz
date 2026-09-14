package akihz.anlaki.dev.data

import android.os.Build

/**
 * Maps OEM-specific Secure/System/Global Settings keys used to control refresh rate.
 *
 * Different manufacturers write to different keys across different namespaces:
 * - **Xiaomi/Redmi (MIUI/HyperOS)**: `miui_refresh_rate`, `user_refresh_rate`
 * - **Samsung (OneUI)**: `refresh_rate_mode`, `peak_refresh_rate` + `min_refresh_rate` pair
 * - **OnePlus (OxygenOS/ColorOS)**: `peak_refresh_rate`, `min_refresh_rate`, `refresh_rate_mode`
 * - **OPPO/realme/vivo/iQOO (ColorOS/FunTouch OS)**: `peak_refresh_rate`, `min_refresh_rate`, `active`
 * - **Motorola/Sony/ASUS**: Standard AOSP keys
 * - **Google Pixel**: Standard AOSP keys + `smooth_display_entry_point`
 * - **Stock Android / Others**: `user_refresh_rate`, `peak_refresh_rate` + `min_refresh_rate`
 *
 * Vendor key tables live in [OemKeyCatalog]. This file keeps models, routing, and detection.
 */
object OemSettingsStrategy {

    /**
     * Namespace for settings commands.
     */
    enum class Namespace {
        SECURE, SYSTEM, GLOBAL
    }

    /**
     * A key entry with its namespace.
     */
    data class SettingsKey(
        val namespace: Namespace,
        val key: String
    )

    /**
     * Complete key set for an OEM, including read, write, and lock-mode keys.
     */
    data class KeySet(
        /** Keys to query when reading the current rate. Ordered by priority. */
        val readKeys: List<SettingsKey>,
        /** Keys to write when applying a new rate. All are written. */
        val writeKeys: List<SettingsKey>,
        /** Keys to write in lock mode (min == peak). These constrain SurfaceFlinger. */
        val lockKeys: List<SettingsKey>,
        /** Whether this OEM supports a discrete mode setting (0=auto, 2=60Hz, 3=high). */
        val supportsMode: Boolean = false,
        /** The mode key if supportsMode is true. */
        val modeKey: SettingsKey? = null
    )

    /**
     * Resolves the appropriate key set for the current device.
     */
    fun resolve(): KeySet {
        return when {
            isXiaomi() -> OemKeyCatalog.xiaomi()
            isSamsung() -> OemKeyCatalog.samsung()
            isOnePlus() -> OemKeyCatalog.onePlus()
            isOppo() -> OemKeyCatalog.oppo()
            isVivo() -> OemKeyCatalog.vivo()
            isRealme() -> OemKeyCatalog.realme()
            isAsus() -> OemKeyCatalog.asus()
            isMotorola() -> OemKeyCatalog.motorola()
            isSony() -> OemKeyCatalog.sony()
            isPixel() -> OemKeyCatalog.pixel()
            else -> OemKeyCatalog.aosp()
        }
    }

    /**
     * Manually select a key set by OEM name. Used for override in settings.
     */
    fun resolveByName(oemName: String): KeySet {
        return when (oemName.lowercase()) {
            "xiaomi", "redmi", "xiaomi / redmi" -> OemKeyCatalog.xiaomi()
            "samsung" -> OemKeyCatalog.samsung()
            "oneplus" -> OemKeyCatalog.onePlus()
            "oppo" -> OemKeyCatalog.oppo()
            "vivo", "iqoo", "vivo / iqoo" -> OemKeyCatalog.vivo()
            "realme" -> OemKeyCatalog.realme()
            "asus", "rog", "asus / rog" -> OemKeyCatalog.asus()
            "motorola", "moto" -> OemKeyCatalog.motorola()
            "sony" -> OemKeyCatalog.sony()
            "google", "pixel", "google pixel" -> OemKeyCatalog.pixel()
            "aosp", "stock", "generic", "aosp / stock" -> OemKeyCatalog.aosp()
            else -> resolve()
        }
    }

    /**
     * Returns a list of all supported OEM names for manual override.
     */
    fun getSupportedOemNames(): List<String> = OemKeyCatalog.supportedNames()

    private fun isXiaomi(): Boolean {
        return Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("redmi", ignoreCase = true) ||
                Build.BRAND.equals("xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("redmi", ignoreCase = true)
    }

    private fun isSamsung(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    private fun isOnePlus(): Boolean {
        return Build.MANUFACTURER.equals("oneplus", ignoreCase = true) ||
                Build.BRAND.equals("oneplus", ignoreCase = true)
    }

    private fun isOppo(): Boolean {
        return Build.MANUFACTURER.equals("oppo", ignoreCase = true) ||
                Build.BRAND.equals("oppo", ignoreCase = true)
    }

    private fun isVivo(): Boolean {
        return Build.MANUFACTURER.equals("vivo", ignoreCase = true) ||
                Build.MANUFACTURER.equals("iqoo", ignoreCase = true) ||
                Build.BRAND.equals("vivo", ignoreCase = true) ||
                Build.BRAND.equals("iqoo", ignoreCase = true)
    }

    private fun isRealme(): Boolean {
        return Build.MANUFACTURER.equals("realme", ignoreCase = true) ||
                Build.BRAND.equals("realme", ignoreCase = true)
    }

    private fun isAsus(): Boolean {
        return Build.MANUFACTURER.equals("asus", ignoreCase = true) ||
                Build.BRAND.equals("asus", ignoreCase = true) ||
                Build.BRAND.equals("rog", ignoreCase = true)
    }

    private fun isMotorola(): Boolean {
        return Build.MANUFACTURER.equals("motorola", ignoreCase = true) ||
                Build.BRAND.equals("motorola", ignoreCase = true) ||
                Build.BRAND.equals("moto", ignoreCase = true)
    }

    private fun isSony(): Boolean {
        return Build.MANUFACTURER.equals("sony", ignoreCase = true) ||
                Build.BRAND.equals("sony", ignoreCase = true)
    }

    private fun isPixel(): Boolean {
        return Build.MANUFACTURER.equals("google", ignoreCase = true) &&
                (Build.BRAND.equals("google", ignoreCase = true) ||
                        Build.MODEL.contains("pixel", ignoreCase = true))
    }
}
