package akihz.anlaki.dev.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for querying installed applications with filtering and search.
 *
 * Uses PackageManager with proper API compatibility and launcher app detection
 * to ensure all user-facing apps are shown correctly.
 */
class AppListRepository(private val context: Context) {

    private val pm = context.packageManager

    /**
     * Information about an installed app.
     */
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val isSystemApp: Boolean,
        val isUpdatedSystemApp: Boolean,
        val hasLauncherIntent: Boolean
    )

    enum class Filter {
        ALL, USER, SYSTEM
    }

    /**
     * Returns all installed apps matching the filter and search query.
     *
     * Uses getInstalledApplicationsCompat() for API 33+ compatibility.
     * Loads on IO dispatcher to avoid blocking the main thread.
     *
     * @param filter which apps to include
     * @param query search text to filter by app name or package name
     * @return sorted list of matching apps
     */
    suspend fun getApps(filter: Filter = Filter.ALL, query: String = ""): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val appInfos = pm.getInstalledApplicationsCompat(0L)
            val launcherPackages = getLauncherPackages()

            appInfos
                .map { app ->
                    AppInfo(
                        packageName = app.packageName,
                        appName = app.loadLabel(pm).toString(),
                        isSystemApp = isSystemApp(app),
                        isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                        hasLauncherIntent = launcherPackages.contains(app.packageName)
                    )
                }
                .filter { app ->
                    when (filter) {
                        Filter.ALL -> true
                        Filter.USER -> !app.isSystemApp || app.isUpdatedSystemApp
                        Filter.SYSTEM -> app.isSystemApp && !app.isUpdatedSystemApp
                    }
                }
                .filter { app ->
                    if (query.isBlank()) true
                    else app.appName.contains(query, ignoreCase = true) ||
                            app.packageName.contains(query, ignoreCase = true)
                }
                .sortedWith(
                    compareByDescending<AppInfo> { it.hasLauncherIntent }
                        .thenBy { it.appName.lowercase() }
                )
        }

    /**
     * Loads the app icon for a specific package.
     * Call this lazily from the UI layer, not during bulk loading.
     */
    fun loadAppIcon(packageName: String): Drawable? {
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Gets the set of packages that have a launcher intent (CATEGORY_LAUNCHER).
     * These are the apps that appear in the user's app drawer.
     */
    private fun getLauncherPackages(): Set<String> {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolveInfos.map { it.activityInfo.packageName }.toSet()
    }

    /**
     * Determines if an app is a pre-installed system app (not updated via Play Store).
     *
     * Updated system apps (like YouTube, Gmail after Play Store update) are treated as user apps
     * since they are user-facing and appear in the launcher.
     */
    private fun isSystemApp(app: ApplicationInfo): Boolean {
        return (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
    }
}

/**
 * Compatibility helper for getInstalledApplications that works across all API levels.
 *
 * API 33+ (Tiramisu) uses ApplicationInfoFlags.of(Long)
 * Older APIs use the deprecated int-based method.
 */
private fun PackageManager.getInstalledApplicationsCompat(flags: Long): List<ApplicationInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags))
    } else {
        @Suppress("DEPRECATION")
        getInstalledApplications(flags.toInt())
    }
}