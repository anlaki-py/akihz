package akihz.anlaki.dev.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootCommandsTest {
    @Test
    fun settingsValuesRemainLiteralShellArguments() {
        val values = listOf("", "hello world", "a'b", "\$HOME; echo injected", "line\nnext")
        for (value in values) {
            val output = CommandProcess.execute(listOf("sh", "-c", "printf '%s' " + quoteShellArgument(value)))
            assertEquals(value.ifEmpty { "OK" }, output)
        }
    }

    @Test
    fun nonzeroExitReportsFailure() {
        assertTrue(CommandProcess.execute(listOf("sh", "-c", "echo denied; exit 1")).startsWith("ERROR: denied"))
    }

    @Test
    fun missingExecutableReportsFailure() {
        assertTrue(CommandProcess.execute(listOf("/missing/akihz-command")).startsWith("ERROR:"))
    }
}
