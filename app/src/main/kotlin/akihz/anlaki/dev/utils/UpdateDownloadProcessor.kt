package akihz.anlaki.dev.utils

import android.app.DownloadManager
import android.content.Context
import akihz.anlaki.dev.data.AppUpdateDownloader
import akihz.anlaki.dev.data.UpdateDownloadStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Result of preparing a completed update for installation. */
sealed interface UpdatePreparationResult {
    data object Ready : UpdatePreparationResult
    data object Missing : UpdatePreparationResult
    data class Failed(val message: String) : UpdatePreparationResult
}

/** Verifies each completed APK and publishes its ready notification once per download. */
object UpdateDownloadProcessor {
    private val mutex = Mutex()

    /** Prepares [downloadId], reusing persisted verification and notification state. */
    suspend fun prepare(context: Context, downloadId: Long): UpdatePreparationResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val appContext = context.applicationContext
                val store = UpdateDownloadStore(appContext)
                var pending = store.load()?.takeIf { it.downloadId == downloadId }
                    ?: return@withLock UpdatePreparationResult.Missing
                val downloader = AppUpdateDownloader(appContext)

                try {
                    val status = downloader.status(downloadId)
                    if (status?.state != DownloadManager.STATUS_SUCCESSFUL) {
                        return@withLock fail(
                            appContext,
                            store,
                            "Android could not download the APK"
                        )
                    }
                    if (!pending.verified) {
                        if (!downloader.verify(downloadId, pending.update.sha256)) {
                            return@withLock fail(
                                appContext,
                                store,
                                "APK security verification failed"
                            )
                        }
                        store.markVerified(downloadId)
                        pending = pending.copy(verified = true)
                    }
                    if (!pending.notificationPosted) {
                        UpdateNotification.showReady(
                            appContext,
                            downloadId,
                            pending.update.versionName
                        )
                        store.markNotificationPosted(downloadId)
                    }
                    UpdatePreparationResult.Ready
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    fail(appContext, store, "Unable to prepare the downloaded update")
                }
            }
        }

    private fun fail(
        context: Context,
        store: UpdateDownloadStore,
        message: String
    ): UpdatePreparationResult.Failed {
        UpdateNotification.showFailure(context, message)
        store.clear()
        return UpdatePreparationResult.Failed(message)
    }
}
