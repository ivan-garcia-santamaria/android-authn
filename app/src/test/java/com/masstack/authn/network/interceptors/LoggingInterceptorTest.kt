package com.masstack.authn.network.interceptors

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LoggingInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        server.shutdown()
        unmockkStatic(Log::class)
    }

    private fun buildClient(maxBodyLength: Int = -1): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor(maxBodyLength = maxBodyLength))
            .build()
    }

    @Test
    fun `should proceed with request and return response`() {
        server.enqueue(MockResponse().setBody("response body").setResponseCode(200))

        val response = buildClient()
            .newCall(Request.Builder().url(server.url("/test")).build())
            .execute()

        assertEquals(200, response.code)
        assertEquals("response body", response.body?.string())
    }

    @Test
    fun `should not modify request headers or body`() {
        server.enqueue(MockResponse().setBody("ok"))
        val body = """{"key":"value"}"""

        buildClient().newCall(
            Request.Builder()
                .url(server.url("/test"))
                .header("X-Custom", "custom-value")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val recorded = server.takeRequest()
        assertEquals("custom-value", recorded.getHeader("X-Custom"))
        assertEquals(body, recorded.body.readUtf8())
    }

    @Test
    fun `should handle request with no body`() {
        server.enqueue(MockResponse().setBody("ok"))

        val response = buildClient()
            .newCall(Request.Builder().url(server.url("/test")).get().build())
            .execute()

        assertEquals(200, response.code)
    }

    @Test
    fun `should not crash on error response codes`() {
        server.enqueue(MockResponse().setBody("not found").setResponseCode(404))

        val response = buildClient()
            .newCall(Request.Builder().url(server.url("/test")).build())
            .execute()

        assertEquals(404, response.code)
    }
}
