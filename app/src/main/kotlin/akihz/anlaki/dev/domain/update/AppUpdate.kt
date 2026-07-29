package akihz.anlaki.dev.domain.update

/** A downloadable app release selected for this device. */
data class AppUpdate(
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val downloadUrl: String,
    val sha256: String?
)
