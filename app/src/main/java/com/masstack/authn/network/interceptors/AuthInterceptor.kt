package com.masstack.authn.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor to add authentication headers and logging
 */
class AuthInterceptor(
    private val getAccessToken: () -> String?
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // If the request already has an Authorization header, don't modify it
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        val token = getAccessToken()

        // If no token is available, proceed with original request
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        // Add Authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
