package akihz.anlaki.dev.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import akihz.anlaki.dev.data.CustomProfileManager
import akihz.anlaki.dev.data.CustomRefreshProfile
import akihz.anlaki.dev.data.CustomSettingsKey
import akihz.anlaki.dev.data.OemSettingsStrategy
import akihz.anlaki.dev.data.OriginalSetting
import akihz.anlaki.dev.data.SettingsCandidate
import akihz.anlaki.dev.data.SettingsSnapshot
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CustomKeysUiState(
    val profile: CustomRefreshProfile = CustomRefreshProfile(),
    val detectedRates: List<Float> = emptyList(),
    val candidates: List<SettingsCandidate> = emptyList(),
    val snapshots: List<SettingsSnapshot> = emptyList(),
    val warningAcknowledged: Boolean = false,
    val busy: Boolean = false,
    val testSeconds: Int? = null,
    val message: String? = null
)

private data class LoadedCustomKeysContent(
    val profile: CustomRefreshProfile,
    val detectedRates: List<Float>,
    val snapshots: List<SettingsSnapshot>,
    val warningAcknowledged: Boolean
)

@HiltViewModel
class CustomKeysViewModel @Inject constructor(
    private val repository: RefreshRateRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CustomKeysUiState())
    val state: StateFlow<CustomKeysUiState> = _state.asStateFlow()
    private var testOriginals: Map<String, OriginalSetting>? = null
    private var countdown: Job? = null
    private var reloadJob: Job? = null
    @Volatile private var screenGeneration = 0

    /** Reloads persisted configuration and physical display rates. */
    fun reload() {
        val generation = screenGeneration
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                LoadedCustomKeysContent(
                    profile = CustomProfileManager.profile(),
                    detectedRates = repository.getSupportedRates().getOrNull().orEmpty(),
                    snapshots = CustomProfileManager.snapshots(),
                    warningAcknowledged = CustomProfileManager.warningAcknowledged
                )
            }
            if (generation != screenGeneration) return@launch
            _state.update {
                it.copy(
                    profile = content.profile,
                    detectedRates = content.detectedRates,
                    snapshots = content.snapshots,
                    warningAcknowledged = content.warningAcknowledged
                )
            }
        }
    }

    /** Releases scan-heavy state when the custom-key screen leaves composition. */
    fun releaseTransientState() {
        screenGeneration++
        reloadJob?.cancel()
        reloadJob = null
        countdown?.cancel()
        countdown = null
        if (testOriginals != null) finishTest(success = false, showMessage = false)
        _state.update {
            it.copy(
                candidates = emptyList(),
                snapshots = emptyList(),
                testSeconds = null,
                message = null
            )
        }
    }

    /** Accepts the one-time advanced feature warning. */
    fun acknowledgeWarning() {
        CustomProfileManager.acknowledgeWarning()
        _state.update { it.copy(warningAcknowledged = true) }
    }

    /** Adds or removes a rate from the custom profile. */
    fun toggleRate(rate: Float) = edit { profile ->
        val rates = if (rate in profile.rates) profile.rates - rate else profile.rates + rate
        profile.copy(rates = rates.sorted())
    }

    /** Adds a discovered or manually entered key. */
    fun addKey(namespace: OemSettingsStrategy.Namespace, name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || _state.value.profile.keys.any {
                it.namespace == namespace && it.name == cleanName
            }) return
        val snapshots = _state.value.snapshots
        edit { profile ->
            val id = "${namespace.name.lowercase()}/$cleanName"
            val values = profile.rates.associate { rate ->
                val label = "${CustomRefreshProfile.rateLabel(rate)} Hz"
                val inferred = snapshots.lastOrNull { it.label == label }?.values?.get(id)
                CustomRefreshProfile.rateKey(rate) to
                    (inferred ?: CustomRefreshProfile.rateLabel(rate))
            }
            profile.copy(keys = profile.keys + CustomSettingsKey(namespace, cleanName, values = values))
        }
    }

    /** Removes a selected key. */
    fun removeKey(id: String) = edit { it.copy(keys = it.keys.filterNot { key -> key.id == id }) }

    /** Changes a selected key's read/write roles. */
    fun updateRoles(id: String, read: Boolean, write: Boolean) = edit { profile ->
        profile.copy(keys = profile.keys.map {
            if (it.id == id) it.copy(canRead = read, canWrite = write) else it
        })
    }

    /** Changes one selected key's value for a profile rate. */
    fun updateValue(id: String, rate: Float, value: String) = edit { profile ->
        profile.copy(keys = profile.keys.map {
            if (it.id == id) {
                it.copy(values = it.values + (CustomRefreshProfile.rateKey(rate) to value))
            } else it
        })
    }

    /** Scans all settings namespaces and ranks likely candidates. */
    fun scan() {
        val generation = screenGeneration
        runBusy {
            val result = CustomProfileManager.candidates()
            result.onSuccess { candidates ->
                if (generation == screenGeneration) {
                    _state.update {
                        it.copy(candidates = candidates, message = "Found ${candidates.size} candidates.")
                    }
                }
            }.onError { _, message -> if (generation == screenGeneration) show(message) }
        }
    }

    /** Captures a labeled settings snapshot for guided diffing. */
    fun capture(label: String) {
        val generation = screenGeneration
        runBusy {
            val result = CustomProfileManager.captureSnapshot(label)
            result.onSuccess {
                if (generation == screenGeneration) {
                    _state.update { state ->
                        state.copy(snapshots = CustomProfileManager.snapshots(), message = "Captured $label.")
                    }
                }
            }.onError { _, message -> if (generation == screenGeneration) show(message) }
        }
    }

    /** Clears all discovery snapshots. */
    fun clearSnapshots() {
        CustomProfileManager.clearSnapshots()
        _state.update { it.copy(snapshots = emptyList(), candidates = emptyList()) }
    }

    /** Starts the guarded 15-second profile test. */
    fun beginTest(rate: Float) = runBusy {
        val result = CustomProfileManager.beginTest(rate)
        result.onSuccess { originals ->
            testOriginals = originals
            countdown?.cancel()
            countdown = viewModelScope.launch {
                for (second in 15 downTo 1) {
                    _state.update { it.copy(testSeconds = second) }
                    delay(1_000)
                }
                finishTest(false)
            }
        }.onError { _, message -> show(message) }
    }

    /** Confirms or rejects the active test and always restores pre-test values. */
    fun finishTest(success: Boolean) {
        finishTest(success = success, showMessage = true)
    }

    private fun finishTest(success: Boolean, showMessage: Boolean) {
        countdown?.cancel()
        countdown = null
        val originals = testOriginals ?: return
        testOriginals = null
        _state.update { it.copy(testSeconds = null, busy = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val restored = CustomProfileManager.restore(originals)
            if (success && restored.isSuccess) CustomProfileManager.markTested()
            _state.update {
                it.copy(
                    profile = CustomProfileManager.profile(),
                    busy = false,
                    message = if (!showMessage) null else if (success && restored.isSuccess) {
                        "Test passed. Values restored."
                    } else {
                        restored.getErrorOrNull()?.message ?: "Test cancelled. Values restored."
                    }
                )
            }
        }
    }

    /** Enables or disables the tested custom profile. */
    fun setEnabled(enabled: Boolean, onChanged: () -> Unit) = runBusy {
        val result = if (enabled) CustomProfileManager.enable() else CustomProfileManager.disable()
        result.onSuccess {
            _state.update { it.copy(profile = CustomProfileManager.profile()) }
            onChanged()
        }.onError { _, message -> show(message) }
    }

    /** Clears the transient status message. */
    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun edit(transform: (CustomRefreshProfile) -> CustomRefreshProfile) {
        val profile = transform(_state.value.profile)
        CustomProfileManager.saveDraft(profile)
        _state.update { it.copy(profile = CustomProfileManager.profile()) }
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            block()
            _state.update { it.copy(busy = false) }
        }
    }

    private fun show(message: String) = _state.update { it.copy(message = message) }

    override fun onCleared() {
        if (testOriginals != null) finishTest(success = false, showMessage = false)
        super.onCleared()
    }
}
