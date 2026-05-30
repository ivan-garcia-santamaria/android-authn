package com.masstack.authn.services

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.masstack.authn.data.models.*
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.utils.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for WebAuthn (Web Authentication API) flow
 * Uses Android Credential Manager API
 */
@Singleton
class WebAuthnService @Inject constructor(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) {

    private val credentialManager = CredentialManager.create(context)
    private val gson = Gson()

    /**
     * Check if WebAuthn is available on this device
     */
    fun isWebAuthnAvailable(): Boolean {
        // Credential Manager API requires Android 9+ (API 28)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    /**
     * Get detailed error message for WebAuthn availability
     */
    fun getWebAuthnUnavailabilityReason(): String? {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P ->
                "WebAuthn requires Android 9 (API 28) or higher. Your device is running Android ${Build.VERSION.SDK_INT}."
            else -> null
        }
    }

    /**
     * Register new WebAuthn credential
     * Note: Registration only verifies the credential was created, it doesn't return an access token
     */
    suspend fun registerCredential(username: String): Result<WebAuthnVerificationResponse> {
        Logger.i("WebAuthn", "=== Starting WebAuthn Registration ===")
        Logger.i("WebAuthn", "Username: $username")

        // Check availability first
        if (!isWebAuthnAvailable()) {
            val reason = getWebAuthnUnavailabilityReason() ?: "WebAuthn is not available"
            Logger.e("WebAuthn", "WebAuthn not available: $reason")
            return Result.failure(Exception(reason))
        }

        return try {
            // Step 1: Get registration challenge from server
            Logger.i("WebAuthn", "Step 1: Requesting registration challenge from server")
            val challengeResult = authRepository.webAuthnRegistrationBegin(username)
            if (challengeResult.isFailure) {
                Logger.e("WebAuthn", "Failed to get challenge", challengeResult.exceptionOrNull())
                return Result.failure(
                    challengeResult.exceptionOrNull() ?: Exception("Failed to get registration challenge")
                )
            }

            val registrationResponse = challengeResult.getOrNull()!!
            Logger.i("WebAuthn", "Registration challenge received")
            Logger.d("WebAuthn", "RP ID: ${registrationResponse.rp.id}")
            Logger.d("WebAuthn", "RP Name: ${registrationResponse.rp.name}")
            Logger.d("WebAuthn", "User ID: ${registrationResponse.user.id}")
            Logger.d("WebAuthn", "Challenge: ${registrationResponse.challenge.take(20)}...")

            // Step 2: Create credential request for Android
            Logger.i("WebAuthn", "Step 2: Building credential request")
            val requestJson = buildRegistrationRequestJson(registrationResponse)
            Logger.d("WebAuthn", "Request JSON: $requestJson")
            val createRequest = CreatePublicKeyCredentialRequest(requestJson)

            // Step 3: Create credential using Credential Manager (biometric)
            Logger.i("WebAuthn", "Step 3: Creating credential with Credential Manager")
            val result = credentialManager.createCredential(
                context = context,
                request = createRequest
            )

            if (result !is CreatePublicKeyCredentialResponse) {
                Logger.e("WebAuthn", "Unexpected credential response type: ${result::class.simpleName}")
                return Result.failure(Exception("Unexpected credential response type"))
            }

            Logger.i("WebAuthn", "Credential created successfully")

            // Step 4: Parse credential response
            Logger.i("WebAuthn", "Step 4: Parsing credential response")
            val credentialData = result.registrationResponseJson
            Logger.d("WebAuthn", "Credential data: ${credentialData.take(200)}...")
            val verification = parseRegistrationResponse(username, credentialData)

            // Step 5: Complete registration on server
            Logger.i("WebAuthn", "Step 5: Completing registration on server")
            val verificationResult = authRepository.webAuthnRegistrationComplete(verification)

            if (verificationResult.isSuccess) {
                Logger.i("WebAuthn", "=== WebAuthn Registration Successful ===")
            } else {
                Logger.e("WebAuthn", "Registration completion failed", verificationResult.exceptionOrNull())
            }

            verificationResult
        } catch (e: CreateCredentialException) {
            Logger.e("WebAuthn", "CreateCredentialException caught", e)
            Logger.e("WebAuthn", "Exception type: ${e::class.simpleName}")
            Logger.e("WebAuthn", "Exception message: ${e.message}")
            Logger.e("WebAuthn", "Exception errorCode: ${e.type}")

            val errorMsg = when {
                e.message?.contains("UNKNOWN") == true || e.message?.contains("framework") == true ->
                    "WebAuthn is not properly configured on this device. Emulator users: Please ensure you're using a Google Play emulator with screen lock enabled (Settings > Security > Screen Lock). Physical device users: Ensure biometric authentication is set up."
                e.message?.contains("canceled") == true || e.message?.contains("cancelled") == true ->
                    "Registration was canceled by the user"
                else -> "WebAuthn registration failed: ${e.message}"
            }
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            Logger.e("WebAuthn", "Unexpected exception during registration", e)
            Result.failure(Exception("WebAuthn registration failed: ${e.message}", e))
        }
    }

