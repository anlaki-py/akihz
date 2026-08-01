package akihz.anlaki.dev.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Persists the single local custom refresh-rate profile and discovery snapshots. */
class CustomProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val snapshotStore = CustomSnapshotStore(context.filesDir)

    var warningAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_WARNING, false)
        set(value) = prefs.edit { putBoolean(KEY_WARNING, value) }

    /** Loads the current custom profile. */
    fun loadProfile(): CustomRefreshProfile {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return CustomRefreshProfile()
        return runCatching { decodeProfile(JSONObject(raw)) }.getOrDefault(CustomRefreshProfile())
    }

    /** Replaces the persisted custom profile. */
    fun saveProfile(profile: CustomRefreshProfile) {
        prefs.edit { putString(KEY_PROFILE, encodeProfile(profile).toString()) }
    }

    /** Loads saved discovery snapshots. */
    fun loadSnapshots(): List<SettingsSnapshot> {
        if (snapshotStore.exists) return snapshotStore.load()
        val legacy = prefs.getString(KEY_SNAPSHOTS, null) ?: return emptyList()
        return snapshotStore.migrate(legacy).also {
            prefs.edit { remove(KEY_SNAPSHOTS) }
        }
    }

    /** Replaces saved discovery snapshots. */
    fun saveSnapshots(snapshots: List<SettingsSnapshot>) {
        snapshotStore.save(snapshots)
        prefs.edit { remove(KEY_SNAPSHOTS) }
    }

    /** Persists values that must be restored after an interrupted test. */
    fun savePendingRestore(originals: Map<String, OriginalSetting>) {
        prefs.edit { putString(KEY_PENDING, encodeOriginals(originals).toString()) }
    }

    /** Loads an interrupted test's restoration journal. */
    fun loadPendingRestore(): Map<String, OriginalSetting> {
        val raw = prefs.getString(KEY_PENDING, null) ?: return emptyMap()
        return runCatching { decodeOriginals(JSONObject(raw)) }.getOrDefault(emptyMap())
    }

    /** Clears the interrupted-test restoration journal. */
    fun clearPendingRestore() {
        prefs.edit { remove(KEY_PENDING) }
    }

    private fun encodeProfile(profile: CustomRefreshProfile): JSONObject = JSONObject().apply {
        put("rates", JSONArray(profile.rates))
        put("tested", profile.tested)
        put("enabled", profile.enabled)
        put("keys", JSONArray().apply {
            profile.keys.forEach { key ->
                put(JSONObject().apply {
                    put("namespace", key.namespace.name)
                    put("name", key.name)
                    put("read", key.canRead)
                    put("write", key.canWrite)
                    put("values", JSONObject(key.values))
                })
            }
        })
        put("originals", encodeOriginals(profile.originals))
    }

    private fun decodeProfile(json: JSONObject): CustomRefreshProfile = CustomRefreshProfile(
        rates = json.getJSONArray("rates").let { array ->
            List(array.length()) { array.getDouble(it).toFloat() }
        },
        keys = json.getJSONArray("keys").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                CustomSettingsKey(
                    namespace = OemSettingsStrategy.Namespace.valueOf(item.getString("namespace")),
                    name = item.getString("name"),
                    canRead = item.getBoolean("read"),
                    canWrite = item.getBoolean("write"),
                    values = item.getJSONObject("values").toStringMap()
                )
            }
        },
        tested = json.optBoolean("tested"),
        enabled = json.optBoolean("enabled"),
        originals = json.optJSONObject("originals")?.let(::decodeOriginals).orEmpty()
    )

    private fun encodeOriginals(originals: Map<String, OriginalSetting>) = JSONObject().apply {
        originals.forEach { (id, original) ->
            put(id, JSONObject().put("existed", original.existed).put("value", original.value))
        }
    }

    private fun decodeOriginals(json: JSONObject): Map<String, OriginalSetting> =
        json.keys().asSequence().associateWith { id ->
            val item = json.getJSONObject(id)
            OriginalSetting(item.getBoolean("existed"), item.optString("value").takeIf { it != "null" })
        }

    private fun JSONObject.toStringMap(): Map<String, String> =
        keys().asSequence().associateWith { getString(it) }

    private companion object {
        const val PREFS_NAME = "custom_refresh_profile"
        const val KEY_PROFILE = "profile"
        const val KEY_SNAPSHOTS = "snapshots"
        const val KEY_WARNING = "warning_acknowledged"
        const val KEY_PENDING = "pending_restore"
    }
}
