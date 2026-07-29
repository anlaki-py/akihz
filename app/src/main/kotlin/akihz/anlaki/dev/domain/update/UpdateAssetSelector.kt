package akihz.anlaki.dev.domain.update

/**
 * Selects the best release APK name for the device ABI order.
 *
 * @param assetNames filenames attached to the release
 * @param tag release tag included in generated APK filenames
 * @param supportedAbis device ABIs ordered by Android preference
 * @return the matching ABI APK, a universal fallback, or null
 */
fun selectUpdateApkName(
    assetNames: List<String>,
    tag: String,
    supportedAbis: List<String>
): String? {
    val preferredAbis = supportedAbis + "universal"
    return preferredAbis.firstNotNullOfOrNull { abi ->
        val expectedName = "akihz-$tag-$abi.apk"
        assetNames.firstOrNull { it == expectedName }
    }
}
