# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android OAuth2 testing application for the MasStack Authn server. Implements three OAuth2 flows:
- **Authorization Code Flow** with PKCE (RFC 7636)
- **WebAuthn** (biometric authentication via Android Credential Manager)
- **CIBA** (Client Initiated Backchannel Authentication with polling mechanism)

Built with Kotlin, Jetpack Compose, and MVVM architecture using Hilt for dependency injection.

## Build & Test Commands

### Environment Setup
```bash
# Set JAVA_HOME (required for Gradle)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### Build
```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build (uses signing config from app/build.gradle)
./gradlew assembleRelease
```

### Testing
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew test --tests "com.masstack.authn.utils.PKCEUtilTest"

# View test report
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Install & Run
```bash
# Install debug APK on connected device/emulator
./gradlew installDebug

# Uninstall
./gradlew uninstallDebug
```

## Architecture

### Dependency Injection (Hilt)
- `di/NetworkModule.kt` - Provides Retrofit, OkHttpClient, AuthApi, and interceptors
- `di/AppModule.kt` - Provides repositories and application-level dependencies
- `AuthnApplication.kt` - Hilt application entry point

### Data Layer
- `data/repositories/AuthRepository.kt` - Central OAuth operations hub for all three flows
- `data/repositories/SettingsRepository.kt` - Manages app configuration via EncryptedSharedPreferences
- `data/models/` - DTOs for token responses, CIBA, WebAuthn, settings, OpenIdConfiguration, errors

### Network Layer
- `network/api/AuthApi.kt` - Retrofit interface with dynamic `@Url` parameters for flexible endpoint configuration
- `network/interceptors/LoggingInterceptor.kt` - Custom request/response logger with optional truncation
- `network/interceptors/OriginInterceptor.kt` - Adds Origin header for WebAuthn CORS
- `network/interceptors/AuthInterceptor.kt` - Token injection (currently returns null as tokens are flow-specific)

**Critical**: All API endpoints use `@Url` parameters rather than hardcoded base URLs. Endpoint URLs are auto-discovered from the `.well-known/openid-configuration` document and stored in Settings. Each flow can use different client IDs.

### Service Layer
- `services/AuthorizationCodeService.kt` - PKCE generation, Custom Tabs launch, deep link handling
- `services/CibaService.kt` - Backchannel authorization initiation and token polling with exponential backoff
- `services/WebAuthnService.kt` - Credential creation/authentication via Android Credential Manager API

### UI Layer (Jetpack Compose)
- `ui/MainActivity.kt` - Home screen with flow selection buttons and token status
- `ui/SettingsActivity.kt` - OpenID Configuration discovery and per-flow client credentials
- `ui/flows/AuthorizationCodeActivity.kt` - Authorization Code flow with Custom Tabs integration
- `ui/flows/WebAuthnActivity.kt` - Biometric registration and authentication UI
- `ui/flows/CibaActivity.kt` - CIBA flow with real-time polling status updates
- `ui/LogsActivity.kt` - Debug log viewer for network activity

### Utilities
- `utils/PKCEUtil.kt` - RFC 7636 compliant code_verifier and code_challenge (S256) generation
- `utils/Logger.kt` - Centralized logging utility for debugging OAuth flows
- `utils/Constants.kt` - OAuth grant types, timeouts, redirect URIs
- `utils/Extensions.kt` - Kotlin extension functions for common operations

## Configuration Architecture

Endpoint URLs are auto-discovered from the server's `.well-known/openid-configuration` document via `OpenIdConfiguration.applyTo()`. The Settings UI only exposes client credentials and flow-specific parameters — endpoint URLs are populated automatically.

Settings stored in EncryptedSharedPreferences:
- **Global**: OpenID Configuration URL, scope, username, server down screen toggle
- **Authorization Code**: client ID, secret, redirect URI (+ auto-discovered: authorize, token, logout URLs)
- **CIBA**: client ID, secret, polling interval (+ auto-discovered: backchannel auth, token URLs)
- **WebAuthn**: client ID, secret (+ auto-discovered: registration/authentication options/verification URLs)

Server availability is checked before each login attempt by doing a GET to the OpenID Configuration URL. Any non-200 response or connection error shows the ServerDownActivity with Goku and a mini-game.

When adding features that interact with the server, always check which flow is active and use the corresponding client configuration.

## OAuth Flow Implementations

### Authorization Code + PKCE
1. Generate code_verifier (43-128 chars, base64url)
2. Generate code_challenge = BASE64URL(SHA256(code_verifier))
3. Launch Custom Tab with authorize URL + state + code_challenge + challenge_method=S256
4. Deep link captures callback at `com.masstack.authn://oauth/callback`
5. Extract code and state from intent data
6. Exchange code + code_verifier for tokens via POST to token endpoint
7. Store tokens in EncryptedSharedPreferences

**Deep link handling**: `AuthorizationCodeActivity` has `launchMode="singleTask"` with intent filter for the callback URI. The activity's `onNewIntent()` processes the authorization response.

