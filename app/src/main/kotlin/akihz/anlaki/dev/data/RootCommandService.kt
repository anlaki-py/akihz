package akihz.anlaki.dev.data

import android.content.Context

/** Root transport selected explicitly during setup. */
internal object RootCommandService : ICommandService.Stub() {
    private lateinit var preferences: android.content.SharedPreferences

    /** Restores the user's backend choice without running a root command. */
    fun init(context: Context) {
        preferences = context.getSharedPreferences("privileged_access", Context.MODE_PRIVATE)
    }

    /** Whether the user previously granted root during setup. */
    val enabled: Boolean get() = preferences.getBoolean("root_enabled", false)

    /** Requests root on a worker thread and returns whether uid 0 was granted. */
    fun request(): Boolean {
        val granted = CommandProcess.execute(listOf("su", "-c", "id -u")) == "0"
        if (granted) preferences.edit().putBoolean("root_enabled", true).apply()
        return granted
    }

    /** Selects Shizuku after the caller checks its permission. */
    fun useShizuku() {
        preferences.edit().putBoolean("root_enabled", false).apply()
    }

    /** Runs [command] with root and reports failures without retrying writes. */
    override fun runCommand(command: String): String =
        CommandProcess.execute(listOf("su", "-c", command))

    /** Quotes each settings argument so values cannot become shell commands. */
    override fun runSettingsCommand(arguments: List<String>): String =
        runCommand((listOf("/system/bin/settings") + arguments).joinToString(" ", transform = ::quoteShellArgument))

    /** No persistent root process is owned by this adapter. */
    override fun destroy() = Unit

    /** No persistent root process exists. */
    override fun getPid(): Int = -1
}

