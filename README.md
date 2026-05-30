# Android Authn

Android app for testing OAuth2 flows against the MasStack Authn server.

Implements three authentication flows:
- **Authorization Code** with PKCE (RFC 7636)
- **WebAuthn** — biometric authentication via Android Credential Manager
- **CIBA** — Client Initiated Backchannel Authentication with polling

Built with Kotlin, Jetpack Compose, and MVVM architecture using Hilt for dependency injection.

## Requirements

- Android 8.0+ (SDK 26) — WebAuthn requires Android 9+
- Kotlin 1.9.20
- Gradle 8.2.0
- Java 17

## Quick Start

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug

# Run tests
./gradlew testDebugUnitTest
```

On first launch, open Settings and configure at least one flow with your Authn server credentials.

## Configuration

Endpoint URLs are auto-discovered from the server's `.well-known/openid-configuration` document. Each flow only requires client credentials.

### Global Settings
- **OpenID Configuration URL** — enter the discovery URL and tap "Discover Endpoints" to auto-populate all endpoint URLs
- OAuth scope (default: `openid profile api:everything`)
- Username (shared across flows)
- Server Down screen toggle with game selection (Snake, Tetris, Space Invaders)

### Authorization Code
- Client ID, Client Secret (optional)
- Redirect URI: `com.masstack.authn://oauth/callback` (registered as deep link)

### CIBA
- Client ID, Client Secret
- Configurable polling interval

### WebAuthn
- Client ID, Client Secret (optional)

## Project Structure

```
app/src/main/java/com/masstack/authn/
├── di/                  # Hilt modules (AppModule, NetworkModule)
├── data/
│   ├── models/          # TokenResponse, CibaResponse, WebAuthnModels, Settings, OpenIdConfiguration, errors
│   └── repositories/    # AuthRepository (OAuth ops), SettingsRepository (config + tokens)
├── network/
│   ├── api/             # AuthApi — Retrofit interface with dynamic @Url endpoints
│   └── interceptors/    # Auth (Bearer), Logging (in-app), Origin (WebAuthn CORS)
├── services/            # AuthorizationCodeService, CibaService, WebAuthnService
├── ui/
│   ├── MainActivity.kt              # Flow selection
│   ├── SettingsActivity.kt           # Per-flow configuration
│   ├── LogsActivity.kt              # Network debug log viewer
│   ├── ServerDownActivity.kt        # Server error screen with mini-games
│   └── flows/
│       ├── AuthorizationCodeActivity.kt  # PKCE flow with Custom Tabs
│       ├── WebAuthnActivity.kt           # Biometric registration & auth
│       └── CibaActivity.kt              # Backchannel polling flow
└── utils/               # PKCEUtil, Logger, NetworkUtils, Constants, Extensions
```

## OAuth Flows

### Authorization Code + PKCE
1. Generates PKCE pair (code_verifier + S256 code_challenge) and random state
2. Opens Custom Tab with authorization URL
3. Captures callback via deep link at `com.masstack.authn://oauth/callback`
4. Exchanges authorization code + code_verifier for tokens
5. Supports logout via Custom Tab redirect

### CIBA
1. Sends backchannel authorization request with login_hint and optional binding message
2. Receives `auth_req_id`, expiry, and polling interval
3. Polls token endpoint handling `authorization_pending`, `slow_down`, `expired_token`
4. Implements exponential backoff with network connectivity checks
5. UI shows real-time polling state and progress

### WebAuthn
**Registration**: Calls `/webauthn/register/begin` to get challenge, creates credential via Android Credential Manager, posts to `/webauthn/register/complete` for verification.

**Authentication**: Calls `/webauthn/authenticate/begin` to get challenge + allowCredentials, authenticates via Credential Manager, posts assertion to `/webauthn/authenticate/complete` to receive tokens.

## Security

- Tokens and secrets stored with `EncryptedSharedPreferences`
- PKCE with S256 method (SHA-256) for Authorization Code flow
- State parameter for CSRF protection
- Client credentials transmitted via Base64-encoded Basic Auth header
- Deep link validation on callback
- Android Credential Manager for biometric operations

## Signing

Release builds read signing config from `keystore.properties` (gitignored). Copy the example to get started:

```bash
cp keystore.properties.example keystore.properties
# Edit with your keystore path and credentials
```

Then build the release APK:

```bash
./gradlew assembleRelease
```

## Tests

109 unit tests across 13 test files:

- **PKCEUtilTest** (8) — RFC 7636 compliance
- **ExtensionsTest** (6) — URL encoding, query string parsing
- **SettingsRepositoryTest** (13) — Settings defaults, isConfigured validation, copy semantics
- **SettingsRepositoryCrudTest** (10) — Persistence CRUD, token expiry, clearTokens
- **OpenIdConfigurationTest** (2) — Discovery model nullable defaults
- **ServerErrorTest** (6) — is503, isServerError, exception model
- **AuthRepositoryTest** (24) — All OAuth operations: token exchange, refresh, CIBA, WebAuthn, caching
- **AuthInterceptorTest** (5) — Bearer token injection, header preservation
- **OriginInterceptorTest** (3) — Android apk-key-hash Origin header
- **LoggingInterceptorTest** (4) — Pass-through, no-body handling
- **AuthorizationCodeServiceTest** (10) — Callback handling, CSRF, PKCE validation
- **CibaServiceTest** (10) — Flow states, polling, error handling, network recovery
- **WebAuthnServiceTest** (8) — JSON building/parsing for registration and authentication

```bash
./gradlew testDebugUnitTest
open app/build/reports/tests/testDebugUnitTest/index.html
```

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose | BOM 2023.10.01 | UI framework |
| Hilt | 2.48 | Dependency injection |
| Retrofit | 2.9.0 | HTTP client |
| OkHttp | 4.12.0 | HTTP + logging interceptor |
| Credential Manager | 1.3.0-alpha01 | WebAuthn support |
| Security Crypto | 1.1.0-alpha06 | Encrypted storage |
| Browser | 1.7.0 | Custom Tabs |
| MockK | 1.13.8 | Test mocking |

## Additional Docs

- [QUICK_START.md](./QUICK_START.md) — Setup and first-run instructions
- [ANDROID_STUDIO_GUIDE.md](./ANDROID_STUDIO_GUIDE.md) — Building, signing, and publishing
- [UNIT_TESTS.md](./UNIT_TESTS.md) — Detailed test documentation
- [LOGGING_GUIDE.md](./LOGGING_GUIDE.md) — Network logging configuration
- [VERSION_MANAGEMENT.md](./VERSION_MANAGEMENT.md) — Versioning strategy
- [CHANGELOG.md](./CHANGELOG.md) — Release history
