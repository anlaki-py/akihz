package akihz.anlaki.dev.data

import akihz.anlaki.dev.ICommandService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Runs Android settings operations in an elevated Shizuku process.
 *
 * Commands are serialized so one reusable reader thread can drain process output safely.
 */
class ICommandServiceImpl : ICommandService.Stub() {
    private val outputExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "settings-output-reader").apply { isDaemon = true }
    }

    /**
     * Executes Android's settings utility without passing user values through a shell.
     *
     * @param arguments operation, namespace, key, and optional value
     * @return command output or an error prefixed with `ERROR`
     */
    override fun runSettingsCommand(arguments: List<String>): String {
        return execute(listOf("/system/bin/settings") + arguments)
    }

    @Synchronized
    private fun execute(command: List<String>): String {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val outputFuture = outputExecutor.submit<String> {
                BufferedReader(InputStreamReader(process.inputStream)).use {
                    it.readText().trim()
                }
            }

            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                runCatching { process.inputStream.close() }
                outputFuture.cancel(true)
                return "ERROR: Command timed out"
            }

            val output = outputFuture.get(OUTPUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            when {
                process.exitValue() != 0 -> "ERROR: ${output.ifBlank { "Exit code ${process.exitValue()}" }}"
                output.isNotEmpty() -> output
                else -> "OK"
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    override fun destroy() {
        outputExecutor.shutdownNow()
        System.exit(0)
    }

    private companion object {
        const val COMMAND_TIMEOUT_SECONDS = 10L
        const val OUTPUT_TIMEOUT_SECONDS = 1L
    }
}
