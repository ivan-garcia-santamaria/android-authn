# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-05-30

### Added
- **OpenID Configuration auto-discovery** — New field in Global Settings for the `.well-known/openid-configuration` URL with a "Discover Endpoints" button that auto-populates all endpoint URLs for every flow
- New `OpenIdConfiguration` data model with `applyTo()` mapping method
- New `OpenIdConfigurationTest` with 4 unit tests
- Server availability pre-check before login in WebAuthn and CIBA flows (Authorization Code already had it)
- `fetchOpenIdConfiguration()` endpoint in AuthApi and AuthRepository

### Changed
- Settings UI simplified: removed 10 individual endpoint URL fields, kept client credentials and flow-specific settings (redirect URI, polling interval)
- Discovered endpoints shown as a read-only summary card after successful discovery
- Server availability check now uses GET on the OpenID Configuration endpoint instead of HEAD on the authorize URL
- Server error handling shows ServerDownActivity for any 5xx error, not just 503
- `parseError()` in AuthRepository now throws `ServerError` for all 5xx responses
- Settings description updated from "Show game and Goku on 503 errors" to "Show game and Goku on server errors"
- ServerDownActivity shows "Server Unavailable" for connection errors without HTTP code

### Fixed
- WebAuthn registration complete URL default corrected from `.../registration/verify` to `.../registration/verification`

## [1.0.9] - 2024-11-15

### Added
- **Configurable Server Down Screen** - Added setting to toggle between Server Down screen and raw error messages
- New toggle in Settings screen: "Server Down Screen" - Controls whether 503 errors show game/Goku screen or raw error message
- Setting is enabled by default (shows game screen)

### Technical
- Added `enableServerDownScreen: Boolean = true` field to `Settings` data class
- Added `KEY_ENABLE_SERVER_DOWN_SCREEN` constant to `Constants.kt`
- Updated `SettingsRepository` to persist and load the new setting
- Updated all flow activities (AuthorizationCode, CIBA, WebAuthn) to check setting before navigating to ServerDownActivity
- When setting is disabled, activities show raw error message instead of navigating to game screen

### User Experience
- Users can now toggle between fun game screen and raw error message
- Useful for comparing behavior before and after v1.0.7
- Allows testing and verification of load balancer error messages
- Setting persists across app restarts

### Settings UI
- Added Switch component in Settings screen under "Global Settings" section
- Switch shows descriptive text: "Show game and Goku on 503 errors"
- Located after username field, before OAuth flow configuration sections

### Context
This allows users to compare the new fun Server Down screen (with Snake game and Goku) introduced in v1.0.7 versus the old behavior that showed the raw "no healthy upstream" error message from the GCP load balancer. Useful for debugging and verifying error handling behavior.

## [1.0.8] - 2024-11-15

### Added
- **Comprehensive logging for all OAuth flows** - Added detailed debug logs throughout the entire application
- Activity lifecycle logging for AuthorizationCodeActivity and WebAuthnActivity
- Logs for all critical OAuth flow operations and state changes

### Logging Coverage
- **AuthorizationCodeService**: Already had comprehensive logging (flow start, PKCE generation, state generation, Custom Tab launch, callback handling, token exchange, logout)
- **WebAuthnService**: Already had comprehensive logging using Logger utility (registration/authentication begin, credential creation/retrieval, server communication)
- **CibaService**: Already had comprehensive logging (added in v1.0.4)
- **AuthorizationCodeActivity**: NEW - Added lifecycle logs (onCreate, onResume, onPause), intent handling, callback processing, auto-start detection, logout handling, error tracking
- **WebAuthnActivity**: NEW - Added WebAuthn availability checks, registration/authentication flow logs, biometric prompt tracking, error handling
- **CibaActivity**: Already had comprehensive logging (added in v1.0.4)

### Debug Features
All logs use Android Log with appropriate levels:
- `Log.d()` for debug information (flow progress, state changes)
- `Log.i()` for important milestones (flow start/end, success events)
- `Log.w()` for warnings (network issues, user cancellations)
- `Log.e()` for errors (failures, exceptions)

