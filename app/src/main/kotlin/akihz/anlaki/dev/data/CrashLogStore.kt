package akihz.anlaki.dev.data

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists uncaught and manual crash reports to internal storage so the
 * debug screen can list, copy, share, or export them.
 *
 * Each crash is one plain-text file under `filesDir/crash_logs`. The handler
 * writes synchronously because the process may die immediately after.
 */
data class CrashEntry(
    val file: File,
    val fileName: String,
    val timestampMillis: Long,
    val sizeBytes: Long,
    val exceptionClass: String,
    val message: String?
)

@Singleton
class CrashLogStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun crashDir(): File = File(context.filesDir, "crash_logs").apply { if (!exists()) mkdirs() }

    /**
     * Synchronously writes a crash file. Call from the uncaught-exception
     * handler before delegating to the previous handler.
     */
    fun saveSync(thread: Thread, throwable: Throwable): File? = runCatching {
        val dir = crashDir()
        pruneIfNeeded(dir)
        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(now)
        val safeEx = throwable.javaClass.simpleName.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val file = File(dir, "crash-$stamp-$safeEx.log")
        file.writeText(buildContent(thread.name, throwable, now))
        file
    }.onFailure { Timber.w(it, "Failed to write crash log") }.getOrNull()

    /** Writes a non-fatal error without killing the process. */
    fun saveCaught(tag: String, throwable: Throwable): File? = runCatching {
        val dir = crashDir()
        pruneIfNeeded(dir)
        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(now)
        val safeTag = tag.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val file = File(dir, "error-$stamp-$safeTag.log")
        file.writeText(buildContent(Thread.currentThread().name, throwable, now, tag = tag))
        file
    }.onFailure { Timber.w(it, "Failed to write caught error") }.getOrNull()

    /** Returns all crash files sorted newest first. */
    fun getAll(): List<CrashEntry> {
        val dir = File(context.filesDir, "crash_logs")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val name = file.name
                // best-effort parse of exception class from filename
                val ex = name.substringAfterLast("-", "").substringBefore(".log").ifEmpty { "Unknown" }
                CrashEntry(
                    file = file,
                    fileName = name,
                    timestampMillis = file.lastModified(),
                    sizeBytes = file.length(),
                    exceptionClass = ex,
                    message = null
                )
            } ?: emptyList()
    }

    fun readContent(entry: CrashEntry): String = runCatching { entry.file.readText() }.getOrElse { "" }

    fun readContent(file: File): String = runCatching { file.readText() }.getOrElse { "" }

    fun delete(entry: CrashEntry): Boolean = runCatching { entry.file.delete() }.getOrDefault(false)

    fun deleteAll(): Int {
        val dir = File(context.filesDir, "crash_logs")
        val files = dir.listFiles() ?: return 0
        var count = 0
        files.forEach { if (it.delete()) count++ }
        return count
    }

    private fun pruneIfNeeded(dir: File) {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.size < MAX_FILES) return
        val toDelete = files.size - MAX_FILES + 1
        files.take(toDelete).forEach { runCatching { it.delete() } }
    }

    private fun buildContent(threadName: String, throwable: Throwable, now: Date, tag: String? = null): String {
        val iso = java.time.Instant.ofEpochMilli(now.time).toString()
        val pkg = context.packageName
        val pkgInfo = runCatching { context.packageManager.getPackageInfo(pkg, 0) }.getOrNull()
        val versionName = pkgInfo?.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo?.longVersionCode?.toString() ?: "0"
        } else {
            @Suppress("DEPRECATION") pkgInfo?.versionCode?.toString() ?: "0"
        }
        val stack = throwable.stackTraceToString()
        // limit to avoid huge files
        val truncated = if (stack.length > MAX_STACK_CHARS) stack.take(MAX_STACK_CHARS) + "\n...truncated" else stack
        return buildString {
            appendLine("Time: $iso")
            if (tag != null) appendLine("Tag: $tag")
            appendLine("App: $pkg $versionName ($versionCode)")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT}) abi=${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}")
            appendLine("Thread: $threadName")
            appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message ?: ""}")
            if (throwable.cause != null) appendLine("Cause: ${throwable.cause}")
            appendLine("--- Stack trace ---")
            appendLine(truncated)
        }
    }

    companion object {
        private const val MAX_FILES = 30
        private const val MAX_STACK_CHARS = 32_000
    }
}
