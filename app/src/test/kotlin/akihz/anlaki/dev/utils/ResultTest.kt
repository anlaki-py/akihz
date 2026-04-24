package akihz.anlaki.dev.utils

import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `success creates Success instance`() {
        val result = Result.success(42)
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `error creates Error instance`() {
        val result = Result.error(ErrorType.PERMISSION_DENIED)
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getErrorOrNull returns error for Error`() {
        val error = Result.error(ErrorType.TIMEOUT, "timed out")
        val err = error.getErrorOrNull()
        assertNotNull(err)
        assertEquals(ErrorType.TIMEOUT, err?.errorType)
        assertEquals("timed out", err?.message)
    }

    @Test
    fun `getErrorOrNull returns null for Success`() {
        val success = Result.success("ok")
        assertNull(success.getErrorOrNull())
    }

    @Test
    fun `map transforms Success value`() {
        val result = Result.success(5).map { it * 2 }
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull())
    }

    @Test
    fun `map preserves Error`() {
        val result = Result.error<Int>(ErrorType.CONNECTION_LOST).map { it * 2 }
        assertTrue(result.isError)
        assertEquals(ErrorType.CONNECTION_LOST, result.getErrorOrNull()?.errorType)
    }

    @Test
    fun `onSuccess executes action for Success`() {
        var called = false
        Result.success(1).onSuccess { called = true }
        assertTrue(called)
    }

    @Test
    fun `onSuccess does not execute for Error`() {
        var called = false
        Result.error<Int>(ErrorType.SERVICE_BINDING_FAILED).onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onError executes action for Error`() {
        var called = false
        Result.error<Int>(ErrorType.SHIZUKU_NOT_RUNNING).onError { _, _ -> called = true }
        assertTrue(called)
    }

    @Test
    fun `onError does not execute for Success`() {
        var called = false
        Result.success(1).onError { _, _ -> called = true }
        assertFalse(called)
    }
}
