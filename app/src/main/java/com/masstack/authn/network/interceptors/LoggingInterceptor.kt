package com.masstack.authn.network.interceptors

import com.masstack.authn.utils.Logger
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException

/**
 * Interceptor to log HTTP requests and responses to our custom Logger
 * This allows viewing network activity in the app's Debug Logs screen
 *
 * @param maxBodyLength Maximum length of request/response body to log.
 *                      Use -1 for unlimited (no truncation).
 *                      Default is 500 characters.
 */
class LoggingInterceptor(
    private val maxBodyLength: Int = 500
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Log request details BEFORE making the call
        Logger.i("HTTP", "━━━━━━━━ REQUEST ━━━━━━━━")
        Logger.i("HTTP", "${request.method} ${request.url}")
        Logger.d("HTTP", "Headers: ${request.headers}")

        // Log request body if present
        request.body?.let { body ->
            Logger.d("HTTP", "Content-Type: ${body.contentType()}")
            Logger.d("HTTP", "Content-Length: ${body.contentLength()} bytes")

            // Try to read and log body content
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                if (bodyString.isNotEmpty()) {
                    // Truncate if configured (maxBodyLength > 0) and body is too long
                    val logged = when {
                        maxBodyLength < 0 -> bodyString // No truncation
                        bodyString.length > maxBodyLength -> bodyString.take(maxBodyLength) + "... (truncated, showing $maxBodyLength of ${bodyString.length} chars)"
                        else -> bodyString
                    }
                    Logger.d("HTTP", "Request Body: $logged")
                } else {
                    Logger.w("HTTP", "Request Body is EMPTY!")
                }
            } catch (e: Exception) {
                Logger.e("HTTP", "Failed to read request body: ${e.message}")
            }
        } ?: run {
            Logger.w("HTTP", "No request body")
        }

        val startTime = System.currentTimeMillis()

        // Execute request
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Logger.e("HTTP", "Request failed: ${e.message}")
            throw e
        }

        val duration = System.currentTimeMillis() - startTime

        // Log response details
        Logger.i("HTTP", "━━━━━━━━ RESPONSE ━━━━━━━━")
        Logger.i("HTTP", "${response.code} ${response.message} (${duration}ms)")
        Logger.d("HTTP", "URL: ${response.request.url}")

        // Log response headers
        if (response.headers.size > 0) {
            Logger.d("HTTP", "Response Headers: ${response.headers}")
        }

        return response
    }
}
