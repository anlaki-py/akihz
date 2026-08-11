package akihz.anlaki.dev.presentation

import akihz.anlaki.dev.data.CustomRefreshProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class CustomDraftSaver(
    private val scope: CoroutineScope,
    private val save: (CustomRefreshProfile) -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val delayMillis: Long = DEFAULT_DELAY_MS
) {
    @Volatile private var pendingJob: Job? = null
    @Volatile private var latest: CustomRefreshProfile? = null
    private val saveMutex = Mutex()

    fun schedule(profile: CustomRefreshProfile) {
        latest = profile
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(delayMillis)
            persist(profile)
        }
    }

    suspend fun flush() {
        pendingJob?.cancelAndJoin()
        latest?.let { persist(it) }
    }

    private suspend fun persist(profile: CustomRefreshProfile) {
        withContext(dispatcher) {
            saveMutex.withLock { save(profile) }
        }
        if (latest == profile) latest = null
    }

    private companion object {
        const val DEFAULT_DELAY_MS = 400L
    }
}
