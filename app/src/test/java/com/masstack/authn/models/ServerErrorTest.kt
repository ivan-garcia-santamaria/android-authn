package com.masstack.authn.models

import com.masstack.authn.data.models.ServerError
import org.junit.Assert.*
import org.junit.Test

class ServerErrorTest {

    @Test
    fun `should store code and message and extend Exception`() {
        val error = ServerError(code = 503, message = "Service Unavailable", body = "no healthy upstream")

        assertEquals(503, error.code)
        assertEquals("Service Unavailable", error.message)
        assertEquals("no healthy upstream", error.body)
        assertTrue(error is Exception)
    }

    @Test
    fun `is503ServiceUnavailable should return true for code 503`() {
        val error = ServerError(code = 503, message = "Service Unavailable")

        assertTrue(error.is503ServiceUnavailable())
    }

    @Test
    fun `is503ServiceUnavailable should return true when body contains no healthy upstream`() {
        val error = ServerError(code = 200, message = "OK", body = "no healthy upstream")

        assertTrue(error.is503ServiceUnavailable())
    }

    @Test
    fun `is503ServiceUnavailable should return false for other codes`() {
        val error = ServerError(code = 404, message = "Not Found")

        assertFalse(error.is503ServiceUnavailable())
    }

    @Test
    fun `isServerError should return true for 5xx codes`() {
        assertTrue(ServerError(code = 500, message = "Internal Server Error").isServerError())
        assertTrue(ServerError(code = 502, message = "Bad Gateway").isServerError())
        assertTrue(ServerError(code = 503, message = "Service Unavailable").isServerError())
        assertTrue(ServerError(code = 599, message = "Unknown").isServerError())
    }

    @Test
    fun `isServerError should return false for 4xx codes`() {
        assertFalse(ServerError(code = 400, message = "Bad Request").isServerError())
        assertFalse(ServerError(code = 401, message = "Unauthorized").isServerError())
        assertFalse(ServerError(code = 404, message = "Not Found").isServerError())
    }
}
