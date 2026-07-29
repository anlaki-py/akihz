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
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.utils.PreferencesHelper
import javax.inject.Inject

data class MainUiState(
    val supportedRates: List<Float> = emptyList(),
    val currentRate: Float? = null,
    val selectedRate: Float? = null,
    val isShizukuReady: Boolean = false,
    val isServiceBound: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshRateRepository: RefreshRateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onShizukuBound() {
        _uiState.update { it.copy(isServiceBound = true, isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            CustomProfileManager.recoverInterruptedTest()
        }
        loadSupportedRates()
        loadCurrentRate()
    }

    fun onShizukuReadyChanged(ready: Boolean) {
        _uiState.update { it.copy(isShizukuReady = ready) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    fun loadSupportedRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.getSupportedRates()
            }
            result.onSuccess { rates ->
                _uiState.update { it.copy(supportedRates = rates, isLoading = false) }
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
