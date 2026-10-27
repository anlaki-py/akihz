package akihz.anlaki.dev.data

import android.content.Context
import android.os.Build
import android.util.Log
import akihz.anlaki.dev.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** A saved diagnostic report available to copy, share, or delete. */
data class CrashEntry(
    val file: File,
    val fileName: String,
    val timestampMillis: Long,
    val sizeBytes: Long,
    val exceptionClass: String,
    val message: String?
)

/** Bounded, synchronous storage shared by fatal, caught, and system error reporting. */
@Singleton
class CrashLogStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val directory: File get() = File(context.filesDir, "crash_logs")
    private val metadata: String by lazy {
        "App: ${context.packageName} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
            "Device: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} " +
            "(${Build.VERSION.SDK_INT}) abi=${Build.SUPPORTED_ABIS.firstOrNull()}\n"
    }

    /** Saves [throwable] from [thread] before the uncaught handler terminates the process. */
    @Synchronized
    fun saveSync(thread: Thread, throwable: Throwable): File? = writeReport(
        "crash", "FATAL", thread.name, throwable.stackTraceToString()
    )

    /** Saves a caught [throwable] with its operation [tag], without stopping the app. */
    @Synchronized
    fun saveCaught(tag: String, throwable: Throwable): File? = writeReport(
        "error", "ERROR", Thread.currentThread().name, "Tag: $tag\n${throwable.stackTraceToString()}"
    )

    /** Saves a formatted warning or error, including Timber's formatted throwable when present. */
    @Synchronized
    fun saveLog(priority: Int, tag: String?, message: String): File? = writeReport(
        "error", if (priority >= Log.ERROR) "ERROR" else "WARN", Thread.currentThread().name,
        "Tag: ${tag ?: "App"}\n$message"
    )

    /** Stores Android's account of a previous process exit, including ANRs and native crashes. */
    @Synchronized
    fun saveExit(timestamp: Long, description: String): File? = writeReport(
        "exit", "PROCESS EXIT", "Android", description, timestamp
    )

    /** Persists a small context trail for theme changes and activity lifecycle events. */
    @Synchronized
    fun recordEvent(tag: String, message: String) {
        try {
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create log directory" }
            val current = File(directory, "events.log")
            if (current.length() >= MAX_EVENT_BYTES) {
                current.copyTo(File(directory, "events-previous.log"), overwrite = true)
                current.writeText("")
            }
            current.appendText("${Instant.now()} [$tag] ${message.take(2_000)}\n")
        } catch (error: Exception) {
            // Never call Timber from its storage backend: a disk failure must not recurse.
            Log.e("CrashLogStore", "Could not save diagnostic event", error)
        }
    }

    /** Returns saved reports newest first. Context events are included in combined exports. */
    @Synchronized
    fun getAll(): List<CrashEntry> = reportFiles().sortedByDescending { it.lastModified() }.map {
        CrashEntry(it, it.name, it.lastModified(), it.length(), it.name.substringBefore('-'), null)
    }

    /** Reads [entry], returning an explicit message if the file is no longer available. */
    fun readContent(entry: CrashEntry): String = readContent(entry.file)

    /** Reads [file], returning an explicit message if storage cannot be read. */
    @Synchronized
    fun readContent(file: File): String = try {
        file.readText()
    } catch (error: Exception) {
        Log.e("CrashLogStore", "Could not read report", error)
        "Could not read ${file.name}: ${error.javaClass.simpleName}"
    }

    /** Copies recent reports and events within the clipboard's size limit. Share includes all logs. */
    @Synchronized
    fun recentReport(): String = buildString {
        append(metadata)
        appendLine("Recent diagnostics. Storage rotates at 30 crashes, 100 errors, and 30 exits.")
        appendLine("--- Recent events ---")
        appendLine(recentEvents())
        for (entry in getAll()) {
            val section = "\n--- ${entry.fileName} ---\n${readContent(entry)}"
            if (length + section.length > MAX_COPY_CHARS) {
                appendLine("\nOlder reports omitted from clipboard. Use Share all logs for the full saved history.")
                break
            }
            append(section)
        }
    }

    /** Writes all retained reports and events to [target] for file sharing. */
    @Synchronized
    fun writeBundle(target: File) {
        target.bufferedWriter().use { output ->
            output.append(metadata)
            listOf(File(directory, "events-previous.log"), File(directory, "events.log"))
                .filter { it.isFile }.plus(reportFiles().sortedByDescending { it.lastModified() })
                .forEach { file ->
                    output.appendLine("\n--- ${file.name} ---")
                    file.reader().use { it.copyTo(output) }
                }
        }
    }

    /** Deletes [entry] and returns whether removal succeeded. */
    @Synchronized
    fun delete(entry: CrashEntry): Boolean = entry.file.delete()

    /** Deletes retained reports and context events, returning the number removed. */
    @Synchronized
    fun deleteAll(): Int = directory.listFiles()?.count { it.isFile && it.delete() } ?: 0

    private fun writeReport(
        category: String,
        level: String,
        thread: String,
        detail: String,
        timestamp: Long = System.currentTimeMillis()
    ): File? {
        var file: File? = null
        return try {
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create log directory" }
            // Unique names also preserve concurrent errors and multiple events in one millisecond.
            file = File.createTempFile("$category-$timestamp-", ".log", directory)
            val content = buildString {
                appendLine("Time: ${Instant.ofEpochMilli(timestamp)}")
                appendLine("Level: $level")
                append(metadata)
                appendLine("Thread: $thread")
                appendLine(detail.take(MAX_DETAIL_CHARS))
                if (detail.length > MAX_DETAIL_CHARS) appendLine("...truncated")
                appendLine("--- Recent events ---")
                appendLine(recentEvents())
            }
            file.outputStream().use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                if (category == "crash") output.fd.sync()
            }
            file.setLastModified(timestamp)
            val limit = if (category == "error") 100 else 30
            reportFiles().filter { it.name.startsWith("$category-") }
                .sortedByDescending { it.lastModified() }.drop(limit).forEach { it.delete() }
            file
        } catch (error: Exception) {
            file?.delete()
            Log.e("CrashLogStore", "Could not save $category report", error)
            null
        }
    }

    private fun reportFiles(): List<File> = directory.listFiles()?.filter {
        it.isFile && it.extension == "log" && !it.name.startsWith("events") && it.length() > 0
    }.orEmpty()

    private fun recentEvents(): String = listOf("events-previous.log", "events.log")
        .map { File(directory, it) }.filter { it.isFile }
        .joinToString("") { it.readText() }.takeLast(8_000)

    private companion object {
        const val MAX_DETAIL_CHARS = 32_000
        const val MAX_EVENT_BYTES = 64_000
        const val MAX_COPY_CHARS = 180_000
    }
}
