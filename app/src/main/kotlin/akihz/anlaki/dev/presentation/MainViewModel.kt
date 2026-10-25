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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.data.CustomProfileManager
import akihz.anlaki.dev.domain.TileRateSelection
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.domain.usecase.GetTileRatesUseCase
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import javax.inject.Inject
import kotlin.coroutines.resume

data class MainUiState(
    val supportedRates: List<Float> = emptyList(),
    val currentRate: Float? = null,
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
    private val shizukuOwner = "main_view_model_${System.identityHashCode(this)}"
    private var rateOperationInProgress = false

    init {
        viewModelScope.launch {
            PreferencesHelper.lastRateFlow.collect { rate ->
                _uiState.update { state ->
                    if (state.isServiceBound) {
                        state.copy(currentRate = rate)
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
                _uiState.update { it.copy(currentRate = rate) }
            }
        }
    }

    /**
     * Applies the selected refresh rate.
     * @param hz rate to apply
     */
    fun selectRate(hz: Float) {
        if (_uiState.value.isLoading) return

        runWithUserService {
            // Show the selected rate once connected; settings writes may take several IPC calls.
            _uiState.update { it.copy(currentRate = hz) }
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(hz)
            }
            result.onSuccess {
                _uiState.update { it.copy(currentRate = hz) }
            }.onError { _, message ->
                _uiState.update { it.copy(error = message) }
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
        runWithUserService {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.resetToDefaults()
            }
            result.onSuccess {
                loadCurrentRate()
            }.onError { _, message ->
                _uiState.update { it.copy(error = message) }
            }
        }
    }

    private fun runWithUserService(action: suspend () -> Unit) {
        if (rateOperationInProgress) return
        rateOperationInProgress = true
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // The UI's bound flag can outlive the service. Acquire the live connection
                // for every operation, just as the Quick Settings tile does.
                val connection = withTimeoutOrNull(10_000L) {
                    awaitUserService()
                } ?: Result.error(ErrorType.TIMEOUT, "Timed out connecting to Shizuku. Try again.")
                connection.onError { _, message ->
                    _uiState.update {
                        it.copy(isServiceBound = ShizukuHelper.isUserServiceBound(), error = message)
                    }
                }
                if (connection.isError) return@launch

                _uiState.update { it.copy(isServiceBound = true) }
                action()
            } finally {
                rateOperationInProgress = false
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun awaitUserService(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { ShizukuHelper.releaseUserService(shizukuOwner) }
        ShizukuHelper.acquireUserService(
            owner = shizukuOwner,
            onConnected = {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            },
            onFailed = { type, message ->
                if (continuation.isActive) continuation.resume(Result.error(type, message))
            }
        )
    }

    /** Releases the connection owned by this screen when its ViewModel is cleared. */
    override fun onCleared() {
        ShizukuHelper.releaseUserService(shizukuOwner)
        super.onCleared()
    }
}
