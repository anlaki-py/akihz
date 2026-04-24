package akihz.anlaki.dev.data

import android.os.Build

/**
 * Maps OEM-specific Secure Settings keys used to control refresh rate.
 *
 * Different manufacturers write to different keys:
 * - Xiaomi (MIUI/HyperOS): `miui_refresh_rate`, falls back to `user_refresh_rate`
 * - Samsung: `min_refresh_rate` + `peak_refresh_rate` pair
 * - Stock Android / Others: `user_refresh_rate`, then `peak_refresh_rate` pair
 */
object OemSettingsStrategy {

    data class KeySet(
        /** Keys to query when reading the current rate. Ordered by priority. */
        val readKeys: List<String>,
        /** Keys to write when applying a new rate. All are written. */
        val writeKeys: List<String>
    )

    fun resolve(): KeySet {
        return when {
            isXiaomi() -> KeySet(
                readKeys = listOf("miui_refresh_rate", "user_refresh_rate"),
                writeKeys = listOf("miui_refresh_rate", "user_refresh_rate")
            )
            isSamsung() -> KeySet(
                readKeys = listOf("peak_refresh_rate", "min_refresh_rate", "user_refresh_rate"),
                writeKeys = listOf("peak_refresh_rate", "min_refresh_rate", "user_refresh_rate")
            )
            else -> KeySet(
                readKeys = listOf("user_refresh_rate", "peak_refresh_rate", "min_refresh_rate"),
                writeKeys = listOf("user_refresh_rate", "peak_refresh_rate", "min_refresh_rate")
            )
        }
    }

    private fun isXiaomi(): Boolean {
        return Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("redmi", ignoreCase = true) ||
                Build.BRAND.equals("xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("redmi", ignoreCase = true)
    }

    private fun isSamsung(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}
