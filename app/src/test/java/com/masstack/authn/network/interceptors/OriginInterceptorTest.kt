package com.masstack.authn.network.interceptors

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OriginInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(OriginInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should add Origin header to request`() {
        server.enqueue(MockResponse().setBody("ok"))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertNotNull(recorded.getHeader("Origin"))
    }

    @Test
    fun `should use correct android apk-key-hash format`() {
        server.enqueue(MockResponse().setBody("ok"))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val origin = server.takeRequest().getHeader("Origin")!!
        assertTrue(origin.startsWith("android:apk-key-hash:"))
    }

    @Test
    fun `should override existing Origin header`() {
        server.enqueue(MockResponse().setBody("ok"))

        val request = Request.Builder()
            .url(server.url("/test"))
            .header("Origin", "https://evil.com")
            .build()
        client.newCall(request).execute()

        val origin = server.takeRequest().getHeader("Origin")!!
        assertTrue(origin.startsWith("android:apk-key-hash:"))
    }
}
