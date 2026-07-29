package akihz.anlaki.dev.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import akihz.anlaki.dev.data.AppUpdateDownloader
import akihz.anlaki.dev.data.UpdateDownloadStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Verifies completed update downloads and publishes the install notification. */
class UpdateDownloadReceiver : BroadcastReceiver() {

    /** Handles completion broadcasts belonging to the persisted app update. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val store = UpdateDownloadStore(context)
        val pendingUpdate = store.load()?.takeIf { it.downloadId == completedId } ?: return
        val asyncResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloader = AppUpdateDownloader(context)
                val status = downloader.status(completedId)
                when {
                    status?.state != DownloadManager.STATUS_SUCCESSFUL -> {
                        UpdateNotification.showFailure(context, "Android could not download the APK")
                        store.clear()
                    }
                    downloader.verify(completedId, pendingUpdate.update.sha256) -> {
                        UpdateNotification.showReady(
                            context,
                            completedId,
                            pendingUpdate.update.versionName
                        )
                    }
                    else -> {
                        UpdateNotification.showFailure(context, "APK security verification failed")
                        store.clear()
                    }
                }
            } catch (_: Exception) {
                UpdateNotification.showFailure(context, "Unable to prepare the downloaded update")
                store.clear()
            } finally {
                asyncResult.finish()
            }
        }
    }
}
