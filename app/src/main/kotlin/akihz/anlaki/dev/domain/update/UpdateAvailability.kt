package akihz.anlaki.dev.domain.update

/** Result of comparing the installed build with its selected update channel. */
enum class UpdateAvailability {
    Available,
    UpToDate,
    AheadOfStable
}

/**
 * Compares local and remote version codes for the selected [channel].
 *
 * A prerelease may have a higher version code than the latest stable release.
 * That state waits for the next stable instead of attempting a destructive downgrade.
 *
 * @param currentVersionCode installed app version code
 * @param latestVersionCode newest version code published in the selected channel
 * @param channel channel selected by the user
 * @return the appropriate update action
 */
fun resolveUpdateAvailability(
    currentVersionCode: Long,
    latestVersionCode: Long,
    channel: UpdateChannel
): UpdateAvailability = when {
    latestVersionCode > currentVersionCode -> UpdateAvailability.Available
    channel == UpdateChannel.Stable && currentVersionCode > latestVersionCode ->
        UpdateAvailability.AheadOfStable
    else -> UpdateAvailability.UpToDate
}
