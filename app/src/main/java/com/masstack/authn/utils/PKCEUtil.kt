package com.masstack.authn.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utility class for PKCE (Proof Key for Code Exchange)
 * Implements RFC 7636
 */
object PKCEUtil {

    /**
     * Generates a cryptographically random code verifier
     * @return Base64 URL-encoded string of 128 characters
     */
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(96) // 96 bytes = 128 characters when base64url encoded
        secureRandom.nextBytes(bytes)
        return base64UrlEncode(bytes)
    }

    /**
     * Generates code challenge from code verifier using S256 method
     * @param codeVerifier The code verifier
     * @return Base64 URL-encoded SHA256 hash of the code verifier
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return base64UrlEncode(digest)
    }

    /**
     * Encodes bytes to Base64 URL-safe format without padding
     */
    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Data class to hold PKCE parameters
     */
    data class PKCEPair(
        val codeVerifier: String,
        val codeChallenge: String
    )

    /**
     * Generates both code verifier and code challenge
     */
    fun generatePKCEPair(): PKCEPair {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        return PKCEPair(codeVerifier, codeChallenge)
    }
}
