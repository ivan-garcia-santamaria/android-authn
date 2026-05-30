package com.masstack.authn.ui.flows

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.masstack.authn.data.models.ServerError
import com.masstack.authn.data.models.TokenResponse
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.services.CibaService
import com.masstack.authn.utils.copyToClipboard
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CibaActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CibaActivity"
    }

    @Inject
    lateinit var cibaService: CibaService

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    private var tokenResponse by mutableStateOf<TokenResponse?>(null)
    private var currentState by mutableStateOf<CibaService.CibaState>(CibaService.CibaState.Idle)
    private var errorMessage by mutableStateOf<String?>(null)
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        setContent {
            MaterialTheme {
                CibaScreen(
                    tokenResponse = tokenResponse,
                    currentState = currentState,
                    errorMessage = errorMessage,
                    defaultUsername = settingsRepository.getSettings().username,
                    onStart = { loginHint, bindingMessage -> startCibaFlow(loginHint, bindingMessage) },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun startCibaFlow(loginHint: String, bindingMessage: String?) {
        if (loginHint.isBlank()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "=== Starting CIBA Flow ===")
        Log.d(TAG, "Login hint: $loginHint")
        Log.d(TAG, "Binding message: ${bindingMessage ?: "none"}")

        // Cancel any existing polling job
        pollingJob?.cancel()

        errorMessage = null
        tokenResponse = null
        currentState = CibaService.CibaState.Idle

        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.getSettings()

            Log.d(TAG, "Resolving endpoints from OpenID Configuration...")
            val checkResult = authRepository.checkServerAvailability()
            if (checkResult.isFailure) {
                val exception = checkResult.exceptionOrNull()
                Log.e(TAG, "Server availability check failed: ${exception?.message}")
                launch(Dispatchers.Main) {
                    if (settings.enableServerDownScreen) {
                        val intent = Intent(this@CibaActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                        val errorCode = if (exception is ServerError) exception.code else 0
                        intent.putExtra("error_message", exception?.message ?: "Server is not available")
                        intent.putExtra("error_code", errorCode)
                        startActivity(intent)
                        finish()
                    } else {
                        errorMessage = exception?.message ?: "Server is not available"
                    }
                }
                return@launch
            }
            Log.d(TAG, "Endpoints resolved - proceeding with CIBA flow")
            Log.d(TAG, "Polling coroutine started")
            try {
                cibaService.startCibaFlow(appContext, loginHint, bindingMessage).collect { state ->
                    Log.d(TAG, "Received state: ${state.javaClass.simpleName}")
                    // Switch to Main dispatcher for UI updates
                    launch(Dispatchers.Main) {
                        currentState = state

                        when (state) {
                            is CibaService.CibaState.Success -> {
                                Log.i(TAG, "CIBA flow completed successfully!")
                                tokenResponse = state.tokenResponse
                                Toast.makeText(this@CibaActivity, "Token obtained!", Toast.LENGTH_SHORT).show()
                            }
                            is CibaService.CibaState.Error -> {
                                Log.e(TAG, "CIBA flow error: ${state.message}")

                                if (state.exception is ServerError && settings.enableServerDownScreen) {
                                    Log.w(TAG, "Server error ${state.exception.code} - navigating to ServerDownActivity")
                                    val intent = Intent(this@CibaActivity, com.masstack.authn.ui.ServerDownActivity::class.java)
                                    intent.putExtra("error_message", state.exception.message)
                                    intent.putExtra("error_code", state.exception.code)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    errorMessage = state.message
                                }
                            }
                            else -> {
                                // Handle other states in UI
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle cancellation or other errors
                Log.e(TAG, "Polling coroutine exception: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                launch(Dispatchers.Main) {
                    if (currentState !is CibaService.CibaState.Success) {
                        errorMessage = "Polling interrupted: ${e.message}"
                        currentState = CibaService.CibaState.Error(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() - Activity losing focus")
        Log.d(TAG, "Polling job active: ${pollingJob?.isActive}")
        Log.d(TAG, "Current state: ${currentState.javaClass.simpleName}")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() - Activity gaining focus")
        Log.d(TAG, "Polling job active: ${pollingJob?.isActive}")
        Log.d(TAG, "Current state: ${currentState.javaClass.simpleName}")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() - Activity stopped")
        Log.d(TAG, "Polling job active: ${pollingJob?.isActive}")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() - Activity started")
        Log.d(TAG, "Polling job active: ${pollingJob?.isActive}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() - Cancelling polling job")
        // Cancel polling when activity is destroyed
        pollingJob?.cancel()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CibaScreen(
    tokenResponse: TokenResponse?,
    currentState: CibaService.CibaState,
    errorMessage: String?,
    defaultUsername: String,
    onStart: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var loginHint by remember { mutableStateOf(defaultUsername) }
    var bindingMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CIBA Flow") },
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
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "CIBA",
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = "Client Initiated Backchannel Authentication",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Authentication via secondary channel with polling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Input Fields (only show when idle or error)
            if (currentState is CibaService.CibaState.Idle || currentState is CibaService.CibaState.Error) {
                OutlinedTextField(
                    value = loginHint,
                    onValueChange = { loginHint = it },
                    label = { Text("Login Hint (Username)") },
                    placeholder = { Text("Enter username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bindingMessage,
                    onValueChange = { bindingMessage = it },
                    label = { Text("Binding Message (Optional)") },
                    placeholder = { Text("Transaction identifier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        onStart(
                            loginHint,
                            bindingMessage.takeIf { it.isNotBlank() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loginHint.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Start",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start CIBA Flow")
                }
            }

            // State Display
            when (currentState) {
                is CibaService.CibaState.Idle -> {
                    // Show instructions
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "How CIBA Works",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. Enter your username (login hint)\n" +
                                        "2. App initiates authentication request\n" +
                                        "3. You receive a notification on your device\n" +
                                        "4. Approve the request on your device\n" +
                                        "5. App polls and obtains the token\n\n" +
                                        "Note: You must approve the request before it expires.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is CibaService.CibaState.Authorizing -> {
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
                                text = "📱 Authorization Initiated",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Divider()
                            Text(
                                text = "Auth Request ID:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = currentState.authReqId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Expires in: ${currentState.expiresIn} seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Polling interval: ${currentState.interval} seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                is CibaService.CibaState.Polling -> {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏳ Polling for Token",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${currentState.attempt}/${currentState.maxAttempts}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            LinearProgressIndicator(
                                progress = currentState.attempt.toFloat() / currentState.maxAttempts.toFloat(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Waiting for user approval on secondary device...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = "💡 Please check your device and approve the authentication request",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                is CibaService.CibaState.Success -> {
                    // Handled by tokenResponse display below
                }

                is CibaService.CibaState.Error -> {
                    // Handled by errorMessage display below
                }
            }

            // Error Display
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
                            text = "✓ CIBA Flow Successful",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Token obtained successfully!",
                            style = MaterialTheme.typography.bodySmall,
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

                // New Flow Button
                Button(
                    onClick = {
                        // Navigate back or reset
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start New CIBA Flow")
                }
            }
        }
    }
}