### CIBA (Backchannel Authentication)
1. POST to `/bc-authorize` with login_hint, scope → receive auth_req_id + interval
2. Start polling: POST to `/oauth/token` with grant_type=urn:openid:params:grant-type:ciba, auth_req_id
3. Handle polling states:
   - `authorization_pending` → continue polling
   - `slow_down` → increase interval
   - Success → receive access_token
   - `expired_token` or other errors → stop polling
4. Store tokens on success

**Polling behavior**: Implemented with coroutines and exponential backoff. UI shows real-time status updates.

### WebAuthn (Biometric)
**Registration**:
1. POST to `/webauthn/register/begin` with username → receive challenge + RP info
2. Use Android `CredentialManager.createCredential()` with PublicKeyCredential
3. POST credential response to `/webauthn/register/complete` → receive verification status

**Authentication**:
1. POST to `/webauthn/authenticate/begin` with username → receive challenge + allowCredentials
2. Use Android `CredentialManager.getCredential()` to get assertion
3. POST assertion to `/webauthn/authenticate/complete` → receive access_token

**Note**: Registration only returns verification status; authentication returns a full TokenResponse.

## Security

- **Tokens**: Stored using `androidx.security.security-crypto` EncryptedSharedPreferences
- **PKCE**: S256 method (SHA-256) for Authorization Code flow
- **State parameter**: CSRF protection for Authorization Code flow
- **Client credentials**: Stored encrypted, transmitted via Basic Auth header (Base64 encoded)
- **Deep links**: Validated in `AuthorizationCodeActivity.onNewIntent()`
- **Biometric**: Android Credential Manager API with device-level biometric authentication

**Signing config**: Production builds read signing credentials from `keystore.properties` (gitignored). See `keystore.properties.example` for the template.

## Testing

Current test coverage (109 tests, 13 files):
- `PKCEUtilTest` (8) - RFC 7636 compliance
- `ExtensionsTest` (6) - Helper functions
- `SettingsRepositoryTest` (13) - Settings model defaults and validation
- `SettingsRepositoryCrudTest` (10) - Persistence CRUD, token expiry
- `OpenIdConfigurationTest` (2) - Discovery model
- `ServerErrorTest` (6) - Error model methods
- `AuthRepositoryTest` (24) - All OAuth operations, caching, error handling
- `AuthInterceptorTest` (5) - Bearer token injection
- `OriginInterceptorTest` (3) - Origin header
- `LoggingInterceptorTest` (4) - Pass-through behavior
- `AuthorizationCodeServiceTest` (10) - Callback handling, CSRF, PKCE
- `CibaServiceTest` (10) - Flow states, polling, network recovery
- `WebAuthnServiceTest` (8) - JSON building/parsing

Tests use JUnit 4, MockK, MockWebServer, and kotlinx-coroutines-test. All tests pass with 0 warnings.

## Development Notes

### Adding New OAuth Endpoints
1. Add endpoint method to `network/api/AuthApi.kt` with `@Url` parameter
2. Add repository function in `data/repositories/AuthRepository.kt`
3. Update service layer to call repository
4. Add corresponding settings fields if needed (client ID, secret, URLs)

### Logging
Network activity is logged via:
- `LoggingInterceptor` (custom logger, visible in app's LogsActivity)
- OkHttp's `HttpLoggingInterceptor` (visible in Logcat with tag "OkHttp")

Logger supports truncation with `maxBodyLength` parameter (-1 for unlimited).

### Deep Link Testing
```bash
# Test OAuth callback deep link
adb shell am start -a android.intent.action.VIEW -d "com.masstack.authn://oauth/callback?code=test&state=test"
```

### Common Issues
- **Hilt compilation errors**: Run `./gradlew clean build`
- **Dependency resolution**: Use `./gradlew --refresh-dependencies`
- **Deep links not working**: Check AndroidManifest.xml intent filter and launchMode
- **WebAuthn failures**: Ensure device has biometric authentication configured
- **Custom Tabs not opening**: Verify browser is installed on device/emulator

## Dependencies

Key libraries:
- `com.google.dagger:hilt-android:2.48` - Dependency injection
- `com.squareup.retrofit2:retrofit:2.9.0` - HTTP client
- `androidx.credentials:credentials:1.3.0-alpha01` - WebAuthn support
- `androidx.browser:browser:1.7.0` - Custom Tabs for Authorization Code flow
- `androidx.security:security-crypto:1.1.0-alpha06` - Encrypted storage
- `io.mockk:mockk:1.13.8` - Testing

Compose BOM: `androidx.compose:compose-bom:2023.10.01`
Kotlin: 1.9.20 | Gradle: 8.2.0 | Min SDK: 26 | Target SDK: 34

## Additional Documentation

- `QUICK_START.md` - Step-by-step setup and first-run instructions
- `ANDROID_STUDIO_GUIDE.md` - Comprehensive guide for building, signing, and publishing
- `UNIT_TESTS.md` - Detailed test documentation
- `LOGGING_GUIDE.md` - Network logging configuration
