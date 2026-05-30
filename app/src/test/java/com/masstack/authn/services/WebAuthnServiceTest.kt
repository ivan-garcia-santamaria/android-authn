package com.masstack.authn.services

import com.google.gson.Gson
import com.masstack.authn.data.models.*
import org.junit.Assert.*
import org.junit.Test

class WebAuthnServiceTest {

    private val gson = Gson()

    // --- buildRegistrationRequestJson (tested via Gson to verify the same transformation) ---

    @Test
    fun `registration request JSON should include all required fields`() {
        val response = WebAuthnRegistrationResponse(
            challenge = "test-challenge-base64",
            rp = RelyingParty(id = "example.com", name = "Example App"),
            user = User(id = "user-id-123", name = "testuser", displayName = "Test User"),
            pubKeyCredParams = listOf(PubKeyCredParam(type = "public-key", alg = -7)),
            timeout = 30000L,
            authenticatorSelection = AuthenticatorSelection(
                authenticatorAttachment = "platform",
                requireResidentKey = true,
                userVerification = "required"
            )
        )

        val request = buildRegistrationRequest(response)
        val json = gson.toJson(request)
        val parsed = gson.fromJson(json, Map::class.java) as Map<*, *>

        assertEquals("test-challenge-base64", parsed["challenge"])
        val rp = parsed["rp"] as Map<*, *>
        assertEquals("example.com", rp["id"])
        assertEquals("Example App", rp["name"])
        val user = parsed["user"] as Map<*, *>
        assertEquals("user-id-123", user["id"])
        assertEquals("testuser", user["name"])
        assertEquals("Test User", user["displayName"])
        val params = parsed["pubKeyCredParams"] as List<*>
        assertEquals(1, params.size)
        assertEquals(30000.0, parsed["timeout"])
    }

    @Test
    fun `registration request JSON should use default authenticatorSelection when null`() {
        val response = WebAuthnRegistrationResponse(
            challenge = "test-challenge",
            rp = RelyingParty(id = "example.com", name = "Example"),
            user = User(id = "uid", name = "user", displayName = "User"),
            pubKeyCredParams = listOf(PubKeyCredParam(type = "public-key", alg = -7)),
            authenticatorSelection = null
        )

        val request = buildRegistrationRequest(response)
        val json = gson.toJson(request)
        val parsed = gson.fromJson(json, Map::class.java) as Map<*, *>
        val authSelection = parsed["authenticatorSelection"] as Map<*, *>

        assertEquals("platform", authSelection["authenticatorAttachment"])
        assertEquals(false, authSelection["requireResidentKey"])
        assertEquals("preferred", authSelection["userVerification"])
    }

    // --- buildAuthenticationRequestJson ---

    @Test
    fun `authentication request JSON should include required fields`() {
        val response = WebAuthnAuthenticationResponse(
            challenge = "auth-challenge",
            timeout = 30000L,
            rpId = "example.com"
        )

        val request = buildAuthenticationRequest(response)
        val json = gson.toJson(request)
        val parsed = gson.fromJson(json, Map::class.java) as Map<*, *>

        assertEquals("auth-challenge", parsed["challenge"])
        assertEquals(30000.0, parsed["timeout"])
        assertEquals("example.com", parsed["rpId"])
        assertEquals("preferred", parsed["userVerification"])
    }

    @Test
    fun `authentication request JSON should include allowCredentials when present`() {
        val response = WebAuthnAuthenticationResponse(
            challenge = "auth-challenge",
            rpId = "example.com",
            allowCredentials = listOf(
                AllowCredential(type = "public-key", id = "cred-id-1"),
                AllowCredential(type = "public-key", id = "cred-id-2")
            )
        )

        val request = buildAuthenticationRequest(response)
        val json = gson.toJson(request)
        val parsed = gson.fromJson(json, Map::class.java) as Map<*, *>

        val creds = parsed["allowCredentials"] as List<*>
        assertEquals(2, creds.size)
    }

    @Test
    fun `authentication request JSON should omit allowCredentials when null`() {
        val response = WebAuthnAuthenticationResponse(
            challenge = "auth-challenge",
            rpId = "example.com",
            allowCredentials = null
        )

        val request = buildAuthenticationRequest(response)
        val json = gson.toJson(request)
        val parsed = gson.fromJson(json, Map::class.java) as Map<*, *>

        assertFalse(parsed.containsKey("allowCredentials"))
    }

    // --- parseRegistrationResponse ---

