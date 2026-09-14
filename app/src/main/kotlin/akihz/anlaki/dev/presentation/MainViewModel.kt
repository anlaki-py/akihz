package akihz.anlaki.dev.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.data.CustomProfileManager
import akihz.anlaki.dev.domain.TileRateSelection
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.domain.usecase.GetTileRatesUseCase
import akihz.anlaki.dev.data.PreferencesHelper
import javax.inject.Inject

data class MainUiState(
    val supportedRates: List<Float> = emptyList(),
    val currentRate: Float? = null,
    val selectedRate: Float? = null,
    val excludedTileRates: Set<Float> = emptySet(),
    val isShizukuReady: Boolean = false,
    val isServiceBound: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshRateRepository: RefreshRateRepository,
    private val getTileRates: GetTileRatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            PreferencesHelper.lastRateFlow.collect { rate ->
                _uiState.update { state ->
                    if (state.isServiceBound) {
                        state.copy(currentRate = rate, selectedRate = rate)
                    } else {
                        state
                    }
                }
            }
        }
    }

    /** Marks Shizuku as bound and loads rates. */
    fun onShizukuBound() {
        _uiState.update { it.copy(isServiceBound = true, isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            CustomProfileManager.recoverInterruptedTest()
        }
        loadSupportedRates()
        loadCurrentRate()
    }

    /**
     * Updates the Shizuku ready flag.
     * @param ready true when Shizuku is ready
     */
    fun onShizukuReadyChanged(ready: Boolean) {
        _uiState.update { it.copy(isShizukuReady = ready) }
    }

    /** Clears the current error message. */
    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    /** Loads supported rates and tile exclusions. */
    fun loadSupportedRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getTileRates(PreferencesHelper.excludedTileRates)
            result.onSuccess { tileRates ->
                if (tileRates.excludedRates != PreferencesHelper.excludedTileRates) {
                    PreferencesHelper.excludedTileRates = tileRates.excludedRates
                }
                _uiState.update {
                    it.copy(
                        supportedRates = tileRates.allRates,
                        excludedTileRates = tileRates.excludedRates,
                        isLoading = false
                    )
                }
            }.onError { _, message ->
                _uiState.update { it.copy(error = "Failed to detect supported rates: $message", isLoading = false) }
            }
        }
    }

    /** Reloads available rates after the active custom profile changes. */
    fun onCustomProfileChanged() {
        loadSupportedRates()
        loadCurrentRate()
    }

    /** Loads the active refresh rate from the device. */
    fun loadCurrentRate() {
        if (!_uiState.value.isServiceBound) return

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.getCurrentRate()
            }
            result.onSuccess { rate ->
                _uiState.update { it.copy(currentRate = rate, selectedRate = rate) }
            }
        }
    }

    /**
     * Applies the selected refresh rate.
     * @param hz rate to apply
     */
    fun selectRate(hz: Float) {
        val state = _uiState.value
        if (state.isLoading || !state.isServiceBound || !ShizukuHelper.hasPermission()) return

        // Reflect the user's choice immediately; OEM writes can take several IPC round trips.
        _uiState.update { it.copy(selectedRate = hz, isLoading = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(hz)
            }
            result.onSuccess {
                _uiState.update { it.copy(currentRate = hz, selectedRate = hz, isLoading = false) }
            }.onError { _, message ->
                _uiState.update { it.copy(error = message, isLoading = false) }
                loadCurrentRate()
            }
        }
    }

    /** Includes or excludes a refresh rate from Quick Settings tile cycling. */
    fun setTileRateIncluded(hz: Float, included: Boolean) {
        val state = _uiState.value
        val excludedRates = TileRateSelection.setIncluded(
            state.supportedRates,
            state.excludedTileRates,
            hz,
            included
        )
        if (excludedRates == state.excludedTileRates) return
        PreferencesHelper.excludedTileRates = excludedRates
        _uiState.update { it.copy(excludedTileRates = excludedRates) }
    }

    /** Restores adaptive system refresh rate settings. */
    fun resetToDefaults() {
        if (!ShizukuHelper.isBinderReady()) {
            _uiState.update { it.copy(error = "Shizuku is not running.") }
            return
        }
        if (!ShizukuHelper.hasPermission()) {
            _uiState.update { it.copy(error = "Shizuku permission not granted.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.resetToDefaults()
            }
            result.onSuccess {
                loadCurrentRate()
                _uiState.update { it.copy(isLoading = false) }
            }.onError { _, message ->
                _uiState.update { it.copy(error = message, isLoading = false) }
            }
        }
    }
}
