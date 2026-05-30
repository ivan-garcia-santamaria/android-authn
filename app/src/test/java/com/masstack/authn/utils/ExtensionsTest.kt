package com.masstack.authn.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Extension functions
 */
class ExtensionsTest {

    @Test
    fun `urlEncode should encode special characters`() {
        // URLEncoder uses + for spaces, which is valid per application/x-www-form-urlencoded
        assertEquals("hello+world", "hello world".urlEncode())
        assertEquals("test%3Dvalue", "test=value".urlEncode())
        assertEquals("foo%26bar", "foo&bar".urlEncode())
        assertEquals("a%2Bb", "a+b".urlEncode())
    }

    @Test
    fun `toQueryString should create valid query parameters`() {
        val params = mapOf(
            "client_id" to "test123",
            "redirect_uri" to "https://example.com/callback",
            "scope" to "openid profile"
        )

        val queryString = params.toQueryString()

        assertTrue(queryString.contains("client_id=test123"))
        assertTrue(queryString.contains("redirect_uri=https%3A%2F%2Fexample.com%2Fcallback"))
        assertTrue(queryString.contains("scope=openid+profile"))
    }

    @Test
    fun `toQueryString should handle empty map`() {
        val params = emptyMap<String, String>()
        val queryString = params.toQueryString()
        assertEquals("", queryString)
    }

    @Test
    fun `parseQueryParams should parse URL parameters`() {
        val queryString = "code=abc123&state=xyz789&foo=bar"
        val params = queryString.parseQueryParams()

        assertEquals(3, params.size)
        assertEquals("abc123", params["code"])
        assertEquals("xyz789", params["state"])
        assertEquals("bar", params["foo"])
    }

    @Test
    fun `parseQueryParams should handle empty string`() {
        val params = "".parseQueryParams()
        assertTrue(params.isEmpty())
    }

    @Test
    fun `parseQueryParams should ignore malformed parameters`() {
        val queryString = "valid=123&invalid&another=456"
        val params = queryString.parseQueryParams()

        assertEquals(2, params.size)
        assertEquals("123", params["valid"])
        assertEquals("456", params["another"])
        assertFalse(params.containsKey("invalid"))
    }
}
