package akihz.anlaki.dev.data

import android.content.Context
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result

/** Coordinates custom profile discovery, transactional writes, and restoration. */
object CustomProfileManager {
    private lateinit var store: CustomProfileStore

    /** Initializes custom-profile persistence. */
    fun init(context: Context) {
        if (!::store.isInitialized) store = CustomProfileStore(context.applicationContext)
    }

    val warningAcknowledged: Boolean get() = store.warningAcknowledged

    /** Records acceptance of the experimental warning. */
    fun acknowledgeWarning() {
        store.warningAcknowledged = true
    }

    /** Returns the current persisted profile. */
    fun profile(): CustomRefreshProfile = store.loadProfile()

    /** Saves an edited draft and invalidates its previous test. */
    fun saveDraft(profile: CustomRefreshProfile) {
        store.saveProfile(profile.copy(tested = false, enabled = false, originals = emptyMap()))
    }

    /** Returns stored discovery snapshots. */
    fun snapshots(): List<SettingsSnapshot> = loadCompactedSnapshots()

    /** Captures a baseline or a bounded difference snapshot for one display rate. */
    fun captureSnapshot(label: String): Result<SettingsSnapshot> {
        val existing = loadCompactedSnapshots()
        if (label != SettingsSnapshotPolicy.BASELINE_LABEL &&
            existing.none { it.label == SettingsSnapshotPolicy.BASELINE_LABEL }
        ) {
            return Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Capture a baseline before capturing a refresh rate."
            )
        }
        val scan = scanAll()
        if (scan.isError) return scan.map { SettingsSnapshot(label, it) }
        val merge = SettingsSnapshotPolicy.merge(
            existing = existing,
            label = label,
            current = scan.getOrNull().orEmpty()
        ) ?: return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Baseline is unavailable.")
        store.saveSnapshots(merge.retained)
        return Result.success(merge.captured)
    }

    /** Clears all discovery snapshots. */
    fun clearSnapshots() = store.saveSnapshots(emptyList())

    /** Scans settings and returns ranked refresh-rate candidates. */
    fun candidates(): Result<List<SettingsCandidate>> =
        scanAll().map { SettingsCandidateDetector.detect(it, loadCompactedSnapshots()) }

    /** Applies a rate through the enabled custom profile. */
    fun applyRate(rate: Float): Result<Unit> {
        val profile = profile()
        if (!profile.enabled) {
            return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Custom profile is not enabled.")
        }
        return applyTransaction(profile, rate).map { Unit }
    }

    /** Applies temporary test values and returns the captured pre-test values. */
    fun beginTest(rate: Float): Result<Map<String, OriginalSetting>> {
        val profile = profile()
        profile.validationError()?.let {
            return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, it)
        }
        return applyTransaction(profile, rate, journalRestore = true)
    }

    /** Restores values left by a test interrupted before confirmation. */
    fun recoverInterruptedTest(): Result<Unit> {
        val pending = store.loadPendingRestore()
        if (pending.isEmpty()) return Result.success(Unit)
        return restore(pending)
    }

    /** Marks the current draft as successfully tested. */
    fun markTested() = store.saveProfile(profile().copy(tested = true))

    /** Enables a tested profile after capturing every writable key. */
    fun enable(): Result<Unit> {
        val profile = profile()
        profile.validationError()?.let {
            return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, it)
        }
        if (!profile.tested) {
            return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Test this profile before enabling it.")
        }
        val originals = captureOriginals(profile.keys.filter { it.canWrite })
        if (originals.isError) return originals.map { Unit }
        store.saveProfile(profile.copy(enabled = true, originals = originals.getOrNull().orEmpty()))
        return Result.success(Unit)
    }

    /** Restores activation-time values and disables the custom profile. */
    fun disable(): Result<Unit> {
        val profile = profile()
        val result = restore(profile.originals)
        if (result.isSuccess) {
            store.saveProfile(profile.copy(enabled = false, originals = emptyMap()))
        }
        return result
    }

    /** Restores captured values, deleting keys that did not originally exist. */
    fun restore(originals: Map<String, OriginalSetting>): Result<Unit> {
        val failures = originals.mapNotNull { (id, original) ->
            val key = keyFromId(id) ?: return@mapNotNull id
            val result = if (original.existed) {
                ShizukuHelper.putSetting(key.namespace, key.name, original.value.orEmpty())
            } else {
                ShizukuHelper.deleteSetting(key.namespace, key.name)
            }
            id.takeIf { result.isError }
        }
        return failures.toResult("Failed to restore").also {
            if (it.isSuccess) store.clearPendingRestore()
        }
    }

    private fun applyTransaction(
        profile: CustomRefreshProfile,
        rate: Float,
        journalRestore: Boolean = false
    ): Result<Map<String, OriginalSetting>> {
        val keys = profile.keys.filter { it.canWrite }
        val originalsResult = captureOriginals(keys)
        if (originalsResult.isError) return originalsResult
        val originals = originalsResult.getOrNull().orEmpty()
        if (journalRestore) store.savePendingRestore(originals)
        val failures = keys.mapNotNull { key ->
            val value = key.values[CustomRefreshProfile.rateKey(rate)]
                ?: return@mapNotNull key.id
            key.id.takeIf { ShizukuHelper.putSetting(key.namespace, key.name, value).isError }
        }
        if (failures.isNotEmpty()) {
            restore(originals)
            return Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Failed to update: ${failures.joinToString()}. Previous values were restored."
            )
        }
        return Result.success(originals)
    }

    private fun captureOriginals(keys: List<CustomSettingsKey>): Result<Map<String, OriginalSetting>> {
        val originals = linkedMapOf<String, OriginalSetting>()
        keys.forEach { key ->
            val result = ShizukuHelper.getSetting(key.namespace, key.name)
            if (result.isError) {
                return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Could not read ${key.id}.")
            }
            val value = result.getOrNull()
            originals[key.id] = OriginalSetting(value != null, value)
        }
        return Result.success(originals)
    }

    private fun scanAll(): Result<Map<String, String>> {
        val values = linkedMapOf<String, String>()
        OemSettingsStrategy.Namespace.entries.forEach { namespace ->
            val result = ShizukuHelper.listSettings(namespace)
            if (result.isError) {
                return Result.error(
                    ErrorType.COMMAND_EXECUTION_FAILED,
                    "Could not scan ${namespace.name.lowercase()}."
                )
            }
            result.getOrNull().orEmpty().forEach { (key, value) ->
                values["${namespace.name.lowercase()}/$key"] = value
            }
        }
        return Result.success(values)
    }

    private fun loadCompactedSnapshots(): List<SettingsSnapshot> {
        val stored = store.loadSnapshots()
        val compacted = SettingsSnapshotPolicy.compact(stored)
        if (compacted != stored) store.saveSnapshots(compacted)
        return compacted
    }

    private fun keyFromId(id: String): CustomSettingsKey? {
        val namespace = runCatching {
            OemSettingsStrategy.Namespace.valueOf(id.substringBefore('/').uppercase())
        }.getOrNull() ?: return null
        return CustomSettingsKey(namespace, id.substringAfter('/'))
    }

    private fun List<String>.toResult(prefix: String): Result<Unit> =
        if (isEmpty()) Result.success(Unit) else Result.error(
            ErrorType.COMMAND_EXECUTION_FAILED,
            "$prefix: ${joinToString()}"
        )
}
