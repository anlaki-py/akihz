package akihz.anlaki.dev.data

import akihz.anlaki.dev.ICommandService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Shizuku user service implementation that executes shell commands
 * in an elevated-privilege process.
 */
class ICommandServiceImpl : ICommandService.Stub() {

    override fun runCommand(command: String): String {
        return execute(listOf("/system/bin/sh", "-c", command))
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

    private fun execute(command: List<String>): String {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val outputFuture = executor.submit<String> {
                BufferedReader(InputStreamReader(process.inputStream)).use {
                    it.readText().trim()
                }
            }

            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
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
        } finally {
            executor.shutdownNow()
        }
    }

    override fun destroy() {
        System.exit(0)
    }

    private companion object {
        const val COMMAND_TIMEOUT_SECONDS = 10L
        const val OUTPUT_TIMEOUT_SECONDS = 1L
    }
}
