package akihz.anlaki.dev.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTypeTest {

    @Test
    fun `all error types have non-empty user messages`() {
        ErrorType.entries.forEach { type ->
            val message = type.getUserMessage()
            assertEquals(
                "Error type $type should not have empty message",
                false,
                message.isBlank()
            )
        }
    }

    @Test
    fun `SHIZUKU_NOT_RUNNING message is correct`() {
        assertEquals(
            "Shizuku is not running",
            ErrorType.SHIZUKU_NOT_RUNNING.getUserMessage()
        )
    }

    @Test
    fun `PERMISSION_DENIED message is correct`() {
        assertEquals(
            "Shizuku permission denied",
            ErrorType.PERMISSION_DENIED.getUserMessage()
        )
    }

    @Test
    fun `DISPLAY_NOT_FOUND message is correct`() {
        assertEquals(
            "Display not found",
            ErrorType.DISPLAY_NOT_FOUND.getUserMessage()
        )
    }

    @Test
    fun `NO_REFRESH_RATES message is correct`() {
        assertEquals(
            "No supported refresh rates detected",
            ErrorType.NO_REFRESH_RATES.getUserMessage()
        )
    }

    @Test
    fun `UNSUPPORTED_API message is correct`() {
        assertEquals(
            "Unsupported Android version",
            ErrorType.UNSUPPORTED_API.getUserMessage()
        )
    }
}
