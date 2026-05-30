package com.masstack.authn.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.masstack.authn.data.models.TokenResponse
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.utils.Constants
import com.masstack.authn.utils.PKCEUtil
import com.masstack.authn.utils.toQueryString
import com.masstack.authn.utils.urlEncode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for Authorization Code flow with PKCE
 */
@Singleton
class AuthorizationCodeService @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) {

    companion object {
        private const val TAG = "AuthCodeService"
    }

    private var currentPKCEPair: PKCEUtil.PKCEPair? = null
    private var currentState: String? = null

    suspend fun checkServerAvailability(): Result<Unit> {
        return authRepository.checkServerAvailability()
    }

    /**
     * Start authorization code flow
     * Opens a Custom Tab with the authorization URL
     */
    fun startAuthorizationFlow() {
        Log.d(TAG, "Starting Authorization Code flow")
        val settings = settingsRepository.getSettings()
        val config = authRepository.getCachedConfig()
            ?: throw IllegalStateException("OpenID Configuration not available - check server availability first")

        if (!settings.isAuthCodeConfigured()) {
            throw IllegalStateException("Authorization Code flow is not properly configured")
        }

        Log.d(TAG, "Authorize URL: ${config.authorizationEndpoint}")
        Log.d(TAG, "Redirect URI: ${settings.authCodeRedirectUri}")

        currentPKCEPair = PKCEUtil.generatePKCEPair()
        Log.d(TAG, "PKCE pair generated - Challenge: ${currentPKCEPair!!.codeChallenge.take(20)}...")

        currentState = generateState()
        Log.d(TAG, "State generated: $currentState")

        val authUrl = buildAuthorizationUrl(
            authorizeUrl = config.authorizationEndpoint!!,
            clientId = settings.authCodeClientId,
            redirectUri = settings.authCodeRedirectUri,
            scope = settings.scope,
            codeChallenge = currentPKCEPair!!.codeChallenge,
            state = currentState!!
        )

        Log.i(TAG, "Opening authorization URL: $authUrl")

        // Open Custom Tab with FLAG_ACTIVITY_NEW_TASK
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        // Add FLAG_ACTIVITY_NEW_TASK since we're using Application Context
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
    }

    /**
     * Handle authorization callback
     * Exchanges the authorization code for access token
     */
    suspend fun handleCallback(uri: Uri): Result<TokenResponse> {
        Log.d(TAG, "Handling OAuth callback")
        Log.d(TAG, "Callback URI: $uri")

        // Extract code and state from URI
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")

        // Check for errors
        if (error != null) {
            val errorMsg = "OAuth Error: $error - $errorDescription"
            Log.e(TAG, errorMsg)
            return Result.failure(Exception(errorMsg))
        }

        // Validate code
        if (code.isNullOrBlank()) {
            val errorMsg = "Authorization code not found in callback"
            Log.e(TAG, errorMsg)
            return Result.failure(Exception(errorMsg))
        }

        Log.d(TAG, "Authorization code received: ${code.take(10)}...")

        // Validate state (CSRF protection)
        if (state != currentState) {
            val errorMsg = "State mismatch - potential CSRF attack. Expected: $currentState, Got: $state"
            Log.e(TAG, errorMsg)
            return Result.failure(Exception(errorMsg))
        }

        Log.d(TAG, "State validated successfully")

        // Validate PKCE verifier
        val codeVerifier = currentPKCEPair?.codeVerifier
        if (codeVerifier.isNullOrBlank()) {
            val errorMsg = "Code verifier not found - PKCE flow corrupted"
            Log.e(TAG, errorMsg)
            return Result.failure(Exception(errorMsg))
        }

        Log.d(TAG, "Code verifier available: ${codeVerifier.take(10)}...")
        Log.i(TAG, "Exchanging authorization code for tokens...")

        // Exchange code for token
        val result = authRepository.exchangeCodeForToken(code, codeVerifier)

        if (result.isSuccess) {
            Log.i(TAG, "Successfully obtained access token")
        } else {
            Log.e(TAG, "Failed to exchange code for token: ${result.exceptionOrNull()?.message}")
        }

        return result
    }

    /**
     * Build authorization URL with specific endpoint
     */
    private fun buildAuthorizationUrl(
        authorizeUrl: String,
        clientId: String,
        redirectUri: String,
        scope: String,
        codeChallenge: String,
        state: String
    ): String {
        val params = mapOf(
            "response_type" to Constants.RESPONSE_TYPE_CODE,
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "scope" to scope,
            "state" to state,
            "code_challenge" to codeChallenge,
            "code_challenge_method" to Constants.CODE_CHALLENGE_METHOD_S256,
            "access_type" to "offline" // Request refresh token
        )

        val queryString = params.toQueryString()

        // Remove any existing query string from authorize URL
        val cleanAuthorizeUrl = authorizeUrl.split("?")[0]

        return "$cleanAuthorizeUrl?$queryString"
    }

    /**
     * Generate random state for CSRF protection
     */
    private fun generateState(): String {
        return java.util.UUID.randomUUID().toString()
    }

    /**
     * Logout user - opens logout URL in browser with callback redirect
     * Uses GET /oauth/logout according to OAuth2 spec
     */
    fun logout() {
        Log.d(TAG, "Starting logout flow")
        val settings = settingsRepository.getSettings()
        val config = authRepository.getCachedConfig()
            ?: throw IllegalStateException("OpenID Configuration not available")

        val logoutUrl = buildLogoutUrl(
            logoutUrl = config.endSessionEndpoint!!,
            clientId = settings.authCodeClientId,
            continueUrl = settings.authCodeRedirectUri,
            scopeAll = false
        )

        Log.i(TAG, "Opening logout URL: $logoutUrl")

        // Open Custom Tab with FLAG_ACTIVITY_NEW_TASK
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(context, Uri.parse(logoutUrl))
    }

    /**
     * Build logout URL with query parameters
     */
    private fun buildLogoutUrl(
        logoutUrl: String,
        clientId: String,
        continueUrl: String,
        scopeAll: Boolean
    ): String {
        val params = mutableMapOf(
            "client_id" to clientId,
            "continue" to continueUrl
        )

        if (scopeAll) {
            params["scope"] = "all"
        }

        val queryString = params.toQueryString()
        val cleanLogoutUrl = logoutUrl.split("?")[0]

        return "$cleanLogoutUrl?$queryString"
    }

    /**
     * Clear current flow data
     */
    fun clearFlow() {
        Log.d(TAG, "Clearing Authorization Code flow data")
        currentPKCEPair = null
        currentState = null
    }
}