### How to Debug
Filter logs by tags in ADB:
```bash
# Authorization Code flow
adb logcat | grep -E "(AuthCodeService|AuthCodeActivity)"

# WebAuthn flow
adb logcat | grep "WebAuthn"

# CIBA flow
adb logcat | grep -E "(CibaService|CibaActivity)"

# All OAuth flows
adb logcat | grep -E "(AuthCodeService|AuthCodeActivity|WebAuthn|CibaService|CibaActivity)"

# Network layer
adb logcat | grep "AuthRepository"
```

### Technical
- Added `TAG` constants to all activities for consistent log filtering
- Added success indicators (✅) and error indicators (❌) for easy visual scanning
- Added section markers (===) for flow start/end to easily identify sessions
- All critical operations now have before/after logging
- Exception stack traces included in error logs

### Context
Previously, only CibaService and WebAuthnService had comprehensive logging. This update adds detailed logging to AuthorizationCodeActivity and WebAuthnActivity, making it possible to debug the complete OAuth flow from UI to network layer. All three OAuth flows (Authorization Code + PKCE, WebAuthn, and CIBA) now have full end-to-end logging coverage.

## [1.0.7] - 2024-11-15

### Added
- **503 Service Unavailable detection** - App now detects when OAuth server is down (HTTP 503 or "no healthy upstream")
- **ServerDownActivity** - Fun error screen with Snake game and Son Goku ASCII art while server is unavailable
- Interactive Snake game to play while waiting for server to come back online
- Son Goku ASCII art with motivational quote
- Automatic detection of GCP load balancer "no healthy upstream" error message

### Technical
- Created `ServerError` data class to wrap HTTP errors with status codes
- Updated `AuthRepository.parseError()` to detect and throw `ServerError` for 503 responses
- Updated all flow activities (AuthorizationCode, CIBA, WebAuthn) to catch `ServerError`
- When 503 is detected, user is navigated to `ServerDownActivity` instead of showing generic error
- Updated `CibaService.CibaState.Error` to include optional exception for error propagation
- Registered `ServerDownActivity` in AndroidManifest

### User Experience
- Users now see a friendly game screen instead of cryptic error message when server is down
- Can play Snake game with swipe controls while waiting
- Visual feedback with Son Goku ASCII art and server status information
- Easy retry button to check if server is back online
- Back button to return to main menu

### Context
When the OAuth server in GCP goes down, the load balancer returns HTTP 503 with plain text "no healthy upstream". Previously, this showed as a generic error. Now users get a fun, engaging screen with a mini-game to pass the time.

## [1.0.6] - 2024-11-15

### Fixed
- Fixed blank screen when navigating back from AuthorizationCodeActivity to MainActivity
- MainActivity now always calls `setContent()` before auto-starting flows to ensure UI is rendered

### Technical
- Moved `setContent()` call before auto-start logic in `MainActivity.onCreate()`
- Previously, when auto-start was triggered, `setContent()` was skipped with early return
- Now MainActivity UI is always rendered, preventing blank screen on back navigation

### Context
When the user had Authorization Code flow configured and the app auto-started the flow on launch, pressing the back button from AuthorizationCodeActivity would return to a blank MainActivity because `setContent()` was never called. This is now fixed.

## [1.0.5] - 2024-11-15

### Fixed
- **CRITICAL**: Fixed CIBA polling DNS resolution failures when network connectivity is lost during backgrounding
- App now detects network unavailability before each poll attempt and waits for network to return
- When DNS resolution fails mid-request, app now checks network status and retries instead of failing
- Network errors no longer count against maximum polling attempts - retries continue until network returns or auth expires

### Added
- **Network connectivity checking** - Added `NetworkUtils.kt` utility with network availability detection
- Pre-poll network connectivity check before each CIBA token request
- Intelligent retry mechanism for transient network errors with backoff delays
- Enhanced logging showing network type and connectivity status during polling
- Network status logging when DNS failures occur

### Technical
- `CibaService.startCibaFlow()` now accepts `Context` parameter for network checking
- Added `NetworkUtils.isNetworkAvailable()` using NetworkCapabilities API (Android M+)
- Added `NetworkUtils.getNetworkType()` to identify WiFi, Cellular, Ethernet, VPN connections
- DNS resolution failures now trigger network check and automatic retry instead of immediate failure
- Failed polling attempts due to network issues are retried without incrementing attempt counter
- 2-5 second delays between network check retries to allow network to stabilize

