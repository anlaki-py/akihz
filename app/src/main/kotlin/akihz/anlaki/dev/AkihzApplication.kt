package akihz.anlaki.dev

import android.app.Application
import akihz.anlaki.dev.data.CustomProfileManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AkihzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CustomProfileManager.init(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
