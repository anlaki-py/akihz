package akihz.anlaki.dev.domain.update

/** Release stream used when checking for app updates. */
enum class UpdateChannel(
    val label: String,
    val acceptsPrereleases: Boolean
) {
    Stable("Stable", false),
    Prerelease("Pre-release", true)
}
