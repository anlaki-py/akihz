package akihz.anlaki.dev.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * Repository for querying installed applications with filtering and search.
 */
class AppListRepository(private val context: Context) {

    private val pm = context.packageManager

    /**
     * Information about an installed app.
     */
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable?,
        val isSystemApp: Boolean
    )

    enum class Filter {
        ALL, USER, SYSTEM
    }

    /**
     * Returns all installed apps matching the filter and search query.
     *
     * @param filter which apps to include
     * @param query search text to filter by app name or package name
     * @return sorted list of matching apps
     */
    fun getApps(filter: Filter = Filter.ALL, query: String = ""): List<AppInfo> {
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    appName = app.loadLabel(pm).toString(),
                    icon = app.loadIcon(pm),
                    isSystemApp = isSystemApp(app)
                )
            }
            .filter { app ->
                when (filter) {
                    Filter.ALL -> true
                    Filter.USER -> !app.isSystemApp
                    Filter.SYSTEM -> app.isSystemApp
                }
            }
            .filter { app ->
                if (query.isBlank()) true
                else app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            }
            .sortedBy { it.appName.lowercase() }

        return apps
    }

    /**
     * Determines if an app is a system app.
     *
     * A system app has FLAG_SYSTEM set AND does NOT have FLAG_UPDATED_SYSTEM_APP.
     * Updated system apps (like updated Google apps) are treated as user apps.
     */
    private fun isSystemApp(app: ApplicationInfo): Boolean {
        return (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
    }
}