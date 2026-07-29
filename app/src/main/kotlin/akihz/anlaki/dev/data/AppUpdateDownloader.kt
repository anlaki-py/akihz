package akihz.anlaki.dev.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import akihz.anlaki.dev.domain.update.AppUpdate
import java.io.File
import java.security.MessageDigest

/** Downloads update APKs through Android's system download service. */
class AppUpdateDownloader(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)

    /** Enqueues [update] and returns its system download identifier. */
    fun enqueue(update: AppUpdate): Long {
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            update.apkName
        ).delete()
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("akiHz ${update.versionName}")
            .setDescription("Downloading app update")
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, update.apkName)
        return manager.enqueue(request).also { downloadId ->
            UpdateDownloadStore(context).save(downloadId, update)
        }
    }

    /** Returns download progress, or null when the download no longer exists. */
    fun status(downloadId: Long): DownloadStatus? {
        manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            )
            val total = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            )
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return DownloadStatus(status, downloaded, total, reason)
        }
    }

    /** Verifies the downloaded APK against the release digest when available. */
    fun verify(downloadId: Long, expectedSha256: String?): Boolean {
        if (expectedSha256 == null) return true
        val digest = MessageDigest.getInstance("SHA-256")
        manager.openDownloadedFile(downloadId).use { descriptor ->
            descriptor.fileDescriptor.let { java.io.FileInputStream(it) }.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
            .equals(expectedSha256, ignoreCase = true)
    }

    /** Creates the intent for Android's per-app unknown-source permission screen. */
    fun installPermissionIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )

    /** Opens Android's package installer for a completed download. */
    fun install(downloadId: Long) {
        val uri = manager.getUriForDownloadedFile(downloadId)
            ?: error("Downloaded APK is unavailable")
        context.startActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    /** Whether Android currently trusts this app to request APK installation. */
    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

/** Current state reported by Android's download manager. */
data class DownloadStatus(
    val state: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int
)
