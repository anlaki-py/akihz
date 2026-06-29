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
    val isLoading: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshRateRepository: RefreshRateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onShizukuBound() {
        _uiState.update { it.copy(isServiceBound = true) }
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
        if (!_uiState.value.isServiceBound || !ShizukuHelper.hasPermission()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(hz)
            }
            result.onSuccess {
                _uiState.update { it.copy(currentRate = hz, selectedRate = hz, isLoading = false) }
            }.onError { _, message ->
                _uiState.update { it.copy(error = message, isLoading = false) }
            }
        }
    }

    fun resetToDefaults(onReady: () -> Unit = {}) {
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
                PreferencesHelper.desiredRate = 0f
                loadCurrentRate()
                _uiState.update { it.copy(isLoading = false) }
            }.onError { _, message ->
                _uiState.update { it.copy(error = message, isLoading = false) }
            }
        }
    }
}
