package com.masstack.authn.data.models

/**
 * Represents a server error with HTTP status code
 */
data class ServerError(
    val code: Int,
    override val message: String,
    val body: String? = null
) : Exception("HTTP $code: $message") {

    /**
     * Check if this is a 503 Service Unavailable error
     */
    fun is503ServiceUnavailable(): Boolean {
        return code == 503 || body?.contains("no healthy upstream", ignoreCase = true) == true
    }

    /**
     * Check if this is any 5xx server error
     */
    fun isServerError(): Boolean {
        return code in 500..599
    }
}
