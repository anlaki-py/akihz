package akihz.anlaki.dev

import android.app.Application
import akihz.anlaki.dev.data.CustomProfileManager
import akihz.anlaki.dev.data.UpdateDownloadStore
import akihz.anlaki.dev.utils.UpdateNotification
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AkihzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CustomProfileManager.init(this)
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
