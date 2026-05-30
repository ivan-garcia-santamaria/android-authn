package com.masstack.authn.services

import android.content.Context
import android.util.Log
import com.masstack.authn.data.models.CibaResponse
import com.masstack.authn.data.models.ServerError
import com.masstack.authn.data.models.TokenResponse
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.utils.Constants
import com.masstack.authn.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for CIBA (Client Initiated Backchannel Authentication) flow
 */
@Singleton
class CibaService @Inject constructor(
    private val authRepository: AuthRepository
) {

    companion object {
        private const val TAG = "CibaService"
    }

    /**
     * State of CIBA authentication
     */
    sealed class CibaState {
        object Idle : CibaState()
        data class Authorizing(val authReqId: String, val expiresIn: Int, val interval: Int) : CibaState()
        data class Polling(val authReqId: String, val attempt: Int, val maxAttempts: Int) : CibaState()
        data class Success(val tokenResponse: TokenResponse) : CibaState()
        data class Error(
            val message: String,
            val errorCode: String? = null,
            val exception: Throwable? = null
        ) : CibaState()
    }

    /**
     * Start CIBA flow
     * Initiates backchannel authorization and returns a flow of states
     */
    fun startCibaFlow(
        context: Context,
        loginHint: String,
        bindingMessage: String? = null
    ): Flow<CibaState> = flow {
        Log.d(TAG, "=== CIBA Flow Started ===")
        Log.d(TAG, "Login hint: $loginHint")
        Log.d(TAG, "Binding message: ${bindingMessage ?: "none"}")

        emit(CibaState.Idle)

        // Step 1: Initiate backchannel authorization
        Log.d(TAG, "Step 1: Initiating backchannel authorization...")
        val authResult = authRepository.cibaAuthorize(loginHint, bindingMessage)

        if (authResult.isFailure) {
            val exception = authResult.exceptionOrNull()
            val error = exception?.message ?: "Authorization failed"
            Log.e(TAG, "Backchannel authorization failed: $error")
            Log.e(TAG, "Exception: $exception")

            if (exception is ServerError) {
                emit(CibaState.Error(error, "SERVER_${exception.code}", exception))
            } else {
                emit(CibaState.Error(error))
            }
            return@flow
        }

        val cibaResponse = authResult.getOrNull()!!
        Log.i(TAG, "Backchannel authorization successful!")
        Log.d(TAG, "Auth Request ID: ${cibaResponse.authReqId}")
        Log.d(TAG, "Expires in: ${cibaResponse.expiresIn} seconds")
        Log.d(TAG, "Polling interval: ${cibaResponse.interval} seconds")

        emit(
            CibaState.Authorizing(
                authReqId = cibaResponse.authReqId,
                expiresIn = cibaResponse.expiresIn,
                interval = cibaResponse.interval
            )
        )

        // Step 2: Poll for token
        val pollingInterval = (cibaResponse.interval * 1000L).coerceAtLeast(Constants.CIBA_DEFAULT_INTERVAL)
        val maxAttempts = calculateMaxAttempts(cibaResponse.expiresIn, cibaResponse.interval)

        Log.d(TAG, "Step 2: Starting token polling...")
        Log.d(TAG, "Polling interval: ${pollingInterval}ms")
        Log.d(TAG, "Max attempts: $maxAttempts")

        var attempt = 0
        var shouldContinue = true

        while (shouldContinue && attempt < maxAttempts) {
            attempt++
            Log.d(TAG, "=== Polling attempt $attempt/$maxAttempts ===")
            emit(CibaState.Polling(cibaResponse.authReqId, attempt, maxAttempts))

            // Wait for the specified interval
            Log.d(TAG, "Waiting ${pollingInterval}ms before polling...")
            delay(pollingInterval)

            // Check network connectivity before polling
            Log.d(TAG, "Checking network connectivity...")
            val isNetworkAvailable = NetworkUtils.isNetworkAvailable(context)
            Log.d(TAG, "Network available: $isNetworkAvailable")

            if (!isNetworkAvailable) {
                Log.w(TAG, "⚠️ Network unavailable before polling - will retry")
                Log.d(TAG, "Network type: ${NetworkUtils.getNetworkType(context)}")
                // Don't increment attempt counter for network issues, just wait and retry
                attempt--
                delay(2000L) // Wait 2 seconds before checking again
                continue
            }

            // Poll for token
            Log.d(TAG, "Polling for token with auth_req_id: ${cibaResponse.authReqId}")
            val tokenResult = authRepository.cibaToken(cibaResponse.authReqId)
            Log.d(TAG, "Poll result: ${if (tokenResult.isSuccess) "SUCCESS" else "FAILURE"}")

            if (tokenResult.isSuccess) {
                // Token obtained successfully
                val tokenResponse = tokenResult.getOrNull()!!
                Log.i(TAG, "✅ Token obtained successfully!")
                Log.d(TAG, "Access token: ${tokenResponse.accessToken.take(20)}...")
                Log.d(TAG, "Token type: ${tokenResponse.tokenType}")
                Log.d(TAG, "Expires in: ${tokenResponse.expiresIn} seconds")
                emit(CibaState.Success(tokenResponse))
                shouldContinue = false
            } else {
                // Check error type
                val error = tokenResult.exceptionOrNull()?.message ?: ""
                val exception = tokenResult.exceptionOrNull()

                Log.w(TAG, "Poll failed with error: $error")
                Log.w(TAG, "Exception type: ${exception?.javaClass?.simpleName}")
                Log.w(TAG, "Full exception: $exception")

                when {
                    error.contains(Constants.ERROR_AUTHORIZATION_PENDING, ignoreCase = true) -> {
                        // Continue polling
                        Log.d(TAG, "Authorization pending, will continue polling...")
                        continue
                    }

                    error.contains(Constants.ERROR_SLOW_DOWN, ignoreCase = true) -> {
                        // Increase polling interval by 5 seconds as per spec
                        Log.d(TAG, "Slow down requested, adding 5s delay...")
                        delay(5000L)
                        continue
                    }

                    error.contains(Constants.ERROR_EXPIRED_TOKEN, ignoreCase = true) -> {
                        Log.e(TAG, "Authorization request expired")
                        emit(CibaState.Error("Authorization request expired", Constants.ERROR_EXPIRED_TOKEN))
                        shouldContinue = false
                    }

                    error.contains(Constants.ERROR_ACCESS_DENIED, ignoreCase = true) -> {
                        Log.e(TAG, "Access denied by user")
                        emit(CibaState.Error("Access denied by user", Constants.ERROR_ACCESS_DENIED))
                        shouldContinue = false
                    }

                    error.contains("Unable to resolve host", ignoreCase = true) -> {
                        Log.e(TAG, "❌ DNS Resolution failure during poll - network lost mid-request")
                        Log.e(TAG, "This typically happens when app loses network connectivity")
                        Log.d(TAG, "Current network type: ${NetworkUtils.getNetworkType(context)}")

                        // Check if network is back
                        val isNetworkNowAvailable = NetworkUtils.isNetworkAvailable(context)
                        Log.d(TAG, "Network check after DNS failure: $isNetworkNowAvailable")

                        if (isNetworkNowAvailable) {
                            // Network is back, retry this attempt
                            Log.i(TAG, "Network is back, retrying this poll attempt...")
                            attempt-- // Don't count this as a failed attempt
                            delay(2000L) // Wait 2s before retry
                            continue
                        } else {
                            // Network still unavailable, wait and retry
                            Log.w(TAG, "Network still unavailable, waiting before retry...")
                            attempt-- // Don't count this as a failed attempt
                            delay(5000L) // Wait 5s for network to come back
                            continue
                        }
                    }

                    else -> {
                        Log.e(TAG, "Unexpected error, stopping polling: $error")
                        emit(CibaState.Error(error))
                        shouldContinue = false
                    }
                }
            }
        }

        // If max attempts reached without success
        if (shouldContinue && attempt >= maxAttempts) {
            Log.e(TAG, "Maximum polling attempts ($maxAttempts) reached without success")
            emit(CibaState.Error("Maximum polling attempts reached. Request timed out."))
        }

        Log.d(TAG, "=== CIBA Flow Ended ===")
    }

    /**
     * Calculate maximum polling attempts based on expiry time and interval
     */
    private fun calculateMaxAttempts(expiresIn: Int, interval: Int): Int {
        return (expiresIn / interval).coerceAtMost(Constants.CIBA_MAX_ATTEMPTS)
    }
}
