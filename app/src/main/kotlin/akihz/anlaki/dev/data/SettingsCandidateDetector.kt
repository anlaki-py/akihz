package akihz.anlaki.dev.data

/** Ranks settings entries that may control display refresh rate. */
object SettingsCandidateDetector {
    private val keywords = listOf(
        "refresh", "fps", "hz", "peak", "min_refresh", "display_mode", "motion", "smooth"
    )
    private val knownKeys = OemSettingsStrategy.getSupportedOemNames()
        .filterNot { it == "Auto-detect" }
        .flatMap { name ->
            val set = OemSettingsStrategy.resolveByName(name)
            set.readKeys + set.writeKeys + set.lockKeys
        }
        .map { it.key }
        .toSet()

    /**
     * Ranks entries from a current scan and optional labeled snapshots.
     *
     * @param current current namespaced settings values
     * @param snapshots baseline and per-rate snapshots
     * @return likely candidates sorted by confidence and name
     */
    fun detect(
        current: Map<String, String>,
        snapshots: List<SettingsSnapshot>
    ): List<SettingsCandidate> = current.mapNotNull { (id, value) ->
        val name = id.substringAfter('/')
        val changed = snapshots.mapNotNull { it.values[id] }.distinct().size > 1
        val known = name in knownKeys
        val keyword = keywords.firstOrNull { name.contains(it, ignoreCase = true) }
        val rateLike = value.toFloatOrNull()?.let { it in 24f..360f } == true
        val score = (if (changed) 100 else 0) + (if (known) 50 else 0) +
            (if (keyword != null) 25 else 0) + (if (rateLike) 5 else 0)
        if (score == 0) return@mapNotNull null
        val namespace = runCatching {
            OemSettingsStrategy.Namespace.valueOf(id.substringBefore('/').uppercase())
        }.getOrNull() ?: return@mapNotNull null
        val reasons = listOfNotNull(
            "changed across snapshots".takeIf { changed },
            "known OEM key".takeIf { known },
            keyword?.let { "matched “$it”" },
            "refresh-like value".takeIf { rateLike }
        )
        SettingsCandidate(namespace, name, value, score, reasons.joinToString())
    }.sortedWith(compareByDescending<SettingsCandidate> { it.score }.thenBy { it.name })
}
