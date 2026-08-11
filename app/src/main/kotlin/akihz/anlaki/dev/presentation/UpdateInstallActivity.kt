package akihz.anlaki.dev.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import akihz.anlaki.dev.data.AppUpdateDownloader
import akihz.anlaki.dev.data.UpdateDownloadStore
import akihz.anlaki.dev.utils.UpdateNotification
import kotlinx.coroutines.launch

/** Bridges an update notification to Android's permission and package installer screens. */
class UpdateInstallActivity : ComponentActivity() {
    private var downloadId = -1L
    private lateinit var downloader: AppUpdateDownloader
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (downloader.canInstallPackages()) launchInstaller() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloader = AppUpdateDownloader(this)
        downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId < 0) {
            finish()
            return
        }
        lifecycleScope.launch {
            val pending = UpdateDownloadStore(this@UpdateInstallActivity).load()
                ?.takeIf { it.downloadId == downloadId }
            val verified = pending != null &&
                downloader.verify(downloadId, pending.update.sha256)
            if (!verified) {
                UpdateNotification.showFailure(
                    this@UpdateInstallActivity,
                    "APK security verification failed"
                )
                finish()
            } else if (downloader.canInstallPackages()) {
                launchInstaller()
            } else {
                permissionLauncher.launch(downloader.installPermissionIntent())
            }
        }
    }

    private fun launchInstaller() {
        runCatching {
            UpdateNotification.cancelReady(this)
            downloader.install(downloadId)
        }
        finish()
    }

    companion object {
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }
}
