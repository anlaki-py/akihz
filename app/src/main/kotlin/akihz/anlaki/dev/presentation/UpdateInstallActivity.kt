package akihz.anlaki.dev.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import akihz.anlaki.dev.data.AppUpdateDownloader
import akihz.anlaki.dev.data.UpdateDownloadStore
import akihz.anlaki.dev.utils.UpdateNotification

/** Bridges an update notification to Android's permission and package installer screens. */
class UpdateInstallActivity : ComponentActivity() {
    private var downloadId = -1L
    private lateinit var downloader: AppUpdateDownloader
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (downloader.canInstallPackages()) install() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloader = AppUpdateDownloader(this)
        downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId < 0) {
            finish()
            return
        }
        if (downloader.canInstallPackages()) {
            install()
        } else {
            permissionLauncher.launch(downloader.installPermissionIntent())
        }
    }

    private fun install() {
        runCatching {
            UpdateNotification.cancelReady(this)
            UpdateDownloadStore(this).clear()
            downloader.install(downloadId)
        }
        finish()
    }

    companion object {
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }
}
