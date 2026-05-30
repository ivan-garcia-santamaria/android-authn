package com.masstack.authn.network.interceptors

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenHolder: TokenHolder

    class TokenHolder(var token: String? = null)

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        tokenHolder = TokenHolder()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenHolder.token })
            .build()
    }

    @Test
    fun `should add Bearer token when token is available`() {
        server.enqueue(MockResponse().setBody("ok"))
        tokenHolder.token = "my-access-token"

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer my-access-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `should not add header when token is null`() {
        server.enqueue(MockResponse().setBody("ok"))
        tokenHolder.token = null

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `should not add header when token is blank`() {
        server.enqueue(MockResponse().setBody("ok"))
        tokenHolder.token = "   "

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `should not override existing Authorization header`() {
        server.enqueue(MockResponse().setBody("ok"))
        tokenHolder.token = "my-access-token"

        val request = Request.Builder()
            .url(server.url("/test"))
            .header("Authorization", "Basic dXNlcjpwYXNz")
            .build()
        buildClient().newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("Basic dXNlcjpwYXNz", recorded.getHeader("Authorization"))
    }

    @Test
    fun `should call token provider on each request`() {
        server.enqueue(MockResponse().setBody("ok"))
        server.enqueue(MockResponse().setBody("ok"))

        var callCount = 0
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor {
                callCount++
                "token-$callCount"
            })
            .build()

        client.newCall(Request.Builder().url(server.url("/first")).build()).execute()
        client.newCall(Request.Builder().url(server.url("/second")).build()).execute()

        assertEquals(2, callCount)
        assertEquals("Bearer token-1", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-2", server.takeRequest().getHeader("Authorization"))
    }
}
