@file:Suppress("RestrictedApi")

package akihz.anlaki.dev.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.TonalPalette
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** User-created Material theme based on primary, secondary, and tertiary seed colors. */
data class CustomTheme(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val primaryColorArgb: Long = 0xFF6750A4,
    val secondaryColorArgb: Long? = null,
    val tertiaryColorArgb: Long? = null
) {
    /** Generates light or dark Material colors from this theme's seeds. */
    fun generateColorScheme(dark: Boolean): ColorScheme {
        val scheme = SchemeTonalSpot(Hct.fromInt(primaryColorArgb.toInt()), dark, 0.0)
            .toComposeColorScheme()
        return scheme
            .withSecondarySeed(secondaryColorArgb, dark)
            .withTertiarySeed(tertiaryColorArgb, dark)
    }

    /** Encodes this theme for clipboard export. */
    fun toJson(): String = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("primaryColorArgb", primaryColorArgb)
        .put("secondaryColorArgb", secondaryColorArgb)
        .put("tertiaryColorArgb", tertiaryColorArgb)
        .toString(2)

    companion object {
        /** Parses an exported theme, or returns null for invalid input. */
        fun fromJson(value: String): CustomTheme? = runCatching {
            val json = JSONObject(value)
            CustomTheme(
                id = json.optString("id").takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
                name = json.getString("name"),
                primaryColorArgb = json.getLong("primaryColorArgb"),
                secondaryColorArgb = json.nullableLong("secondaryColorArgb"),
                tertiaryColorArgb = json.nullableLong("tertiaryColorArgb")
            )
        }.getOrNull()

        /** Encodes saved themes for preferences. */
        fun encodeList(themes: List<CustomTheme>): String = JSONArray().apply {
            themes.forEach { put(JSONObject(it.toJson())) }
        }.toString()

        /** Decodes saved themes, dropping malformed entries. */
        fun decodeList(value: String?): List<CustomTheme> = runCatching {
            val array = JSONArray(value ?: "[]")
            buildList {
                for (index in 0 until array.length()) {
                    fromJson(array.getJSONObject(index).toString())?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }
}

private fun JSONObject.nullableLong(key: String): Long? =
    if (has(key) && !isNull(key)) getLong(key) else null

private fun ColorScheme.withSecondarySeed(argb: Long?, dark: Boolean): ColorScheme {
    if (argb == null) return this
    val palette = TonalPalette.fromInt(argb.toInt())
    return copy(
        secondary = Color(palette.tone(if (dark) 80 else 40)),
        onSecondary = Color(palette.tone(if (dark) 20 else 100)),
        secondaryContainer = Color(palette.tone(if (dark) 30 else 90)),
        onSecondaryContainer = Color(palette.tone(if (dark) 90 else 10))
    )
}

private fun ColorScheme.withTertiarySeed(argb: Long?, dark: Boolean): ColorScheme {
    if (argb == null) return this
    val palette = TonalPalette.fromInt(argb.toInt())
    return copy(
        tertiary = Color(palette.tone(if (dark) 80 else 40)),
        onTertiary = Color(palette.tone(if (dark) 20 else 100)),
        tertiaryContainer = Color(palette.tone(if (dark) 30 else 90)),
        onTertiaryContainer = Color(palette.tone(if (dark) 90 else 10))
    )
}