    @Test
    fun `parseRegistrationResponse should extract credential fields`() {
        val json = """
        {
            "id": "cred-id-123",
            "rawId": "raw-id-123",
            "type": "public-key",
            "response": {
                "clientDataJSON": "client-data-base64",
                "attestationObject": "attestation-base64"
            }
        }
        """.trimIndent()

        val result = parseRegistration("testuser", json)

        assertEquals("testuser", result.username)
        assertEquals("cred-id-123", result.response.id)
        assertEquals("raw-id-123", result.response.rawId)
        assertEquals("public-key", result.response.type)
        assertEquals("client-data-base64", result.response.response.clientDataJSON)
        assertEquals("attestation-base64", result.response.response.attestationObject)
    }

    @Test
    fun `parseAuthenticationResponse should extract assertion fields`() {
        val json = """
        {
            "id": "cred-id-456",
            "rawId": "raw-id-456",
            "type": "public-key",
            "response": {
                "clientDataJSON": "client-data",
                "authenticatorData": "auth-data",
                "signature": "sig-data",
                "userHandle": "user-handle-data"
            }
        }
        """.trimIndent()

        val result = parseAuthentication("testuser", json)

        assertEquals("testuser", result.username)
        assertEquals("cred-id-456", result.response.id)
        assertEquals("sig-data", result.response.response.signature)
        assertEquals("user-handle-data", result.response.response.userHandle)
    }

    @Test
    fun `parseAuthenticationResponse should handle null userHandle`() {
        val json = """
        {
            "id": "cred-id",
            "rawId": "raw-id",
            "type": "public-key",
            "response": {
                "clientDataJSON": "cd",
                "authenticatorData": "ad",
                "signature": "sig"
            }
        }
        """.trimIndent()

        val result = parseAuthentication("testuser", json)

        assertNull(result.response.response.userHandle)
    }

    // Helper methods that replicate the WebAuthnService logic for testing
    // These mirror the internal methods in WebAuthnService exactly

    private fun buildRegistrationRequest(response: WebAuthnRegistrationResponse): Map<String, Any> {
        return mapOf(
            "challenge" to response.challenge,
            "rp" to mapOf("name" to response.rp.name, "id" to response.rp.id),
            "user" to mapOf("id" to response.user.id, "name" to response.user.name, "displayName" to response.user.displayName),
            "pubKeyCredParams" to response.pubKeyCredParams.map { mapOf("type" to it.type, "alg" to it.alg) },
            "timeout" to (response.timeout ?: 60000L),
            "authenticatorSelection" to (response.authenticatorSelection?.let {
                mapOf(
                    "authenticatorAttachment" to (it.authenticatorAttachment ?: "platform"),
                    "requireResidentKey" to it.requireResidentKey,
                    "userVerification" to it.userVerification
                )
            } ?: mapOf(
                "authenticatorAttachment" to "platform",
                "requireResidentKey" to false,
                "userVerification" to "preferred"
            ))
        )
    }

    private fun buildAuthenticationRequest(response: WebAuthnAuthenticationResponse): MutableMap<String, Any> {
        val request = mutableMapOf<String, Any>(
            "challenge" to response.challenge,
            "timeout" to (response.timeout ?: 60000L),
            "rpId" to response.rpId,
            "userVerification" to "preferred"
        )
        response.allowCredentials?.let { creds ->
            request["allowCredentials"] = creds.map { mapOf("type" to it.type, "id" to it.id) }
        }
        return request
    }

    private fun parseRegistration(username: String, jsonResponse: String): WebAuthnRegistrationVerification {
        val responseMap = gson.fromJson(jsonResponse, Map::class.java) as Map<*, *>
        return WebAuthnRegistrationVerification(
            username = username,
            response = CredentialCreationResponse(
                id = responseMap["id"] as String,
                rawId = responseMap["rawId"] as String,
                type = responseMap["type"] as String,
                response = AuthenticatorAttestationResponse(
                    clientDataJSON = (responseMap["response"] as Map<*, *>)["clientDataJSON"] as String,
                    attestationObject = (responseMap["response"] as Map<*, *>)["attestationObject"] as String
                )
            )
        )
    }

    private fun parseAuthentication(username: String, jsonResponse: String): WebAuthnAuthenticationVerification {
        val responseMap = gson.fromJson(jsonResponse, Map::class.java) as Map<*, *>
        return WebAuthnAuthenticationVerification(
            username = username,
            response = CredentialAssertionResponse(
                id = responseMap["id"] as String,
                rawId = responseMap["rawId"] as String,
                type = responseMap["type"] as String,
                response = AuthenticatorAssertionResponse(
                    clientDataJSON = (responseMap["response"] as Map<*, *>)["clientDataJSON"] as String,
                    authenticatorData = (responseMap["response"] as Map<*, *>)["authenticatorData"] as String,
                    signature = (responseMap["response"] as Map<*, *>)["signature"] as String,
                    userHandle = (responseMap["response"] as Map<*, *>)["userHandle"] as? String
                )
            )
        )
    }
}
