package akihz.anlaki.dev

import android.app.Application
import akihz.anlaki.dev.data.CustomProfileManager
import akihz.anlaki.dev.data.UpdateDownloadStore
import akihz.anlaki.dev.data.UpdateCheckScheduler
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.UpdateAvailableNotification
import akihz.anlaki.dev.utils.UpdateNotification
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AkihzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferencesHelper.init(this)
        CustomProfileManager.init(this)
        if (PreferencesHelper.latestAvailableVersionCode <= BuildConfig.VERSION_CODE) {
            PreferencesHelper.clearAvailableUpdate()
            UpdateAvailableNotification.cancel(this)
        }
        UpdateCheckScheduler.sync(this)
        UpdateDownloadStore(this).load()
            ?.takeIf { it.update.versionCode <= BuildConfig.VERSION_CODE }
            ?.let {
                UpdateDownloadStore(this).clear()
                UpdateNotification.cancelReady(this)
            }
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
