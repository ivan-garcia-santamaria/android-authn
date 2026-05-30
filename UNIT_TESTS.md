# Unit Tests - Android OAuth2 Testing App

## Summary

**13 test files** with a total of **109 unit tests**.

---

## Tests by Category

### Utilities

**PKCEUtilTest.kt** (8 tests) — `app/src/test/java/com/masstack/authn/utils/PKCEUtilTest.kt`
RFC 7636 PKCE implementation: code_verifier length/format, code_challenge SHA-256, pair generation and uniqueness.

**ExtensionsTest.kt** (6 tests) — `app/src/test/java/com/masstack/authn/utils/ExtensionsTest.kt`
URL encoding, query string building, query parameter parsing, edge cases (empty map, malformed params).

---

### Data Models

**SettingsRepositoryTest.kt** (13 tests) — `app/src/test/java/com/masstack/authn/repositories/SettingsRepositoryTest.kt`
Settings model defaults, `isAuthCodeConfigured()`, `isCibaConfigured()`, `isWebAuthnConfigured()` for all true/false paths, copy semantics, openidConfigurationUrl.

**OpenIdConfigurationTest.kt** (2 tests) — `app/src/test/java/com/masstack/authn/models/OpenIdConfigurationTest.kt`
Nullable defaults for all 9 endpoint fields, value storage when provided.

**ServerErrorTest.kt** (6 tests) — `app/src/test/java/com/masstack/authn/models/ServerErrorTest.kt`
`is503ServiceUnavailable()` for code 503, "no healthy upstream" body, other codes. `isServerError()` for 5xx/4xx. Exception properties.

---

### Repositories

**AuthRepositoryTest.kt** (24 tests) — `app/src/test/java/com/masstack/authn/repositories/AuthRepositoryTest.kt`
All OAuth operations with mocked AuthApi:
- `fetchOpenIdConfiguration`: success, error, 5xx ServerError, network exception
- `checkServerAvailability`: success + cache, non-200, network exception, URL from settings
- `exchangeCodeForToken`: success + save tokens, blank clientSecret, error
- `refreshToken`: success + save, error, Basic auth header
- `cibaAuthorize`/`cibaToken`: success/error paths
- `getCachedConfig`: null initial, post-cache
- WebAuthn: registrationBegin, authenticationComplete success/error + token saving

**SettingsRepositoryCrudTest.kt** (10 tests) — `app/src/test/java/com/masstack/authn/repositories/SettingsRepositoryCrudTest.kt`
`getSettings` defaults and value reading, `saveSettings` to shared/secure prefs, `saveAccessToken` with expiry, `saveRefreshToken` null/non-null, `getAccessToken` expired/valid, `clearTokens`.

---

### Network Interceptors

**AuthInterceptorTest.kt** (5 tests) — `app/src/test/java/com/masstack/authn/network/interceptors/AuthInterceptorTest.kt`
Bearer token injection when available, skip when null/blank, preserve existing Authorization header, provider called per-request.

**OriginInterceptorTest.kt** (3 tests) — `app/src/test/java/com/masstack/authn/network/interceptors/OriginInterceptorTest.kt`
Adds `android:apk-key-hash:` Origin header, overrides existing Origin.

**LoggingInterceptorTest.kt** (4 tests) — `app/src/test/java/com/masstack/authn/network/interceptors/LoggingInterceptorTest.kt`
Pass-through without modifying request/response, handles no-body GET, handles error response codes.

---

### Services

**AuthorizationCodeServiceTest.kt** (10 tests) — `app/src/test/java/com/masstack/authn/services/AuthorizationCodeServiceTest.kt`
`handleCallback`: error in URI, missing/blank code, state mismatch (CSRF), null PKCE verifier. `startAuthorizationFlow`/`logout`: throws when config not cached, throws when not configured. `clearFlow` resets state. `checkServerAvailability` delegates to repository.

**CibaServiceTest.kt** (10 tests) — `app/src/test/java/com/masstack/authn/services/CibaServiceTest.kt`
Flow state machine: Idle→Error on auth failure, Idle→Authorizing→Polling→Success, authorization_pending continues polling, expired_token/access_denied/unknown errors stop, max attempts timeout, network unavailable skip, Authorizing state data verification, ServerError code propagation.

**WebAuthnServiceTest.kt** (8 tests) — `app/src/test/java/com/masstack/authn/services/WebAuthnServiceTest.kt`
Registration request JSON: required fields, default authenticatorSelection. Authentication request JSON: required fields, allowCredentials present/absent. Registration response parsing: credential field extraction. Authentication response parsing: assertion fields, null userHandle.

---

## Test Dependencies

```gradle
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
testImplementation 'io.mockk:mockk:1.13.8'
testImplementation 'io.mockk:mockk-android:1.13.8'
testImplementation 'androidx.arch.core:core-testing:2.2.0'
testImplementation 'org.robolectric:robolectric:4.11.1'
testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
```

---

## Running Tests

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# All tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew test --tests "*.AuthRepositoryTest"

# View report
open app/build/reports/tests/testDebugUnitTest/index.html
```

---

## Coverage Summary

| Category | Files | Tests |
|----------|-------|-------|
| Utilities | 2 | 14 |
| Data Models | 3 | 21 |
| Repositories | 2 | 34 |
| Interceptors | 3 | 12 |
| Services | 3 | 28 |
| **TOTAL** | **13** | **109** |

---

## Not Unit Testable

These require instrumented tests (`androidTest`) on a real device or emulator:

- `WebAuthnService.registerCredential/authenticateCredential` — Android Credential Manager API
- `AuthorizationCodeService.startAuthorizationFlow/logout` — Custom Tabs browser launch
- `NetworkUtils.isNetworkAvailable` — ConnectivityManager API
- All UI Activities — Compose UI rendering
