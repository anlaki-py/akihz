package akihz.anlaki.dev.data

import android.os.Build
import akihz.anlaki.dev.domain.update.AppUpdate
import akihz.anlaki.dev.domain.update.UpdateChannel
import akihz.anlaki.dev.domain.update.selectUpdateApkName
import akihz.anlaki.dev.domain.update.selectValidSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES_API = "https://api.github.com/repos/anlaki-py/akihz/releases?per_page=20"
private const val METADATA_FILE = "release-metadata.json"
private const val PACKAGE_NAME = "akihz.anlaki.dev"

/** HTTP failure returned by the GitHub update service. */
internal class UpdateHttpException(val statusCode: Int) :
    IOException("Update server returned HTTP $statusCode")

/** Resolves published GitHub releases into an APK suitable for this device. */
class GitHubUpdateRepository {

    /**
     * Finds the newest release permitted by [channel].
     *
     * The pre-release channel also accepts stable releases so users never remain
     * on an older beta after its stable successor is published.
     */
    suspend fun findLatest(channel: UpdateChannel): AppUpdate = withContext(Dispatchers.IO) {
        val releases = JSONArray(request(RELEASES_API))
        val release = (0 until releases.length())
            .map(releases::getJSONObject)
            .firstOrNull {
                !it.getBoolean("draft") &&
                    (channel.acceptsPrereleases || !it.getBoolean("prerelease"))
            } ?: error("No ${channel.label.lowercase()} release is available")

        val assets = release.getJSONArray("assets")
        val metadataAsset = assets.objects()
            .firstOrNull { it.getString("name") == METADATA_FILE }
            ?: error("Release metadata is missing")
        val metadata = JSONObject(request(metadataAsset.getString("browser_download_url")))
        check(metadata.getString("packageName") == PACKAGE_NAME) {
            "Release metadata belongs to a different app"
        }
        val apk = selectApk(assets, release.getString("tag_name"))
        val metadataDigest = metadata.getJSONArray("assets").objects()
            .firstOrNull { it.getString("file") == apk.getString("name") }
            ?.optString("sha256")
        val digest = selectValidSha256(apk.optString("digest"), metadataDigest)
            ?: error("Release APK has no valid SHA-256 digest")

        AppUpdate(
            versionName = metadata.getString("versionName"),
            versionCode = metadata.getLong("versionCode"),
            apkName = apk.getString("name"),
            downloadUrl = apk.getString("browser_download_url"),
            sha256 = digest
        )
    }

    private fun selectApk(assets: JSONArray, tag: String): JSONObject {
        val apks = assets.objects().filter { it.getString("name").endsWith(".apk") }
        val selectedName = selectUpdateApkName(
            assetNames = apks.map { it.getString("name") },
            tag = tag,
            supportedAbis = Build.SUPPORTED_ABIS.toList()
        ) ?: error("No compatible APK is available for this device")
        return apks.first { it.getString("name") == selectedName }
    }

    private fun request(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "akiHz-Android")
            if (connection.responseCode !in 200..299) {
                throw UpdateHttpException(connection.responseCode)
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)
}
