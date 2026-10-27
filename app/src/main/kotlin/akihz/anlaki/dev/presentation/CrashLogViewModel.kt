package akihz.anlaki.dev.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import akihz.anlaki.dev.data.CrashEntry
import akihz.anlaki.dev.data.CrashLogStore
import akihz.anlaki.dev.data.ProcessExitLogCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/** UI state for the crash-log page. */
data class CrashLogUiState(
    val entries: List<CrashEntry> = emptyList(),
    val selectedContent: String? = null,
    val selectedFileName: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class CrashLogViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val crashLogStore: CrashLogStore,
    private val processExitLogCollector: ProcessExitLogCollector
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrashLogUiState(isLoading = true))
    val uiState: StateFlow<CrashLogUiState> = _uiState.asStateFlow()

    init { refresh() }

    /** Reloads crash files from disk. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val entries = withContext(Dispatchers.IO) {
                processExitLogCollector.collect()
                crashLogStore.getAll()
            }
            _uiState.value = _uiState.value.copy(entries = entries, isLoading = false)
        }
    }

    /** Loads full text for preview/copy. */
    fun openEntry(entry: CrashEntry) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) { crashLogStore.readContent(entry) }
            _uiState.value = _uiState.value.copy(
                selectedContent = content,
                selectedFileName = entry.fileName
            )
        }
    }

    /** Clears the selected crash preview. */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedContent = null, selectedFileName = null)
    }

    /** Copies selected entry text to clipboard. */
    fun copyToClipboard() {
        val content = _uiState.value.selectedContent ?: run {
            _uiState.value = _uiState.value.copy(message = "No report selected")
            return
        }
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("error log", content))
        _uiState.value = _uiState.value.copy(message = "Copied to clipboard")
    }

    /** Copies a specific entry without opening it. */
    fun copyEntry(entry: CrashEntry) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) { crashLogStore.readContent(entry) }
            val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("error log", content))
            _uiState.value = _uiState.value.copy(message = "Copied ${entry.fileName}")
        }
    }

    /** Copies recent errors and context in one report, bounded for Android's clipboard. */
    fun copyDebugReport() {
        viewModelScope.launch {
            try {
                val report = withContext(Dispatchers.IO) { crashLogStore.recentReport() }
                val clipboard = appContext.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("akiHz debug report", report))
                _uiState.value = _uiState.value.copy(message = "Debug report copied")
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                Timber.e(error, "Could not copy debug report")
                _uiState.value = _uiState.value.copy(message = "Could not copy report. Try Share all logs.")
            }
        }
    }

    /** Shares every retained error, crash, system exit, and context event in a text file. */
    fun shareAllLogs() {
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    val directory = File(appContext.cacheDir, "share").apply { mkdirs() }
                    val file = File(directory, "akihz-debug-report.txt")
                    crashLogStore.writeBundle(file)
                    FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
                }
                launchShareIntent(uri, "akihz-debug-report.txt")
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                Timber.e(error, "Could not share debug report")
                _uiState.value = _uiState.value.copy(message = "Could not share debug report")
            }
        }
    }

    /**
     * Saves the crash file to Downloads.
     * @param entry crash to save
     */
    fun saveToDownloads(entry: CrashEntry) {
        viewModelScope.launch {
            val result = runCatching { writeToDownloads(entry.file) }
                .onFailure { Timber.w(it, "Failed to save crash to Downloads") }
                .getOrElse {
                    _uiState.value = _uiState.value.copy(message = "Could not save")
                    return@launch
                }
            _uiState.value = _uiState.value.copy(message = "Saved to $result")
        }
    }

    /** Saves the selected crash to Downloads. */
    fun saveSelectedToDownloads() {
        val name = _uiState.value.selectedFileName ?: run {
            _uiState.value = _uiState.value.copy(message = "No report selected")
            return
        }
        val entry = _uiState.value.entries.firstOrNull { it.fileName == name } ?: run {
            _uiState.value = _uiState.value.copy(message = "File not found")
            return
        }
        saveToDownloads(entry)
    }

    /**
     * Shares the given crash file.
     * @param entry crash to share
     */
    fun shareEntry(entry: CrashEntry) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) { shareableUri(entry.file) }
            if (uri == null) {
                _uiState.value = _uiState.value.copy(message = "Could not prepare file")
                return@launch
            }
            launchShareIntent(uri, entry.fileName)
        }
    }

    /** Shares the selected crash as a temp file. */
    fun shareSelected() {
        val content = _uiState.value.selectedContent ?: run {
            _uiState.value = _uiState.value.copy(message = "No report selected")
            return
        }
        val name = _uiState.value.selectedFileName ?: "crash.log"
        viewModelScope.launch {
            val tmp = withContext(Dispatchers.IO) {
                val cached = File(appContext.cacheDir, "share").apply { mkdirs() }
                val target = File(cached, name)
                target.writeText(content)
                FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", target)
            }
            launchShareIntent(tmp, name)
        }
    }

    /**
     * Deletes the given crash file.
     * @param entry crash to delete
     */
    fun deleteEntry(entry: CrashEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { crashLogStore.delete(entry) }
            _uiState.value = _uiState.value.copy(
                entries = _uiState.value.entries.filter { it.fileName != entry.fileName },
                selectedContent = if (_uiState.value.selectedFileName == entry.fileName) null else _uiState.value.selectedContent,
                selectedFileName = if (_uiState.value.selectedFileName == entry.fileName) null else _uiState.value.selectedFileName,
                message = "Deleted ${entry.fileName}"
            )
        }
    }

    /** Deletes all stored crash files. */
    fun deleteAll() {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) { crashLogStore.deleteAll() }
            _uiState.value = CrashLogUiState(entries = emptyList(), message = "Deleted $count file(s)")
        }
    }

    /** Exercises the release logging path without crashing the app. */
    fun logTestCrash() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Timber.tag("DiagnosticsTest").e(IllegalStateException("Test error from debug screen"), "Test caught error")
            }
            refresh()
            _uiState.value = _uiState.value.copy(message = "Test error logged")
        }
    }

    /** Clears the transient status message. */
    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private suspend fun writeToDownloads(source: File): String = withContext(Dispatchers.IO) {
        val name = source.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = appContext.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/akihz/crashes")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: error("MediaStore insert null")
            resolver.openOutputStream(uri).use { out ->
                source.inputStream().use { input -> checkNotNull(out).also { input.copyTo(it) } }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "${Environment.DIRECTORY_DOWNLOADS}/akihz/crashes/$name"
        } else {
            @Suppress("DEPRECATION")
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloads, "akihz/crashes").apply { mkdirs() }
            val target = File(targetDir, name)
            source.copyTo(target, overwrite = true)
            target.absolutePath
        }
    }

    private fun shareableUri(source: File): Uri? = runCatching {
        val cached = File(appContext.cacheDir, "share").apply { mkdirs() }
        val target = File(cached, source.name)
        source.copyTo(target, overwrite = true)
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", target)
    }.onFailure { Timber.e(it, "Could not prepare report for sharing") }.getOrNull()

    private fun launchShareIntent(uri: Uri, fileName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share error log").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(chooser)
        _uiState.value = _uiState.value.copy(message = "Choose where to share")
    }
}
