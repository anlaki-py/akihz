package akihz.anlaki.dev.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import akihz.anlaki.dev.domain.update.UpdateCheckFrequency
import akihz.anlaki.dev.utils.PreferencesHelper
import java.util.concurrent.TimeUnit

/** Keeps exactly one periodic update-check job aligned with the user's preference. */
object UpdateCheckScheduler {
    private const val UNIQUE_WORK_NAME = "automatic_app_update_check"

    /** Applies the currently persisted update-check frequency. */
    fun sync(context: Context) {
        schedule(context, PreferencesHelper.updateCheckFrequency)
    }

    /** Schedules the selected frequency, or cancels automatic checks for Never. */
    fun schedule(context: Context, frequency: UpdateCheckFrequency) {
        val workManager = WorkManager.getInstance(context)
        val intervalDays = frequency.intervalDays
        if (intervalDays == null) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            intervalDays,
            TimeUnit.DAYS
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
