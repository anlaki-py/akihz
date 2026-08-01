package akihz.anlaki.dev.data

import android.content.Context
import androidx.core.content.edit
import akihz.anlaki.dev.domain.update.AppUpdate

/** Persists an active update and its preparation state across process recreation. */
class UpdateDownloadStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Saves the system download identifier and its expected release metadata. */
    fun save(downloadId: Long, update: AppUpdate) {
        preferences.edit {
            putLong(KEY_DOWNLOAD_ID, downloadId)
            putString(KEY_VERSION_NAME, update.versionName)
            putLong(KEY_VERSION_CODE, update.versionCode)
            putString(KEY_APK_NAME, update.apkName)
            putString(KEY_DOWNLOAD_URL, update.downloadUrl)
            putString(KEY_SHA256, update.sha256)
            putBoolean(KEY_VERIFIED, false)
            putBoolean(KEY_NOTIFICATION_POSTED, false)
        }
    }

    /** Loads the pending download, or null when no update is active. */
    fun load(): PendingUpdateDownload? {
        val id = preferences.getLong(KEY_DOWNLOAD_ID, -1L)
        val versionName = preferences.getString(KEY_VERSION_NAME, null)
        val apkName = preferences.getString(KEY_APK_NAME, null)
        val downloadUrl = preferences.getString(KEY_DOWNLOAD_URL, null)
        val sha256 = preferences.getString(KEY_SHA256, null)
        if (
            id < 0 ||
            versionName == null ||
            apkName == null ||
            downloadUrl == null ||
            sha256 == null
        ) return null
        return PendingUpdateDownload(
            downloadId = id,
            update = AppUpdate(
                versionName = versionName,
                versionCode = preferences.getLong(KEY_VERSION_CODE, -1L),
                apkName = apkName,
                downloadUrl = downloadUrl,
                sha256 = sha256
            ),
            verified = preferences.getBoolean(KEY_VERIFIED, false),
            notificationPosted = preferences.getBoolean(KEY_NOTIFICATION_POSTED, false)
        )
    }

    /** Records that the APK digest was successfully verified. */
    fun markVerified(downloadId: Long) {
        if (preferences.getLong(KEY_DOWNLOAD_ID, -1L) != downloadId) return
        preferences.edit { putBoolean(KEY_VERIFIED, true) }
    }

    /** Records that the install-ready notification was already published. */
    fun markNotificationPosted(downloadId: Long) {
        if (preferences.getLong(KEY_DOWNLOAD_ID, -1L) != downloadId) return
        preferences.edit { putBoolean(KEY_NOTIFICATION_POSTED, true) }
    }

    /** Clears pending update metadata after installation or terminal failure. */
    fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "update_download"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_VERSION_NAME = "version_name"
        const val KEY_VERSION_CODE = "version_code"
        const val KEY_APK_NAME = "apk_name"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_SHA256 = "sha256"
        const val KEY_VERIFIED = "verified"
        const val KEY_NOTIFICATION_POSTED = "notification_posted"
    }
}

/** Persisted association between a Download Manager job and its release. */
data class PendingUpdateDownload(
    val downloadId: Long,
    val update: AppUpdate,
    val verified: Boolean = false,
    val notificationPosted: Boolean = false
)
