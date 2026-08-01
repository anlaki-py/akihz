package akihz.anlaki.dev.data

import android.util.AtomicFile
import java.io.File
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.json.JSONArray
import org.json.JSONObject

internal const val MAX_STORED_SNAPSHOTS = 12

/** Stores bounded discovery snapshots outside memory-backed preferences. */
internal class CustomSnapshotStore(filesDir: File) {
    private val file = AtomicFile(File(filesDir, FILE_NAME))

    val exists: Boolean get() = file.baseFile.exists()

    @Synchronized
    fun load(): List<SettingsSnapshot> {
        if (!exists) return emptyList()
        return runCatching {
            file.openRead().use { input ->
                GZIPInputStream(input).bufferedReader().use { decode(it.readText()) }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(snapshots: List<SettingsSnapshot>) {
        val output = file.startWrite()
        try {
            val gzip = GZIPOutputStream(output)
            val writer = OutputStreamWriter(gzip, Charsets.UTF_8)
            writer.write(encode(retainSnapshots(snapshots)).toString())
            writer.flush()
            gzip.finish()
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    fun migrate(raw: String): List<SettingsSnapshot> {
        val snapshots = runCatching { decode(raw) }.getOrDefault(emptyList())
        save(snapshots)
        return retainSnapshots(snapshots)
    }

    private fun encode(snapshots: List<SettingsSnapshot>) = JSONArray().apply {
        snapshots.forEach { snapshot ->
            put(JSONObject().put("label", snapshot.label).put("values", JSONObject(snapshot.values)))
        }
    }

    private fun decode(raw: String): List<SettingsSnapshot> {
        val array = JSONArray(raw)
        val snapshots = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            SettingsSnapshot(item.getString("label"), item.getJSONObject("values").toStringMap())
        }
        return retainSnapshots(snapshots)
    }

    private fun JSONObject.toStringMap(): Map<String, String> =
        keys().asSequence().associateWith { getString(it) }

    private companion object {
        const val FILE_NAME = "custom_key_snapshots.json.gz"
    }
}

/** Replaces recaptured labels and keeps only the newest bounded set. */
internal fun retainSnapshots(snapshots: List<SettingsSnapshot>): List<SettingsSnapshot> {
    val unique = linkedMapOf<String, SettingsSnapshot>()
    snapshots.forEach { snapshot ->
        unique.remove(snapshot.label)
        unique[snapshot.label] = snapshot
    }
    return unique.values.toList().takeLast(MAX_STORED_SNAPSHOTS)
}
