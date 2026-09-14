package akihz.anlaki.dev.data

import akihz.anlaki.dev.ICommandService
import akihz.anlaki.dev.data.OemSettingsStrategy.Namespace
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import timber.log.Timber

/**
 * Runs settings commands against the bound user service.
 *
 * Owns raw exec, settings CRUD, and key validation. Callers pass the
 * bound service in, so this file holds no connection state.
 */
internal object ShizukuSettingsGateway {

    internal const val MAX_SETTING_KEY_LENGTH = 128
    internal const val MAX_SETTING_VALUE_LENGTH = 512

    internal fun namespaceToString(namespace: Namespace): String {
        return when (namespace) {
            Namespace.SECURE -> "secure"
            Namespace.SYSTEM -> "system"
            Namespace.GLOBAL -> "global"
        }
    }

    internal fun exec(service: ICommandService?, command: String): Result<String> {
        if (service == null) {
            return Result.error(ErrorType.SERVICE_BINDING_FAILED, "Service not bound")
        }

        return try {
            val result = service.runCommand(command)
            if (result.startsWith("ERROR")) {
                Timber.w("Command failed: %s", result)
                Result.error(ErrorType.COMMAND_EXECUTION_FAILED, result)
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Timber.e(e, "Command execution exception")
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, e.message ?: "Unknown error")
        }
    }

    internal fun execSettings(service: ICommandService?, arguments: List<String>): Result<String> {
        if (service == null) {
            return Result.error(ErrorType.SERVICE_BINDING_FAILED, "Service not bound")
        }
        return try {
            val output = service.runSettingsCommand(arguments)
            if (output.startsWith("ERROR")) {
                Result.error(ErrorType.COMMAND_EXECUTION_FAILED, output)
            } else {
                Result.success(output)
            }
        } catch (e: Exception) {
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, e.message ?: "Settings command failed")
        }
    }

    /** Lists all readable entries in one Android settings namespace. */
    internal fun listSettings(service: ICommandService?, namespace: Namespace): Result<Map<String, String>> =
        execSettings(service, listOf("list", namespaceToString(namespace))).map { output ->
            output.lineSequence().mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else {
                    line.substring(0, separator) to line.substring(separator + 1)
                }
            }.toMap()
        }

    /** Reads one setting, returning null when the key does not exist. */
    internal fun getSetting(service: ICommandService?, namespace: Namespace, key: String): Result<String?> {
        if (!isValidKey(key)) return invalidKey()
        return execSettings(service, listOf("get", namespaceToString(namespace), key)).map {
            it.trim().takeUnless { value -> value == "null" || value == "OK" }
        }
    }

    /** Writes one setting without evaluating user data as shell syntax. */
    internal fun putSetting(
        service: ICommandService?,
        namespace: Namespace,
        key: String,
        value: String
    ): Result<Unit> {
        if (!isValidKey(key)) return invalidKey()
        if (value.contains('\u0000') || value.length > MAX_SETTING_VALUE_LENGTH) {
            return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Invalid setting value.")
        }
        return execSettings(service, listOf("put", namespaceToString(namespace), key, value)).map { Unit }
    }

    /** Deletes one setting. */
    internal fun deleteSetting(service: ICommandService?, namespace: Namespace, key: String): Result<Unit> {
        if (!isValidKey(key)) return invalidKey()
        return execSettings(service, listOf("delete", namespaceToString(namespace), key)).map { Unit }
    }

    internal fun isValidKey(key: String): Boolean =
        key.length in 1..MAX_SETTING_KEY_LENGTH && key.all {
            it.isLetterOrDigit() || it == '_' || it == '.' || it == '-'
        }

    internal fun <T> invalidKey(): Result<T> =
        Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Invalid setting key.")
}
