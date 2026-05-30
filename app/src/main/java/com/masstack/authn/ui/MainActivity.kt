package com.masstack.authn.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.masstack.authn.BuildConfig
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.ui.flows.AuthorizationCodeActivity
import com.masstack.authn.ui.flows.CibaActivity
import com.masstack.authn.ui.flows.WebAuthnActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    companion object {
        private const val EXTRA_AUTO_STARTED = "auto_started"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Always set content first to avoid blank screen when returning from other activities
        setContent {
            MaterialTheme {
                MainScreen(
                    onSettingsClick = { navigateToSettings() },
                    onLogsClick = { navigateToLogs() },
                    onAuthCodeClick = { navigateToAuthCode() },
                    onWebAuthnClick = { navigateToWebAuthn() },
                    onCibaClick = { navigateToCiba() }
                )
            }
        }

        // Auto-start Authorization Code flow if configured
        // Only on initial app launch, not when returning from AuthorizationCodeActivity
        val alreadyAutoStarted = intent.getBooleanExtra(EXTRA_AUTO_STARTED, false)
        val settings = settingsRepository.getSettings()

        if (!alreadyAutoStarted && settings.isAuthCodeConfigured()) {
            navigateToAuthCode()
        }
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun navigateToAuthCode() {
        startActivity(Intent(this, AuthorizationCodeActivity::class.java))
    }

    private fun navigateToWebAuthn() {
        startActivity(Intent(this, WebAuthnActivity::class.java))
    }

    private fun navigateToCiba() {
        startActivity(Intent(this, CibaActivity::class.java))
    }

    private fun navigateToLogs() {
        startActivity(Intent(this, LogsActivity::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    onLogsClick: () -> Unit,
    onAuthCodeClick: () -> Unit,
    onWebAuthnClick: () -> Unit,
    onCibaClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OAuth2 Flow Tester v${BuildConfig.APP_VERSION}") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                onSettingsClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Debug Logs") },
                            onClick = {
                                showMenu = false
                                onLogsClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.BugReport, contentDescription = null)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Select an OAuth2 flow to test",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // OAuth Flow Cards
            OAuthFlowCard(
                title = "Authorization Code",
                description = "OAuth2 Authorization Code with PKCE",
                icon = Icons.Default.Key,
                onClick = onAuthCodeClick
            )

            OAuthFlowCard(
                title = "WebAuthn",
                description = "Web Authentication API (Biometric)",
                icon = Icons.Default.Fingerprint,
                onClick = onWebAuthnClick
            )

            OAuthFlowCard(
                title = "CIBA",
                description = "Client Initiated Backchannel Authentication",
                icon = Icons.Default.PhoneAndroid,
                onClick = onCibaClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthFlowCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
