package com.masstack.authn.ui.flows

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.masstack.authn.data.models.ServerError
import com.masstack.authn.data.models.TokenResponse
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.services.AuthorizationCodeService
import com.masstack.authn.utils.Constants
import com.masstack.authn.utils.copyToClipboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthorizationCodeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AuthCodeActivity"
    }

    @Inject
    lateinit var authCodeService: AuthorizationCodeService

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var tokenResponse by mutableStateOf<TokenResponse?>(null)
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var isWaitingForCallback by mutableStateOf(false)
    private var redirectCountdown by mutableStateOf<Int?>(null)
    private var shouldCheckForCustomTabClosure = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        // Handle callback from Custom Tab
        handleIntent(intent)

        // Auto-start authorization if configured and not a callback
        val isCallback = intent?.data != null
        Log.d(TAG, "Is callback: $isCallback")

        if (!isCallback) {
            val settings = settingsRepository.getSettings()
            Log.d(TAG, "Checking if AuthCode is configured...")
            if (settings.isAuthCodeConfigured()) {
                Log.i(TAG, "AuthCode configured - auto-starting flow")
                // Auto-start the flow
                startAuthorization()
            } else {
                Log.d(TAG, "AuthCode not configured - waiting for user action")
            }
        }

        setContent {
            MaterialTheme {
                AuthorizationCodeScreen(
                    tokenResponse = tokenResponse,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    isWaitingForCallback = isWaitingForCallback,
                    redirectCountdown = redirectCountdown,
                    onStartAuth = { startAuthorization() },
                    onLogout = { logout() },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent() called with intent: ${intent.data}")
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() - Activity gaining focus")
        Log.d(TAG, "  isWaitingForCallback: $isWaitingForCallback")
        Log.d(TAG, "  shouldCheckForCustomTabClosure: $shouldCheckForCustomTabClosure")

        // Only check for custom tab closure if we've explicitly set the flag
        // This prevents false positives on the first resume when the activity is created
        if (shouldCheckForCustomTabClosure && isWaitingForCallback) {
            Log.w(TAG, "Custom Tab was closed without completing auth")
            shouldCheckForCustomTabClosure = false
            isWaitingForCallback = false
            authCodeService.clearFlow()

            // User closed the Custom Tab without completing auth - redirect to MainActivity
            Log.d(TAG, "Redirecting to MainActivity...")
            val intent = Intent(this@AuthorizationCodeActivity, com.masstack.authn.ui.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("auto_started", true)
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() - Activity losing focus")
        Log.d(TAG, "  isWaitingForCallback: $isWaitingForCallback")

        // If we're waiting for callback and activity is pausing (Custom Tab opening or user navigating away)
        // set flag to check for closure when we resume
        if (isWaitingForCallback) {
            Log.d(TAG, "Setting shouldCheckForCustomTabClosure flag")
            shouldCheckForCustomTabClosure = true
        }
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        Log.d(TAG, "handleIntent() called with data: $data")

        if (data != null && data.scheme == "com.masstack.authn") {
            Log.i(TAG, "Received OAuth callback")
            isWaitingForCallback = false
            shouldCheckForCustomTabClosure = false // Reset flag when callback is received

            // Check if it's a logout callback or authorization code callback
            val code = data.getQueryParameter("code")
            if (code != null) {
                Log.d(TAG, "Authorization code found in callback - handling code exchange")
                // Authorization code callback
                handleCallback(data)
            } else {
                Log.d(TAG, "No code in callback - handling as logout callback")
                // Logout callback - redirect to MainActivity
                handleLogoutCallback()
            }
        } else {
            Log.d(TAG, "No callback data or wrong scheme")
        }
    }

    private fun handleLogoutCallback() {
        Log.i(TAG, "Handling logout callback")
        // Clear local state
        tokenResponse = null
        errorMessage = null
        authCodeService.clearFlow()

        // Navigate to MainActivity (main screen) with flag to prevent auto-start loop
        Log.d(TAG, "Redirecting to MainActivity after logout")
        val intent = Intent(this, com.masstack.authn.ui.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("auto_started", true)
        startActivity(intent)
        finish()
    }

    private fun startAuthorization() {
        Log.i(TAG, "=== Starting Authorization Flow ===")
        isLoading = true
        errorMessage = null
        tokenResponse = null

        lifecycleScope.launch {
            // Check if Server Down screen is enabled in settings
            val settings = settingsRepository.getSettings()

            Log.d(TAG, "Resolving endpoints from OpenID Configuration...")
            val checkResult = authCodeService.checkServerAvailability()

            if (checkResult.isFailure) {
                val exception = checkResult.exceptionOrNull()
                Log.e(TAG, "Server availability check failed: ${exception?.message}")

                if (settings.enableServerDownScreen) {
                    val intent = Intent(this@AuthorizationCodeActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                    val errorCode = if (exception is ServerError) exception.code else 0
                    intent.putExtra("error_message", exception?.message ?: "Server is not available")
                    intent.putExtra("error_code", errorCode)
                    startActivity(intent)
                    finish()
                } else {
                    errorMessage = exception?.message ?: "Server is not available"
                }

                isLoading = false
                return@launch
            }

            Log.i(TAG, "Endpoints resolved - proceeding with authorization")

            isLoading = false
            isWaitingForCallback = true

            try {
                authCodeService.startAuthorizationFlow()
                Log.d(TAG, "Authorization flow started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start authorization: ${e.message}", e)
                isWaitingForCallback = false
                shouldCheckForCustomTabClosure = false
                errorMessage = "Failed to start authorization: ${e.message}"
            }
        }
    }

    private fun handleCallback(uri: Uri) {
        Log.i(TAG, "=== Handling Authorization Callback ===")
        Log.d(TAG, "Callback URI: $uri")
        isLoading = true

        lifecycleScope.launch {
            Log.d(TAG, "Exchanging authorization code for token...")
            val result = authCodeService.handleCallback(uri)

            isLoading = false
            if (result.isSuccess) {
                tokenResponse = result.getOrNull()
                Log.i(TAG, "✅ Authorization successful! Token obtained")
                Toast.makeText(this@AuthorizationCodeActivity, "Authorization successful!", Toast.LENGTH_SHORT).show()

                // Start countdown to redirect to MainActivity
                startRedirectCountdown()
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "❌ Authorization failed: ${exception?.message}")

                val settings = settingsRepository.getSettings()
                if (exception is ServerError && settings.enableServerDownScreen) {
                    Log.w(TAG, "Server error ${exception.code} - navigating to ServerDownActivity")
                    val intent = Intent(this@AuthorizationCodeActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                    intent.putExtra("error_message", exception.message)
                    intent.putExtra("error_code", exception.code)
                    startActivity(intent)
                    finish()
                } else {
                    errorMessage = exception?.message ?: "Unknown error"
                    Log.e(TAG, "Setting error message: $errorMessage")
                }
            }

            authCodeService.clearFlow()
            Log.d(TAG, "Authorization flow data cleared")
        }
    }

    private fun startRedirectCountdown() {
        Log.d(TAG, "Starting redirect countdown (5 seconds)")
        lifecycleScope.launch {
            for (i in 5 downTo 1) {
                redirectCountdown = i
                delay(1000)
            }

            // Redirect to MainActivity with flag to prevent auto-start loop
            Log.d(TAG, "Countdown finished - redirecting to MainActivity")
            val intent = Intent(this@AuthorizationCodeActivity, com.masstack.authn.ui.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("auto_started", true)
            startActivity(intent)
            finish()
        }
    }

    private fun logout() {
        Log.i(TAG, "=== Starting Logout ===")
        try {
            // Open logout URL in browser
            authCodeService.logout()
            Log.d(TAG, "Logout URL opened in browser")

            // The callback will be handled by handleLogoutCallback()
            // which will clear state and redirect to MainActivity
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start logout: ${e.message}", e)
            Toast.makeText(this, "Failed to start logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizationCodeScreen(
    tokenResponse: TokenResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    isWaitingForCallback: Boolean,
    redirectCountdown: Int?,
    onStartAuth: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authorization Code Flow") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OAuth2 Authorization Code with PKCE",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Redirect URI:\n${Constants.REDIRECT_URI}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Start Button
            if (!isWaitingForCallback && tokenResponse == null && errorMessage == null) {
                Button(
                    onClick = onStartAuth,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Start Authorization")
                }
            }

            // Waiting for callback
            if (isWaitingForCallback) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏳ Waiting for callback...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Please complete the authentication in the browser",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Loading
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Exchanging code for token...")
            }

            // Error
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Button(
                    onClick = onStartAuth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
            }

            // Success - Token Display
            tokenResponse?.let { token ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✓ Authorization Successful",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        // Show countdown if available
                        redirectCountdown?.let { countdown ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Redirecting to main screen in $countdown seconds...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                TokenDisplay(
                    tokenResponse = token,
                    onCopy = { label, text ->
                        context.copyToClipboard(label, text)
                    }
                )

                // Logout Button
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
fun TokenDisplay(
    tokenResponse: TokenResponse,
    onCopy: (String, String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Token Information",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Access Token
            TokenField(
                label = "Access Token",
                value = tokenResponse.accessToken,
                onCopy = { onCopy("Access Token", tokenResponse.accessToken) }
            )

            // Refresh Token
            tokenResponse.refreshToken?.let { refreshToken ->
                Spacer(modifier = Modifier.height(12.dp))
                TokenField(
                    label = "Refresh Token",
                    value = refreshToken,
                    onCopy = { onCopy("Refresh Token", refreshToken) }
                )
            }

            // ID Token
            tokenResponse.idToken?.let { idToken ->
                Spacer(modifier = Modifier.height(12.dp))
                TokenField(
                    label = "ID Token",
                    value = idToken,
                    onCopy = { onCopy("ID Token", idToken) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Additional Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Expires In:", style = MaterialTheme.typography.bodyMedium)
                Text("${tokenResponse.expiresIn} seconds", style = MaterialTheme.typography.bodyMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Token Type:", style = MaterialTheme.typography.bodyMedium)
                Text(tokenResponse.tokenType, style = MaterialTheme.typography.bodyMedium)
            }

            tokenResponse.scope?.let { scope ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Scope:", style = MaterialTheme.typography.bodyMedium)
                    Text(scope, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun TokenField(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = value.take(100) + if (value.length > 100) "..." else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
