package akihz.anlaki.dev.data

import akihz.anlaki.dev.ICommandService
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku user service implementation that executes shell commands
 * in an elevated-privilege process.
 */
class ICommandServiceImpl : ICommandService.Stub() {

    override fun runCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", command))

            process.inputStream.use { inputStream ->
                process.errorStream.use { errorStream ->
                    val output = BufferedReader(InputStreamReader(inputStream)).use { it.readText().trim() }
                    val error = BufferedReader(InputStreamReader(errorStream)).use { it.readText().trim() }

                    process.waitFor()

                    when {
                        error.isNotEmpty() -> "ERROR: $error"
                        output.isNotEmpty() -> output
                        else -> "OK"
                    }
                }
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}