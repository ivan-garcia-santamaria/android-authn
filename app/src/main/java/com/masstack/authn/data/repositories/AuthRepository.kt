package com.masstack.authn.data.repositories

import android.util.Base64
import android.util.Log
import com.masstack.authn.data.models.*
import com.masstack.authn.network.api.AuthApi
import com.masstack.authn.utils.Constants
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    private var cachedConfig: OpenIdConfiguration? = null

    fun getCachedConfig(): OpenIdConfiguration? = cachedConfig

    suspend fun fetchOpenIdConfiguration(url: String): Result<OpenIdConfiguration> {
        return try {
            Log.d(TAG, "Fetching OpenID Configuration from: $url")
            val response = authApi.fetchOpenIdConfiguration(url)

            Log.d(TAG, "OpenID Configuration response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val config = response.body()!!
                Log.i(TAG, "OpenID Configuration fetched successfully")
                Log.d(TAG, "Issuer: ${config.issuer}")
                Log.d(TAG, "Authorization endpoint: ${config.authorizationEndpoint}")
                Log.d(TAG, "Token endpoint: ${config.tokenEndpoint}")
                Result.success(config)
            } else {
                val error = parseError(response)
                Log.e(TAG, "Failed to fetch OpenID Configuration: $error")
                Result.failure(Exception(error))
            }
        } catch (e: ServerError) {
            Log.e(TAG, "Server error fetching OpenID Configuration: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching OpenID Configuration: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkServerAvailability(): Result<Unit> {
        val url = settingsRepository.getSettings().openidConfigurationUrl
        return try {
            Log.d(TAG, "Checking server availability via OpenID Configuration at: $url")
            val response = authApi.fetchOpenIdConfiguration(url)

            Log.d(TAG, "Server availability check - Response code: ${response.code()}")

            if (response.code() == 200 && response.isSuccessful && response.body() != null) {
                cachedConfig = response.body()!!
                Log.d(TAG, "Server is available - endpoints cached from discovery")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Server returned code: ${response.code()} - Body: $errorBody")
                throw ServerError(
                    code = response.code(),
                    message = response.message() ?: "Service Unavailable",
                    body = errorBody
                )
            }
        } catch (e: ServerError) {
            Log.e(TAG, "Server unavailable: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check server availability: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String
    ): Result<TokenResponse> {
        return try {
            Log.d(TAG, "Exchanging authorization code for tokens")
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!

            Log.d(TAG, "Token endpoint: ${config.tokenEndpoint}")
            Log.d(TAG, "Client ID: ${settings.authCodeClientId}")
            Log.d(TAG, "Redirect URI: ${settings.authCodeRedirectUri}")

            val response = authApi.exchangeCodeForToken(
                url = config.tokenEndpoint!!,
                grantType = Constants.FLOW_AUTHORIZATION_CODE,
                code = code,
                clientId = settings.authCodeClientId,
                clientSecret = if (settings.authCodeClientSecret.isNotBlank()) settings.authCodeClientSecret else null,
                redirectUri = settings.authCodeRedirectUri,
                codeVerifier = codeVerifier
            )

            Log.d(TAG, "Token exchange response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                Log.i(TAG, "Token exchange successful")
                settingsRepository.saveAccessToken(tokenResponse.accessToken, tokenResponse.expiresIn)
                settingsRepository.saveRefreshToken(tokenResponse.refreshToken)
                Result.success(tokenResponse)
            } else {
                val error = parseError(response)
                Log.e(TAG, "Token exchange failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token exchange: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<TokenResponse> {
        return try {
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!
            val authHeader = createBasicAuthHeader(settings.authCodeClientId, settings.authCodeClientSecret)

            val response = authApi.refreshToken(
                url = config.tokenEndpoint!!,
                refreshToken = refreshToken,
                authorization = authHeader
            )

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                settingsRepository.saveAccessToken(tokenResponse.accessToken, tokenResponse.expiresIn)
                settingsRepository.saveRefreshToken(tokenResponse.refreshToken)
                Result.success(tokenResponse)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cibaAuthorize(
        loginHint: String,
        bindingMessage: String? = null
    ): Result<CibaResponse> {
        return try {
            Log.d(TAG, "Initiating CIBA backchannel authorization")
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!
            val authHeader = createBasicAuthHeader(settings.cibaClientId, settings.cibaClientSecret)

            Log.d(TAG, "CIBA endpoint: ${config.backchannelAuthenticationEndpoint}")
            Log.d(TAG, "CIBA Client ID: ${settings.cibaClientId}")
            Log.d(TAG, "Login hint: $loginHint")

            val response = authApi.cibaAuthorize(
                url = config.backchannelAuthenticationEndpoint!!,
                authorization = authHeader,
                loginHint = loginHint,
                scope = settings.scope,
                bindingMessage = bindingMessage
            )

            Log.d(TAG, "CIBA authorize response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val cibaResponse = response.body()!!
                Log.i(TAG, "CIBA authorization initiated successfully")
                Log.d(TAG, "Auth request ID: ${cibaResponse.authReqId}")
                Result.success(cibaResponse)
            } else {
                val error = parseError(response)
                Log.e(TAG, "CIBA authorization failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during CIBA authorization: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cibaToken(authReqId: String): Result<TokenResponse> {
        return try {
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!
            val authHeader = createBasicAuthHeader(settings.cibaClientId, settings.cibaClientSecret)

            val response = authApi.cibaToken(
                url = config.tokenEndpoint!!,
                authorization = authHeader,
                grantType = Constants.FLOW_CIBA,
                authReqId = authReqId
            )

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                settingsRepository.saveAccessToken(tokenResponse.accessToken, tokenResponse.expiresIn)
                settingsRepository.saveRefreshToken(tokenResponse.refreshToken)
                Result.success(tokenResponse)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun webAuthnRegistrationBegin(username: String): Result<WebAuthnRegistrationResponse> {
        return try {
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!
            val authHeader = createBasicAuthHeader(settings.webauthnClientId, settings.webauthnClientSecret)

            Log.d(TAG, "=== WebAuthn Registration Begin ===")
            Log.d(TAG, "URL: ${config.webauthnRegistrationOptionsEndpoint}")
            Log.d(TAG, "Username: $username")

            val response = authApi.webAuthnRegistrationBegin(
                url = config.webauthnRegistrationOptionsEndpoint!!,
                authorization = authHeader,
                request = WebAuthnRegistrationRequest(username)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.i(TAG, "WebAuthn registration begin successful")
                Result.success(body)
            } else {
                val error = parseError(response)
                Log.e(TAG, "WebAuthn registration begin failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WebAuthn registration begin: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun webAuthnRegistrationComplete(
        verification: WebAuthnRegistrationVerification
    ): Result<WebAuthnVerificationResponse> {
        return try {
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!
            val authHeader = createBasicAuthHeader(settings.webauthnClientId, settings.webauthnClientSecret)

            Log.d(TAG, "=== WebAuthn Registration Complete ===")
            Log.d(TAG, "URL: ${config.webauthnRegistrationVerificationEndpoint}")

            val response = authApi.webAuthnRegistrationComplete(
                url = config.webauthnRegistrationVerificationEndpoint!!,
                authorization = authHeader,
                verification = verification
            )

            if (response.isSuccessful && response.body() != null) {
                val verificationResponse = response.body()!!
                Log.i(TAG, "WebAuthn registration complete successful")
                Result.success(verificationResponse)
            } else {
                val error = parseError(response)
                Log.e(TAG, "WebAuthn registration complete failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WebAuthn registration complete: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun webAuthnAuthenticationBegin(username: String): Result<WebAuthnAuthenticationResponse> {
        return try {
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!
            val authHeader = createBasicAuthHeader(settings.webauthnClientId, settings.webauthnClientSecret)

            Log.d(TAG, "=== WebAuthn Authentication Begin ===")
            Log.d(TAG, "URL: ${config.webauthnAuthenticationOptionsEndpoint}")
            Log.d(TAG, "Username: $username")

            val response = authApi.webAuthnAuthenticationBegin(
                url = config.webauthnAuthenticationOptionsEndpoint!!,
                authorization = authHeader,
                request = WebAuthnAuthenticationRequest(username)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.i(TAG, "WebAuthn authentication begin successful")
                Result.success(body)
            } else {
                val error = parseError(response)
                Log.e(TAG, "WebAuthn authentication begin failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WebAuthn authentication begin: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun webAuthnAuthenticationComplete(
        verification: WebAuthnAuthenticationVerification
    ): Result<TokenResponse> {
        return try {
            val settings = settingsRepository.getSettings()
            val config = cachedConfig!!
            val authHeader = createBasicAuthHeader(settings.webauthnClientId, settings.webauthnClientSecret)

            Log.d(TAG, "=== WebAuthn Authentication Complete ===")
            Log.d(TAG, "URL: ${config.webauthnAuthenticationVerificationEndpoint}")

            val response = authApi.webAuthnAuthenticationComplete(
                url = config.webauthnAuthenticationVerificationEndpoint!!,
                authorization = authHeader,
                verification = verification
            )

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                Log.i(TAG, "WebAuthn authentication complete successful")
                settingsRepository.saveAccessToken(tokenResponse.accessToken, tokenResponse.expiresIn)
                settingsRepository.saveRefreshToken(tokenResponse.refreshToken)
                Result.success(tokenResponse)
            } else {
                val error = parseError(response)
                Log.e(TAG, "WebAuthn authentication complete failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WebAuthn authentication complete: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun createBasicAuthHeader(clientId: String, clientSecret: String): String {
        val credentials = "$clientId:$clientSecret"
        val encodedCredentials = Base64.encodeToString(
            credentials.toByteArray(),
            Base64.NO_WRAP
        )
        return "Basic $encodedCredentials"
    }

    private fun <T> parseError(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()

            Log.d(TAG, "parseError - Response code: ${response.code()}")
            Log.d(TAG, "parseError - Error body: $errorBody")

            if (response.code() in 500..599 || errorBody?.contains("no healthy upstream", ignoreCase = true) == true) {
                Log.w(TAG, "parseError - Server error detected (${response.code()}) - throwing ServerError")
                throw ServerError(
                    code = response.code(),
                    message = response.message() ?: "Server Error",
                    body = errorBody
                )
            }

            errorBody ?: "Unknown error (${response.code()})"
        } catch (e: ServerError) {
            Log.d(TAG, "parseError - Re-throwing ServerError")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "parseError - Exception: ${e.message}")
            "Error ${response.code()}: ${response.message()}"
        }
    }
}
