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

/**
 * Returns the first syntactically valid SHA-256 digest from [candidates].
 *
 * GitHub may expose the digest on the asset and in release metadata. Requiring
 * one valid source prevents unverified APK installation.
 */
fun selectValidSha256(vararg candidates: String?): String? =
    candidates.asSequence()
        .filterNotNull()
        .map { it.removePrefix("sha256:").lowercase() }
        .firstOrNull { digest ->
            digest.length == 64 && digest.all { it in '0'..'9' || it in 'a'..'f' }
        }