    /**
     * Authenticate with existing WebAuthn credential
     */
    suspend fun authenticateCredential(username: String): Result<TokenResponse> {
        Logger.i("WebAuthn", "=== Starting WebAuthn Authentication ===")
        Logger.i("WebAuthn", "Username: $username")

        // Check availability first
        if (!isWebAuthnAvailable()) {
            val reason = getWebAuthnUnavailabilityReason() ?: "WebAuthn is not available"
            Logger.e("WebAuthn", "WebAuthn not available: $reason")
            return Result.failure(Exception(reason))
        }

        return try {
            // Step 1: Get authentication challenge from server
            Logger.i("WebAuthn", "Step 1: Requesting authentication challenge from server")
            val challengeResult = authRepository.webAuthnAuthenticationBegin(username)
            if (challengeResult.isFailure) {
                Logger.e("WebAuthn", "Failed to get challenge", challengeResult.exceptionOrNull())
                return Result.failure(
                    challengeResult.exceptionOrNull() ?: Exception("Failed to get authentication challenge")
                )
            }

            val authenticationResponse = challengeResult.getOrNull()!!
            Logger.i("WebAuthn", "Authentication challenge received")
            Logger.d("WebAuthn", "RP ID: ${authenticationResponse.rpId}")
            Logger.d("WebAuthn", "Challenge: ${authenticationResponse.challenge.take(20)}...")
            Logger.d("WebAuthn", "AllowCredentials: ${authenticationResponse.allowCredentials?.size ?: 0} credentials")

            // Step 2: Create credential request for Android
            Logger.i("WebAuthn", "Step 2: Building credential request")
            val requestJson = buildAuthenticationRequestJson(authenticationResponse)
            Logger.d("WebAuthn", "Request JSON: $requestJson")
            val getRequest = GetCredentialRequest(
                listOf(GetPublicKeyCredentialOption(requestJson))
            )

            // Step 3: Get credential using Credential Manager (biometric)
            Logger.i("WebAuthn", "Step 3: Getting credential with Credential Manager")
            val result = credentialManager.getCredential(
                context = context,
                request = getRequest
            )

            Logger.i("WebAuthn", "Credential retrieved successfully")

            val credential = result.credential
            if (credential !is PublicKeyCredential) {
                Logger.e("WebAuthn", "Unexpected credential type: ${credential::class.simpleName}")
                return Result.failure(Exception("Unexpected credential type"))
            }

            // Step 4: Parse authentication response
            Logger.i("WebAuthn", "Step 4: Parsing authentication response")
            val credentialData = credential.authenticationResponseJson
            Logger.d("WebAuthn", "Credential data: ${credentialData.take(200)}...")

            // Decode and log the origin being used
            try {
                val responseMap = gson.fromJson(credentialData, Map::class.java) as Map<*, *>
                val clientDataJSON = (responseMap["response"] as? Map<*, *>)?.get("clientDataJSON") as? String
                if (clientDataJSON != null) {
                    val decodedClientData = String(Base64.decode(clientDataJSON, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
                    Logger.d("WebAuthn", "Client Data JSON: $decodedClientData")
                }
            } catch (e: Exception) {
                Logger.w("WebAuthn", "Could not decode client data: ${e.message}")
            }

            val verification = parseAuthenticationResponse(username, credentialData)

            // Step 5: Complete authentication on server
            Logger.i("WebAuthn", "Step 5: Completing authentication on server")
            val tokenResult = authRepository.webAuthnAuthenticationComplete(verification)

            if (tokenResult.isSuccess) {
                Logger.i("WebAuthn", "=== WebAuthn Authentication Successful ===")
            } else {
                Logger.e("WebAuthn", "Authentication completion failed", tokenResult.exceptionOrNull())
            }

            tokenResult
        } catch (e: GetCredentialException) {
            Logger.e("WebAuthn", "GetCredentialException caught", e)
            Logger.e("WebAuthn", "Exception type: ${e::class.simpleName}")
            Logger.e("WebAuthn", "Exception message: ${e.message}")
            Logger.e("WebAuthn", "Exception errorCode: ${e.type}")

            val errorMsg = when {
                e.message?.contains("UNKNOWN") == true || e.message?.contains("framework") == true ->
                    "WebAuthn is not properly configured on this device. Emulator users: Please ensure you're using a Google Play emulator with screen lock enabled (Settings > Security > Screen Lock). Physical device users: Ensure biometric authentication is set up."
                e.message?.contains("canceled") == true || e.message?.contains("cancelled") == true ->
                    "Authentication was canceled by the user"
                e.message?.contains("No credentials available") == true ->
                    "No WebAuthn credentials found for this user. Please register first."
                else -> "WebAuthn authentication failed: ${e.message}"
            }
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            Logger.e("WebAuthn", "Unexpected exception during authentication", e)
            Result.failure(Exception("WebAuthn authentication failed: ${e.message}", e))
        }
    }

    /**
     * Build registration request JSON for Android Credential Manager
     */
    @VisibleForTesting
    internal fun buildRegistrationRequestJson(response: WebAuthnRegistrationResponse): String {
        val request = mapOf(
            "challenge" to response.challenge,
            "rp" to mapOf(
                "name" to response.rp.name,
                "id" to response.rp.id
            ),
            "user" to mapOf(
                "id" to response.user.id,
                "name" to response.user.name,
                "displayName" to response.user.displayName
            ),
            "pubKeyCredParams" to response.pubKeyCredParams.map { param ->
                mapOf(
                    "type" to param.type,
                    "alg" to param.alg
                )
            },
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

        return gson.toJson(request)
    }

    /**
     * Build authentication request JSON for Android Credential Manager
     */
    @VisibleForTesting
    internal fun buildAuthenticationRequestJson(response: WebAuthnAuthenticationResponse): String {
        val request = mutableMapOf<String, Any>(
            "challenge" to response.challenge,
            "timeout" to (response.timeout ?: 60000L),
            "rpId" to response.rpId,
            "userVerification" to "preferred"
        )

        response.allowCredentials?.let { credentials ->
            request["allowCredentials"] = credentials.map { cred ->
                mapOf(
                    "type" to cred.type,
                    "id" to cred.id
                )
            }
        }

        return gson.toJson(request)
    }

    /**
     * Parse registration response from Credential Manager
     */
    @VisibleForTesting
    internal fun parseRegistrationResponse(username: String, jsonResponse: String): WebAuthnRegistrationVerification {
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

    /**
     * Parse authentication response from Credential Manager
     */
    @VisibleForTesting
    internal fun parseAuthenticationResponse(username: String, jsonResponse: String): WebAuthnAuthenticationVerification {
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