### Context
Previously, when users switched to email/authenticator apps during CIBA flow, the device could lose and regain network connectivity (WiFi reconnection, etc.), causing "Unable to resolve host" errors. The app would fail immediately without retrying. Now the app detects network issues proactively and waits for connectivity to return before continuing polling.

## [1.0.4] - 2024-11-10

### Added
- **Comprehensive logging for CIBA flow** - Added detailed debug logs throughout the entire CIBA authentication process
- Activity lifecycle logging (onCreate, onPause, onResume, onStop, onStart, onDestroy)
- Network error detection with specific handling for DNS resolution failures
- Exception type and full stack trace logging

### Debug Features
- `CibaService` now logs:
  - Flow start/end markers
  - Backchannel authorization requests and responses
  - Each polling attempt with attempt number
  - Poll results (success/failure)
  - Token details when obtained
  - Detailed error information including exception types
  - Specific detection of "Unable to resolve host" errors
- `CibaActivity` now logs:
  - All lifecycle events (onCreate, onPause, onResume, etc.)
  - Polling job status at each lifecycle event
  - Current state transitions
  - Coroutine start and exception details

### Technical
- All logs use `Log.d()`, `Log.i()`, `Log.w()`, and `Log.e()` with tag filtering
- Filter logs with: `adb logcat | grep -E "(CibaService|CibaActivity)"`
- WebAuthn logging maintained as before

## [1.0.3] - 2024-11-07

### Fixed
- **CRITICAL**: Fixed CIBA polling failure when app loses focus (backgrounding)
- CIBA flow now continues polling even when user switches to email/other apps
- Network operations now use `Dispatchers.IO` to survive lifecycle changes
- Added proper error handling for interrupted polling

### Technical
- Changed CIBA polling from default dispatcher to `Dispatchers.IO`
- Added `pollingJob` reference to track and cancel ongoing polls
- Added try-catch block to handle cancellation gracefully
- Added `onDestroy()` cleanup to cancel polling job

### Context
Previously, when users initiated CIBA flow and switched to their email to approve the request, the polling coroutine would be cancelled or interrupted, causing "cannot access host" errors when returning to the app. This is now fixed.

## [1.0.2] - 2024-11-07

### Fixed
- Fixed adaptive icon cropping issue - logo now displays completely without being cut off
- Added 20% padding inset to icon foreground layer for proper safe zone compliance
- Icon now appears properly on all Android versions and launcher shapes

### Technical
- Created `ic_launcher_foreground.xml` with proper inset layer-list
- Adaptive icons now reference the padded foreground drawable

## [1.0.1] - 2024-11-07

### Changed
- Updated app icon and launcher icons with new Authn logo (lock with geometric polygonal design)
- Updated splash screen logo to match new branding
- Replaced all mipmap icon densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- Updated adaptive icon background color to match logo theme (#0D1821)

### Fixed
- Properly converted JPG logo to PNG format for all icon sizes
- Fixed icon compilation errors in AAPT2

## [1.0.0] - 2024-11-07

### Added
- Centralized version management via `version.properties` file
- Version display in main screen title (e.g., "OAuth2 Flow Tester v1.0.0")
- `VERSION_MANAGEMENT.md` documentation file
- `CHANGELOG.md` for tracking version history

### Fixed
- App icon now displays correctly in installed APK
- Updated adaptive icon configuration to use `authn_logo.png` from drawable resources
- Fixed both `ic_launcher.xml` and `ic_launcher_round.xml` to reference correct logo asset

### Changed
- App icon configuration now references `@drawable/authn_logo` instead of generic mipmap resources
- BuildConfig generation enabled in `app/build.gradle`
- `versionName` in build.gradle now reads from `version.properties`

### Technical Details
- Added `buildConfig true` to build features
- Created `buildConfigField` for `APP_VERSION` accessible via `BuildConfig.APP_VERSION`
- Updated `MainActivity.kt` to import and display `BuildConfig.APP_VERSION`
- Version format: MAJOR.MINOR.PATCH (Semantic Versioning)

---

## Version Update Guidelines

When making changes:
1. Update `version.properties` with new version number
2. Add entry to this CHANGELOG.md under new version header
3. Rebuild project: `./gradlew clean assembleDebug`
4. Update `VERSION_MANAGEMENT.md` history table if needed
