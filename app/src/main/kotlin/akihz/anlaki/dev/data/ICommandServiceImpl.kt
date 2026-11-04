package akihz.anlaki.dev.data


/**
 * Shizuku user service implementation that executes shell commands
 * in an elevated-privilege process.
 */
class ICommandServiceImpl : ICommandService.Stub() {

    /** Runs [command] through a shell and returns its output. */
    override fun runCommand(command: String): String {
        return CommandProcess.execute(listOf("/system/bin/sh", "-c", command))
    }

    /**
     * Executes Android's settings utility without passing user values through a shell.
     *
     * @param arguments operation, namespace, key, and optional value
     * @return command output or an error prefixed with `ERROR`
     */
    override fun runSettingsCommand(arguments: List<String>): String {
        return CommandProcess.execute(listOf("/system/bin/settings") + arguments)
    }

    /** Exits the user service process. */
    override fun destroy() {
        System.exit(0)
    }

    /** Returns this process id. */
    override fun getPid(): Int = android.os.Process.myPid()

}
