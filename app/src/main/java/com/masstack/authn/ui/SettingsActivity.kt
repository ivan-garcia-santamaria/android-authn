package com.masstack.authn.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.masstack.authn.data.models.OpenIdConfiguration
import com.masstack.authn.data.models.Settings
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.Intent
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val currentSettings = settingsRepository.getSettings()
                val coroutineScope = rememberCoroutineScope()

                SettingsScreen(
                    initialSettings = currentSettings,
                    onSave = { settings ->
                        settingsRepository.saveSettings(settings)
                        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onBack = { finish() },
                    onTestServerDownScreen = {
                        val intent = Intent(this, ServerDownActivity::class.java)
                        intent.putExtra("error_message", "Service Unavailable")
                        intent.putExtra("error_code", 503)
                        startActivity(intent)
                    },
                    onDiscover = { url, onResult ->
                        coroutineScope.launch {
                            val result = authRepository.fetchOpenIdConfiguration(url)
                            onResult(result)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialSettings: Settings,
    onSave: (Settings) -> Unit,
    onBack: () -> Unit,
    onTestServerDownScreen: () -> Unit = {},
    onDiscover: (url: String, onResult: (Result<OpenIdConfiguration>) -> Unit) -> Unit = { _, _ -> }
) {
    // Global settings
    var scope by remember { mutableStateOf(initialSettings.scope) }
    var username by remember { mutableStateOf(initialSettings.username) }
    var openidConfigurationUrl by remember { mutableStateOf(initialSettings.openidConfigurationUrl) }
    var enableServerDownScreen by remember { mutableStateOf(initialSettings.enableServerDownScreen) }
    var serverDownGame by remember { mutableStateOf(initialSettings.serverDownGame) }

    // Discovery state
    var isDiscovering by remember { mutableStateOf(false) }
    var discoveryError by remember { mutableStateOf<String?>(null) }
    var discoveredConfig by remember { mutableStateOf<OpenIdConfiguration?>(null) }

    // Authorization Code Flow configuration
    var authCodeClientId by remember { mutableStateOf(initialSettings.authCodeClientId) }
    var authCodeClientSecret by remember { mutableStateOf(initialSettings.authCodeClientSecret) }
    var authCodeRedirectUri by remember { mutableStateOf(initialSettings.authCodeRedirectUri) }

    // CIBA Flow configuration
    var cibaClientId by remember { mutableStateOf(initialSettings.cibaClientId) }
    var cibaClientSecret by remember { mutableStateOf(initialSettings.cibaClientSecret) }
    var cibaPollingInterval by remember { mutableStateOf(initialSettings.cibaPollingInterval.toString()) }

    // WebAuthn Flow configuration
    var webauthnClientId by remember { mutableStateOf(initialSettings.webauthnClientId) }
    var webauthnClientSecret by remember { mutableStateOf(initialSettings.webauthnClientSecret) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OAuth Configuration") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Global Settings Section
            SectionHeader(text = "Global Settings")

            OutlinedTextField(
                value = scope,
                onValueChange = { scope = it },
                label = { Text("Scope") },
                placeholder = { Text("openid profile api:everything") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                placeholder = { Text("Default username for testing") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = openidConfigurationUrl,
                onValueChange = { openidConfigurationUrl = it },
                label = { Text("OpenID Configuration URL") },
                placeholder = { Text("https://.../.well-known/openid-configuration") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Button(
                onClick = {
                    isDiscovering = true
                    discoveryError = null
                    discoveredConfig = null
                    onDiscover(openidConfigurationUrl) { result ->
                        isDiscovering = false
                        result.onSuccess { config ->
                            discoveredConfig = config
                        }
                        result.onFailure { error ->
                            discoveryError = error.message ?: "Discovery failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = openidConfigurationUrl.isNotBlank() && !isDiscovering
            ) {
                if (isDiscovering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Discovering...")
                } else {
                    Text("Discover Endpoints")
                }
            }

            discoveredConfig?.let { config ->
                Text(
                    text = "Endpoints discovered successfully",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Discovered Endpoints", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        config.authorizationEndpoint?.let { EndpointRow("Authorization", it) }
                        config.tokenEndpoint?.let { EndpointRow("Token", it) }
                        config.endSessionEndpoint?.let { EndpointRow("Logout", it) }
                        config.backchannelAuthenticationEndpoint?.let { EndpointRow("CIBA Backchannel", it) }
                        config.webauthnRegistrationOptionsEndpoint?.let { EndpointRow("WebAuthn Reg Options", it) }
                        config.webauthnRegistrationVerificationEndpoint?.let { EndpointRow("WebAuthn Reg Verify", it) }
                        config.webauthnAuthenticationOptionsEndpoint?.let { EndpointRow("WebAuthn Auth Options", it) }
                        config.webauthnAuthenticationVerificationEndpoint?.let { EndpointRow("WebAuthn Auth Verify", it) }
                    }
                }
            }

            discoveryError?.let { error ->
                Text(
                    text = "Discovery failed: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Server Down Screen",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Show game and Goku on server errors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableServerDownScreen,
                    onCheckedChange = { enableServerDownScreen = it }
                )
            }

            OutlinedButton(
                onClick = onTestServerDownScreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview Server Down Screen")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Game Selection",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Choose which game to show on server down screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = serverDownGame == "SNAKE",
                    onClick = { serverDownGame = "SNAKE" },
                    label = { Text("Snake") }
                )
                FilterChip(
                    selected = serverDownGame == "TETRIS",
                    onClick = { serverDownGame = "TETRIS" },
                    label = { Text("Tetris") }
                )
                FilterChip(
                    selected = serverDownGame == "SPACE_INVADERS",
                    onClick = { serverDownGame = "SPACE_INVADERS" },
                    label = { Text("Invaders") }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Authorization Code Flow Section
            SectionHeader(text = "Authorization Code Flow")

            OutlinedTextField(
                value = authCodeClientId,
                onValueChange = { authCodeClientId = it },
                label = { Text("Client ID *") },
                placeholder = { Text("Authorization Code client ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = authCodeClientSecret,
                onValueChange = { authCodeClientSecret = it },
                label = { Text("Client Secret") },
                placeholder = { Text("Authorization Code client secret (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = authCodeRedirectUri,
                onValueChange = { authCodeRedirectUri = it },
                label = { Text("Redirect URI") },
                placeholder = { Text("com.masstack.authn://oauth/callback") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // CIBA Flow Section
            SectionHeader(text = "CIBA Flow")

            OutlinedTextField(
                value = cibaClientId,
                onValueChange = { cibaClientId = it },
                label = { Text("CIBA Client ID *") },
                placeholder = { Text("CIBA-specific client ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = cibaClientSecret,
                onValueChange = { cibaClientSecret = it },
                label = { Text("CIBA Client Secret *") },
                placeholder = { Text("CIBA-specific client secret") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = cibaPollingInterval,
                onValueChange = { cibaPollingInterval = it },
                label = { Text("Polling Interval (seconds)") },
                placeholder = { Text("5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // WebAuthn Flow Section
            SectionHeader(text = "WebAuthn Flow")

            OutlinedTextField(
                value = webauthnClientId,
                onValueChange = { webauthnClientId = it },
                label = { Text("WebAuthn Client ID *") },
                placeholder = { Text("WebAuthn-specific client ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = webauthnClientSecret,
                onValueChange = { webauthnClientSecret = it },
                label = { Text("WebAuthn Client Secret") },
                placeholder = { Text("WebAuthn-specific client secret (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val settings = Settings(
                        scope = scope,
                        username = username,
                        openidConfigurationUrl = openidConfigurationUrl,
                        enableServerDownScreen = enableServerDownScreen,
                        serverDownGame = serverDownGame,
                        authCodeClientId = authCodeClientId,
                        authCodeClientSecret = authCodeClientSecret,
                        authCodeRedirectUri = authCodeRedirectUri,
                        cibaClientId = cibaClientId,
                        cibaClientSecret = cibaClientSecret,
                        cibaPollingInterval = cibaPollingInterval.toIntOrNull() ?: 5,
                        webauthnClientId = webauthnClientId,
                        webauthnClientSecret = webauthnClientSecret
                    )
                    onSave(settings)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun EndpointRow(label: String, url: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
