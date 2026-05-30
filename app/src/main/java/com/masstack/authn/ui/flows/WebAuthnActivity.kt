package com.masstack.authn.ui.flows

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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.masstack.authn.data.models.ServerError
import com.masstack.authn.data.models.TokenResponse
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.services.WebAuthnService
import com.masstack.authn.utils.copyToClipboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebAuthnActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WebAuthnActivity"
    }

    @Inject
    lateinit var webAuthnService: WebAuthnService

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private var tokenResponse by mutableStateOf<TokenResponse?>(null)
    private var registrationVerified by mutableStateOf(false)
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var operationType by mutableStateOf<WebAuthnOperationType>(WebAuthnOperationType.NONE)
    private var warningMessage by mutableStateOf<String?>(null)

    enum class WebAuthnOperationType {
        NONE, REGISTER, AUTHENTICATE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        // Check WebAuthn availability
        Log.d(TAG, "Checking WebAuthn availability...")
        if (!webAuthnService.isWebAuthnAvailable()) {
            warningMessage = webAuthnService.getWebAuthnUnavailabilityReason()
            Log.w(TAG, "WebAuthn unavailable: $warningMessage")
        } else {
            Log.d(TAG, "WebAuthn is available on this device")
        }

        setContent {
            MaterialTheme {
                WebAuthnScreen(
                    tokenResponse = tokenResponse,
                    registrationVerified = registrationVerified,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    warningMessage = warningMessage,
                    operationType = operationType,
                    defaultUsername = settingsRepository.getSettings().username,
                    onRegister = { username -> startRegistration(username) },
                    onAuthenticate = { username -> startAuthentication(username) },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun startRegistration(username: String) {
        Log.i(TAG, "=== Starting WebAuthn Registration ===")
        Log.d(TAG, "Username: $username")

        if (username.isBlank()) {
            Log.w(TAG, "Registration aborted: username is blank")
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        operationType = WebAuthnOperationType.REGISTER
        errorMessage = null
        tokenResponse = null
        registrationVerified = false

        lifecycleScope.launch {
            val settings = settingsRepository.getSettings()

            Log.d(TAG, "Resolving endpoints from OpenID Configuration...")
            val checkResult = authRepository.checkServerAvailability()
            if (checkResult.isFailure) {
                val exception = checkResult.exceptionOrNull()
                Log.e(TAG, "Server availability check failed: ${exception?.message}")
                if (settings.enableServerDownScreen) {
                    val intent = Intent(this@WebAuthnActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                    val errorCode = if (exception is ServerError) exception.code else 0
                    intent.putExtra("error_message", exception?.message ?: "Server is not available")
                    intent.putExtra("error_code", errorCode)
                    startActivity(intent)
                    finish()
                } else {
                    errorMessage = exception?.message ?: "Server is not available"
                    operationType = WebAuthnOperationType.NONE
                }
                isLoading = false
                return@launch
            }
            Log.d(TAG, "Endpoints resolved - proceeding with registration")

            Log.d(TAG, "Calling webAuthnService.registerCredential()")
            val result = webAuthnService.registerCredential(username)

            isLoading = false
            if (result.isSuccess) {
                val verificationResponse = result.getOrNull()
                registrationVerified = verificationResponse?.verified == true
                Log.i(TAG, "✅ Registration successful! Verified: $registrationVerified")
                Toast.makeText(this@WebAuthnActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "❌ Registration failed: ${exception?.message}")

                if (exception is ServerError && settings.enableServerDownScreen) {
                    Log.w(TAG, "Server error ${exception.code} - navigating to ServerDownActivity")
                    val intent = Intent(this@WebAuthnActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                    intent.putExtra("error_message", exception.message)
                    intent.putExtra("error_code", exception.code)
                    startActivity(intent)
                    finish()
                } else {
                    errorMessage = exception?.message ?: "Registration failed"
                    operationType = WebAuthnOperationType.NONE
                }
            }
        }
    }

    private fun startAuthentication(username: String) {
        Log.i(TAG, "=== Starting WebAuthn Authentication ===")
        Log.d(TAG, "Username: $username")

        if (username.isBlank()) {
            Log.w(TAG, "Authentication aborted: username is blank")
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        operationType = WebAuthnOperationType.AUTHENTICATE
        errorMessage = null
        tokenResponse = null

        lifecycleScope.launch {
            val settings = settingsRepository.getSettings()

            Log.d(TAG, "Resolving endpoints from OpenID Configuration...")
            val checkResult = authRepository.checkServerAvailability()
            if (checkResult.isFailure) {
                val exception = checkResult.exceptionOrNull()
                Log.e(TAG, "Server availability check failed: ${exception?.message}")
                if (settings.enableServerDownScreen) {
                    val intent = Intent(this@WebAuthnActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                    val errorCode = if (exception is ServerError) exception.code else 0
                    intent.putExtra("error_message", exception?.message ?: "Server is not available")
                    intent.putExtra("error_code", errorCode)
                    startActivity(intent)
                    finish()
                } else {
                    errorMessage = exception?.message ?: "Server is not available"
                    operationType = WebAuthnOperationType.NONE
                }
                isLoading = false
                return@launch
            }
            Log.d(TAG, "Endpoints resolved - proceeding with authentication")

            Log.d(TAG, "Calling webAuthnService.authenticateCredential()")
            val result = webAuthnService.authenticateCredential(username)

            isLoading = false
            if (result.isSuccess) {
                tokenResponse = result.getOrNull()
                Log.i(TAG, "✅ Authentication successful! Token obtained")
                Toast.makeText(this@WebAuthnActivity, "Authentication successful!", Toast.LENGTH_SHORT).show()
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "❌ Authentication failed: ${exception?.message}")

                if (exception is ServerError && settings.enableServerDownScreen) {
                    Log.w(TAG, "Server error ${exception.code} - navigating to ServerDownActivity")
                    val intent = Intent(this@WebAuthnActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                    intent.putExtra("error_message", exception.message)
                    intent.putExtra("error_code", exception.code)
                    startActivity(intent)
                    finish()
                } else {
                    errorMessage = exception?.message ?: "Authentication failed"
                    operationType = WebAuthnOperationType.NONE
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAuthnScreen(
    tokenResponse: TokenResponse?,
    registrationVerified: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    warningMessage: String?,
    operationType: WebAuthnActivity.WebAuthnOperationType,
    defaultUsername: String,
    onRegister: (String) -> Unit,
    onAuthenticate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf(defaultUsername) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebAuthn Flow") },
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
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "WebAuthn",
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = "Web Authentication API",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use biometric authentication (fingerprint, face, etc.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Warning message for availability issues
            warningMessage?.let { warning ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ WebAuthn Unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Username Input
            if (tokenResponse == null && !registrationVerified && !isLoading) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { Text("Enter username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Register Button
                Button(
                    onClick = { onRegister(username) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Register",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register New Credential")
                }

                // Authenticate Button
                OutlinedButton(
                    onClick = { onAuthenticate(username) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Authenticate",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authenticate with Credential")
                }

                Divider()

                // Instructions Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Instructions",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Register: Create a new biometric credential for this username\n" +
                                    "2. Authenticate: Use your registered credential to get an access token\n\n" +
                                    "Note: You must register before you can authenticate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Loading
            if (isLoading) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = when (operationType) {
                                WebAuthnActivity.WebAuthnOperationType.REGISTER -> "📝 Registering credential..."
                                WebAuthnActivity.WebAuthnOperationType.AUTHENTICATE -> "🔐 Authenticating..."
                                else -> "Processing..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Please complete the biometric prompt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
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
            }

            // Success - Registration Verification
            if (registrationVerified && operationType == WebAuthnActivity.WebAuthnOperationType.REGISTER) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✓ Registration Successful",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your credential has been registered. You can now authenticate to get an access token.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // New Operation Button
                Button(
                    onClick = {
                        // Reset and go back
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }

            // Success - Token Display (Authentication)
            tokenResponse?.let { token ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✓ Authentication Successful",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                TokenDisplay(
                    tokenResponse = token,
                    onCopy = { label, text ->
                        context.copyToClipboard(label, text)
                    }
                )

                // New Operation Button
                Button(
                    onClick = {
                        // This would reset the state in the activity
                        // For now, just navigate back
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start New Operation")
                }
            }
        }
    }
}
