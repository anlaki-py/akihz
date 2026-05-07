package akihz.anlaki.dev.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts [KeepAliveService] after device boot or app update.
 */
class BootReceiver : BroadcastReceiver() {

    /**
     * Handles boot and package replacement broadcasts.
     *
     * @param context receiver context supplied by Android
     * @param intent broadcast intent
     */
    override fun onReceive(context: Context, intent: Intent) {
        val isBootOrUpdate = intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (isBootOrUpdate) {
            KeepAliveService.start(context)
        }
    }
}
