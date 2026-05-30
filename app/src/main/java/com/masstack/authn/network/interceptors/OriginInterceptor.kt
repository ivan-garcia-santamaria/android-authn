package com.masstack.authn.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor to add Origin header for WebAuthn requests
 * Android requires the Origin header to be in the format: android:apk-key-hash:<base64url-encoded-sha256-cert>
 */
class OriginInterceptor : Interceptor {

    companion object {
        // Base64URL encoded SHA256 certificate fingerprint
        // This must match the certificate used to sign the app
        // For debug builds: com.masstack.authn debug keystore
        // SHA256: 6D:99:2E:39:D1:71:78:6E:48:C2:8D:83:4C:1B:5C:81:B9:36:94:1A:DD:40:6F:5A:F5:5E:C3:5D:7F:88:37:38
        // Base64URL: bZkuOdFxeG5Iwo2DTBtcgbk2lBrdQG9a9V7DXX-INzg
        private const val ANDROID_ORIGIN = "android:apk-key-hash:bZkuOdFxeG5Iwo2DTBtcgbk2lBrdQG9a9V7DXX-INzg"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Add Origin header for all requests
        // This is required for WebAuthn to work properly on Android
        val requestWithOrigin = originalRequest.newBuilder()
            .header("Origin", ANDROID_ORIGIN)
            .build()

        return chain.proceed(requestWithOrigin)
    }
}
