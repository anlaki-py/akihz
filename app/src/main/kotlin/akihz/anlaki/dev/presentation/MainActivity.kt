package akihz.anlaki.dev.presentation

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.presentation.theme.LauncherIconSwitcher
import akihz.anlaki.dev.presentation.theme.AkihzColors
import akihz.anlaki.dev.presentation.theme.AppThemeMode
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import akihz.anlaki.dev.data.PreferencesHelper
import rikka.shizuku.Shizuku
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

private data class AppDialog(
    val title: String,
    val message: String,
    val cancelable: Boolean
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_UPDATES = "akihz.extra.OPEN_UPDATES"
        const val EXTRA_OPEN_DEBUG = "akihz.extra.OPEN_DEBUG"
        const val EXTRA_DEBUG_PAGE = "akihz.extra.DEBUG_PAGE"
        const val EXTRA_OPEN_FPS_MONITOR = "akihz.extra.OPEN_FPS_MONITOR"
        private const val REQUEST_CODE_SHIZUKU = 1001
        private const val SHIZUKU_OWNER = "main_activity"
    }

    private val launcherIconSwitcher by lazy { LauncherIconSwitcher(this) }
    private val viewModel: MainViewModel by viewModels()
    private var isServiceBound = false
    private var hasAcceptedWelcomeNotice = false
    private val dialogState = MutableStateFlow<AppDialog?>(null)
    private val openUpdatesRequest = MutableStateFlow(0)
    private val openDebugRequest = MutableStateFlow(0)
    private val openFpsMonitorRequest = MutableStateFlow(0)
    private var pendingDebugPage: String? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            PreferencesHelper.notificationPermissionRequested = true
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (hasAcceptedWelcomeNotice) {
            checkShizukuPermission()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isServiceBound = false
        if (hasAcceptedWelcomeNotice) {
            showError("Shizuku service died. Please restart Shizuku.")
        }
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU && grantResult == PackageManager.PERMISSION_GRANTED) {
            bindUserServiceAndLoad()
        } else if (requestCode == REQUEST_CODE_SHIZUKU) {
            showError("Shizuku permission denied.")
        }
    }

    /** Creates the main screen and binds app services. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        PreferencesHelper.init(this)
        updateLauncherIcon(PreferencesHelper.themedAppIcon, PreferencesHelper.themeMode)
        hasAcceptedWelcomeNotice = PreferencesHelper.welcomeNoticeAccepted
        if (hasAcceptedWelcomeNotice && PreferencesHelper.keepAliveEnabled) {
            KeepAliveService.start(this)
        }
        requestNotificationPermissionIfNeeded()
        // A recreated activity replays the launch intent; do not jump to a tile page again.
        handleUpdateIntent(intent, allowTilePreferences = savedInstanceState == null)

        setContent {
            var themedAppIcon by rememberSaveable { mutableStateOf(PreferencesHelper.themedAppIcon) }
            var themeMode by rememberSaveable { mutableStateOf(PreferencesHelper.themeMode) }
            var dynamicColor by rememberSaveable { mutableStateOf(PreferencesHelper.dynamicColor) }
            var themeId by rememberSaveable { mutableStateOf(PreferencesHelper.themeId) }
            var customThemes by remember { mutableStateOf(PreferencesHelper.customThemes) }
            var amoledMode by rememberSaveable { mutableStateOf(PreferencesHelper.amoledMode) }
            var keepAliveEnabled by rememberSaveable { mutableStateOf(PreferencesHelper.keepAliveEnabled) }
            var homeDebugSettings by remember {
                mutableStateOf(PreferencesHelper.homeDebugSettings)
            }
            var debugOptionsUnlocked by rememberSaveable {
                mutableStateOf(PreferencesHelper.debugOptionsUnlocked)
            }
            var showWelcomeNotice by rememberSaveable {
                mutableStateOf(!PreferencesHelper.welcomeNoticeAccepted)
            }
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.System -> systemDarkTheme
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            AnlakiTheme(
                darkTheme = darkTheme,
                amoledMode = amoledMode,
                dynamicColor = dynamicColor,
                themeId = themeId,
                customThemes = customThemes
            ) {
                val backgroundColor = AkihzColors.pageContainer
                SideEffect {
                    window.statusBarColor = backgroundColor.toArgb()
                    window.navigationBarColor = backgroundColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                val uiState by viewModel.uiState.collectAsState()
                val appDialog by dialogState.collectAsState()
                val updateRequest by openUpdatesRequest.collectAsState()
                val debugRequest by openDebugRequest.collectAsState()
                val fpsMonitorRequest by openFpsMonitorRequest.collectAsState()

                AkihzApp(
                    uiState = uiState,
                    onRateSelected = viewModel::selectRate,
                    onTileRateIncludedChanged = viewModel::setTileRateIncluded,
                    onResetToDefaults = viewModel::resetToDefaults,
                    onCustomProfileChanged = viewModel::onCustomProfileChanged,
                    themedAppIcon = themedAppIcon,
                    onThemedAppIconChanged = { enabled ->
                        if (updateLauncherIcon(enabled, themeMode)) {
                            PreferencesHelper.themedAppIcon = enabled
                            themedAppIcon = enabled
                        }
                    },
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    themeId = themeId,
                    customThemes = customThemes,
                    amoledMode = amoledMode,
                    homeDebugSettings = homeDebugSettings,
                    onHomeDebugSettingsChanged = { settings ->
                        homeDebugSettings = settings
                        PreferencesHelper.homeDebugSettings = settings
                    },
                    debugOptionsUnlocked = debugOptionsUnlocked,
                    onDebugOptionsUnlocked = {
                        debugOptionsUnlocked = true
                        PreferencesHelper.debugOptionsUnlocked = true
                    },
                    onThemeModeChanged = { mode ->
                        if (updateLauncherIcon(themedAppIcon, mode)) {
                            PreferencesHelper.themeMode = mode
                            themeMode = mode
                        }
                    },
                    onDynamicColorChanged = { enabled ->
                        dynamicColor = enabled
                        PreferencesHelper.dynamicColor = enabled
                    },
                    onThemeIdChanged = { selectedId ->
                        themeId = selectedId
                        PreferencesHelper.themeId = selectedId
                    },
                    onCustomThemesChanged = { themes ->
                        customThemes = themes
                        PreferencesHelper.customThemes = themes
                    },
                    onAmoledModeChanged = { enabled ->
                        amoledMode = enabled
                        PreferencesHelper.amoledMode = enabled
                    },
                    keepAliveEnabled = keepAliveEnabled,
                    onKeepAliveEnabledChanged = { enabled ->
                        if (enabled) {
                            PreferencesHelper.keepAliveEnabled = true
                            if (KeepAliveService.start(this@MainActivity)) {
                                keepAliveEnabled = true
                            } else {
                                PreferencesHelper.keepAliveEnabled = false
                                keepAliveEnabled = false
                                showDialog(
                                    "Background service blocked",
                                    "Android did not allow akiHz to keep its background service running. " +
                                        "The tile still works, but it may start slower."
                                )
                            }
                        } else {
                            keepAliveEnabled = false
                            PreferencesHelper.keepAliveEnabled = false
                            KeepAliveService.stop(this@MainActivity)
                        }
                    },
                    openUpdatesRequest = updateRequest,
                    openDebugRequest = debugRequest,
                    openFpsMonitorRequest = fpsMonitorRequest,
                    debugPage = pendingDebugPage,
                    onErrorDismissed = { viewModel.onErrorDismissed() }
                )
                appDialog?.let { dialog ->
                    AlertDialog(
                        onDismissRequest = {
                            if (dialog.cancelable) dialogState.value = null
                        },
                        title = { Text(dialog.title) },
                        text = { Text(dialog.message) },
                        confirmButton = {
                            TextButton(onClick = { dialogState.value = null }) {
                                Text("OK")
                            }
                        }
                    )
                }
                if (showWelcomeNotice) {
                    FirstRunNoticeDialog(
                        onAccept = {
                            PreferencesHelper.welcomeNoticeAccepted = true
                            hasAcceptedWelcomeNotice = true
                            showWelcomeNotice = false
                            if (keepAliveEnabled && !KeepAliveService.start(this@MainActivity)) {
                                showDialog(
                                    "Background service blocked",
                                    "Android did not allow akiHz to keep its background service running. " +
                                        "The tile still works, but it may start slower."
                                )
                            }
                            checkShizukuPermission()
                        }
                    )
                }
            }
        }
    }

    /**
     * Handles a new intent while the activity is alive.
     * @param intent incoming intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent, allowTilePreferences = true)
    }

    /** Registers Shizuku listeners and refreshes permission state. */
    override fun onResume() {
        super.onResume()
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        if (hasAcceptedWelcomeNotice) {
            checkShizukuPermission()
        }
        // Another app may have changed the rate while this screen was away.
        viewModel.loadCurrentRate()
    }

    /** Unregisters Shizuku listeners. */
    override fun onPause() {
        super.onPause()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    /** Releases the Shizuku user service. */
    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.releaseUserService(SHIZUKU_OWNER)
    }

    private fun updateLauncherIcon(themed: Boolean, mode: AppThemeMode): Boolean = try {
        launcherIconSwitcher.apply(themed, mode)
        true
    } catch (error: RuntimeException) {
        Timber.w(error, "Could not change launcher icon")
        // Older Android versions can fail after enabling the new alias. Restore the saved choice.
        runCatching {
            launcherIconSwitcher.apply(PreferencesHelper.themedAppIcon, PreferencesHelper.themeMode)
        }.onFailure { Timber.w(it, "Could not restore previous launcher icon") }
        showDialog("App icon unavailable", "Android could not change the app icon. Try again.")
        false
    }

    private fun checkShizukuPermission() {
        if (!ShizukuHelper.isBinderReady()) {
            showShizukuNotInstalledDialog()
            return
        }

        if (ShizukuHelper.hasPermission()) {
            bindUserServiceAndLoad()
        } else {
            ShizukuHelper.requestPermission(REQUEST_CODE_SHIZUKU)
        }
    }

    private fun bindUserServiceAndLoad() {
        if (isServiceBound && ShizukuHelper.isUserServiceBound()) return

        ShizukuHelper.acquireUserService(
            owner = SHIZUKU_OWNER,
            onConnected = {
                isServiceBound = true
                viewModel.onShizukuBound()
            },
            onFailed = { _, message ->
                showError(message)
            }
        )
    }

    private fun showShizukuNotInstalledDialog() {
        showDialog("Shizuku Required", "This app requires Shizuku to function.\n\n1. Install Shizuku\n2. Open Shizuku and follow setup\n3. Return to this app", false)
    }

    private fun showError(message: String) = showDialog("Error", message)

    /**
     * Routes the launch intent to the page it asks for.
     *
     * @param intent launch or new intent, may be null
     * @param allowTilePreferences whether a Quick Settings long press may navigate
     */
    private fun handleUpdateIntent(intent: Intent?, allowTilePreferences: Boolean) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATES, false) == true) {
            openUpdatesRequest.value += 1
            intent.removeExtra(EXTRA_OPEN_UPDATES)
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_FPS_MONITOR, false) == true) {
            openFpsMonitorRequest.value += 1
            intent.removeExtra(EXTRA_OPEN_FPS_MONITOR)
        }
        if (allowTilePreferences) {
            handleTilePreferencesIntent(intent)
        }
        val debugPageExtra = intent?.getStringExtra(EXTRA_DEBUG_PAGE)
        val openDebug = intent?.getBooleanExtra(EXTRA_OPEN_DEBUG, false) == true || debugPageExtra != null
        if (openDebug) {
            pendingDebugPage = debugPageExtra ?: "categories"
            if (pendingDebugPage.isNullOrBlank()) pendingDebugPage = "categories"
            // Auto-unlock debug options for CLI access.
            runCatching { PreferencesHelper.debugOptionsUnlocked = true }
            openDebugRequest.value += 1
            intent?.removeExtra(EXTRA_OPEN_DEBUG)
            intent?.removeExtra(EXTRA_DEBUG_PAGE)
        }
    }

    /**
     * Opens the FPS monitor settings when its Quick Settings tile is long-pressed.
     *
     * The system launches this activity for a long press and names the pressed
     * tile in [Intent.EXTRA_COMPONENT_NAME]. Long-pressing the refresh-rate tile
     * stays on the current page. The extra and action are consumed so an
     * activity recreation does not repeat the jump.
     */
    private fun handleTilePreferencesIntent(intent: Intent?) {
        if (intent == null || intent.action != TileService.ACTION_QS_TILE_PREFERENCES) return
        val tile = intent.tilePreferencesComponent()
        intent.action = null
        intent.removeExtra(Intent.EXTRA_COMPONENT_NAME)
        if (tile?.className == FpsMonitorTileService::class.java.name) {
            openFpsMonitorRequest.value += 1
        }
    }

    /** Reads the tile component from a long-press intent on every supported API level. */
    private fun Intent.tilePreferencesComponent(): ComponentName? {
        val parcelable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra<ComponentName>(Intent.EXTRA_COMPONENT_NAME)
        }
        if (parcelable != null) return parcelable
        // A few OEM builds store the component as a flattened string instead.
        return extras?.getString(Intent.EXTRA_COMPONENT_NAME)
            ?.let { ComponentName.unflattenFromString(it) }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (PreferencesHelper.notificationPermissionRequested) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            PreferencesHelper.notificationPermissionRequested = true
            return
        }
        // Only request once on first install; mark as requested before launching to avoid loops.
        PreferencesHelper.notificationPermissionRequested = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun showDialog(title: String, message: String, cancelable: Boolean = true) {
        dialogState.value = AppDialog(
            title = title,
            message = message,
            cancelable = cancelable
        )
    }
}
