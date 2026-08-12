package akihz.anlaki.dev.domain.update

/** User-selected interval for automatic app update checks. */
enum class UpdateCheckFrequency(
    val label: String,
    val intervalDays: Long?
) {
    Daily("Daily", 1),
    EveryThreeDays("Every 3 days", 3),
    Weekly("Weekly", 7),
    Never("Never", null);

    companion object {
        /** Restores a frequency while safely handling values from newer app versions. */
        fun fromStoredValue(value: String?): UpdateCheckFrequency =
            entries.firstOrNull { it.name == value } ?: Daily
    }
}
