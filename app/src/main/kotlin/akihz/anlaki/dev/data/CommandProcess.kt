package akihz.anlaki.dev.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Runs bounded shell processes for both privilege backends. */
internal object CommandProcess {
    /** Returns output or an ERROR message for [command]. */
    fun execute(command: List<String>): String {
        val executor = Executors.newSingleThreadExecutor()
        var process: Process? = null
        return try {
            val runningProcess = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            process = runningProcess
            val outputFuture = executor.submit<String> {
                BufferedReader(InputStreamReader(runningProcess.inputStream)).use {
                    it.readText().trim()
                }
            }

            if (!runningProcess.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runningProcess.destroyForcibly()
                outputFuture.cancel(true)
                return "ERROR: Command timed out"
            }

            val output = outputFuture.get(OUTPUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            when {
                runningProcess.exitValue() != 0 -> "ERROR: ${output.ifBlank { "Exit code ${runningProcess.exitValue()}" }}"
                output.isNotEmpty() -> output
                else -> "OK"
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        } finally {
            process?.destroyForcibly()
            executor.shutdownNow()
        }
    }

    private const val COMMAND_TIMEOUT_SECONDS = 30L
    private const val OUTPUT_TIMEOUT_SECONDS = 1L
}
