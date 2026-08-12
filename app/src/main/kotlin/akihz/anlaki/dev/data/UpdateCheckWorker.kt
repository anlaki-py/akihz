package akihz.anlaki.dev.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.domain.update.UpdateAvailability
import akihz.anlaki.dev.domain.update.resolveUpdateAvailability
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.UpdateAvailableNotification
import java.io.IOException

/** Checks GitHub for a newer release and alerts once for each available version. */
class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /** Performs one background update check. */
    override suspend fun doWork(): Result {
        PreferencesHelper.init(applicationContext)
        return try {
            checkForUpdate()
            Result.success()
        } catch (error: Exception) {
            if (error.isTransient() && runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                Result.success()
            }
        }
    }

    private suspend fun checkForUpdate() {
        val channel = PreferencesHelper.updateChannel
        val update = GitHubUpdateRepository().findLatest(channel)
        PreferencesHelper.lastUpdateCheckAt = System.currentTimeMillis()
        when (
            resolveUpdateAvailability(
                BuildConfig.VERSION_CODE.toLong(),
                update.versionCode,
                channel
            )
        ) {
            UpdateAvailability.Available -> handleAvailableUpdate(update.versionCode, update.versionName)
            UpdateAvailability.UpToDate,
            UpdateAvailability.AheadOfStable -> clearAvailableUpdate()
        }
    }

    private fun handleAvailableUpdate(versionCode: Long, versionName: String) {
        PreferencesHelper.latestAvailableVersionCode = versionCode
        PreferencesHelper.latestAvailableVersionName = versionName
        if (PreferencesHelper.lastNotifiedVersionCode == versionCode) return
        if (UpdateAvailableNotification.show(applicationContext, versionName)) {
            PreferencesHelper.lastNotifiedVersionCode = versionCode
        }
    }

    private fun clearAvailableUpdate() {
        PreferencesHelper.clearAvailableUpdate()
        UpdateAvailableNotification.cancel(applicationContext)
    }

    private fun Exception.isTransient(): Boolean = when (this) {
        is UpdateHttpException -> statusCode == 408 || statusCode == 429 || statusCode >= 500
        is IOException -> true
        else -> false
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
