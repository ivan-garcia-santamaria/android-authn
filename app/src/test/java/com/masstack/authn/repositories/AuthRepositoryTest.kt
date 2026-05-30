package com.masstack.authn.repositories

import android.util.Base64
import android.util.Log
import com.masstack.authn.TestFixtures
import com.masstack.authn.data.models.*
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.network.api.AuthApi
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var authApi: AuthApi
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }

        authApi = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.getSettings() } returns TestFixtures.settings()

        authRepository = AuthRepository(authApi, settingsRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(Base64::class)
        unmockkAll()
    }

    // --- fetchOpenIdConfiguration ---

    @Test
    fun `fetchOpenIdConfiguration should return success when response is successful`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)

        val result = authRepository.fetchOpenIdConfiguration("https://example.com/.well-known/openid-configuration")

        assertTrue(result.isSuccess)
        assertEquals("https://authn.example.com", result.getOrNull()?.issuer)
    }

    @Test
    fun `fetchOpenIdConfiguration should return failure when response is not successful`() = runTest {
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.errorResponse(400, "Bad Request")

        val result = authRepository.fetchOpenIdConfiguration("https://example.com/.well-known/openid-configuration")

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchOpenIdConfiguration should return failure with ServerError for 5xx response`() = runTest {
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.errorResponse(500, "Internal Server Error")

        val result = authRepository.fetchOpenIdConfiguration("https://example.com/.well-known/openid-configuration")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerError)
    }

    @Test
    fun `fetchOpenIdConfiguration should return failure on network exception`() = runTest {
        coEvery { authApi.fetchOpenIdConfiguration(any()) } throws java.io.IOException("Unable to resolve host")

        val result = authRepository.fetchOpenIdConfiguration("https://example.com/.well-known/openid-configuration")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is java.io.IOException)
    }

    // --- checkServerAvailability ---

    @Test
    fun `checkServerAvailability should return success and cache config when 200`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)

        assertNull(authRepository.getCachedConfig())

        val result = authRepository.checkServerAvailability()

        assertTrue(result.isSuccess)
        assertNotNull(authRepository.getCachedConfig())
        assertEquals("https://authn.example.com", authRepository.getCachedConfig()?.issuer)
    }

    @Test
    fun `checkServerAvailability should return failure with ServerError for non-200`() = runTest {
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.errorResponse(503, "Service Unavailable")

        val result = authRepository.checkServerAvailability()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerError)
    }

    @Test
    fun `checkServerAvailability should return failure on network exception`() = runTest {
        coEvery { authApi.fetchOpenIdConfiguration(any()) } throws java.io.IOException("Network error")

        val result = authRepository.checkServerAvailability()

        assertTrue(result.isFailure)
    }

    @Test
    fun `checkServerAvailability should use openidConfigurationUrl from settings`() = runTest {
        val customUrl = "https://custom.server.com/.well-known/openid-configuration"
        every { settingsRepository.getSettings() } returns TestFixtures.settings(openidConfigurationUrl = customUrl)
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(TestFixtures.openIdConfiguration())

        authRepository.checkServerAvailability()

        coVerify { authApi.fetchOpenIdConfiguration(customUrl) }
    }

    // --- exchangeCodeForToken ---

    @Test
    fun `exchangeCodeForToken should return success and save tokens`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        val tokenResponse = TestFixtures.tokenResponse()
        coEvery { authApi.exchangeCodeForToken(any(), any(), any(), any(), any(), any(), any()) } returns TestFixtures.successResponse(tokenResponse)

        val result = authRepository.exchangeCodeForToken("auth-code", "code-verifier")

        assertTrue(result.isSuccess)
        assertEquals("test-access-token", result.getOrNull()?.accessToken)
        verify { settingsRepository.saveAccessToken("test-access-token", 3600) }
        verify { settingsRepository.saveRefreshToken("test-refresh-token") }
    }

    @Test
    fun `exchangeCodeForToken should pass clientSecret only when non-blank`() = runTest {
        every { settingsRepository.getSettings() } returns TestFixtures.settings(authCodeClientSecret = "")
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.exchangeCodeForToken(any(), any(), any(), any(), any(), any(), any()) } returns TestFixtures.successResponse(TestFixtures.tokenResponse())

        authRepository.exchangeCodeForToken("code", "verifier")

        coVerify {
            authApi.exchangeCodeForToken(
                url = any(),
                grantType = any(),
                code = any(),
                clientId = any(),
                clientSecret = null,
                redirectUri = any(),
                codeVerifier = any()
            )
        }
    }

    @Test
    fun `exchangeCodeForToken should return failure on error response`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.exchangeCodeForToken(any(), any(), any(), any(), any(), any(), any()) } returns TestFixtures.errorResponse(400, """{"error":"invalid_grant"}""")

        val result = authRepository.exchangeCodeForToken("bad-code", "verifier")

        assertTrue(result.isFailure)
    }

    // --- refreshToken ---

    @Test
    fun `refreshToken should return success and save new tokens`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        val tokenResponse = TestFixtures.tokenResponse(accessToken = "new-token", refreshToken = "new-refresh")
        coEvery { authApi.refreshToken(any(), any(), any(), any()) } returns TestFixtures.successResponse(tokenResponse)

        val result = authRepository.refreshToken("old-refresh-token")

        assertTrue(result.isSuccess)
        verify { settingsRepository.saveAccessToken("new-token", 3600) }
        verify { settingsRepository.saveRefreshToken("new-refresh") }
    }

    @Test
    fun `refreshToken should return failure on error response`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.refreshToken(any(), any(), any(), any()) } returns TestFixtures.errorResponse(401, """{"error":"invalid_grant"}""")

        val result = authRepository.refreshToken("expired-token")

        assertTrue(result.isFailure)
    }

    @Test
    fun `refreshToken should use Basic auth header`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.refreshToken(any(), any(), any(), any()) } returns TestFixtures.successResponse(TestFixtures.tokenResponse())

        authRepository.refreshToken("refresh-token")

        coVerify { authApi.refreshToken(any(), any(), any(), match { it.startsWith("Basic ") }) }
    }

    // --- cibaAuthorize ---

    @Test
    fun `cibaAuthorize should return success with CibaResponse`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        val cibaResponse = TestFixtures.cibaResponse()
        coEvery { authApi.cibaAuthorize(any(), any(), any(), any(), any()) } returns TestFixtures.successResponse(cibaResponse)

        val result = authRepository.cibaAuthorize("testuser")

        assertTrue(result.isSuccess)
        assertEquals("test-auth-req-id", result.getOrNull()?.authReqId)
    }

    @Test
    fun `cibaAuthorize should return failure on error`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.cibaAuthorize(any(), any(), any(), any(), any()) } returns TestFixtures.errorResponse(400, """{"error":"invalid_request"}""")

        val result = authRepository.cibaAuthorize("testuser")

        assertTrue(result.isFailure)
    }

    // --- cibaToken ---

    @Test
    fun `cibaToken should return success and save tokens`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        val tokenResponse = TestFixtures.tokenResponse()
        coEvery { authApi.cibaToken(any(), any(), any(), any()) } returns TestFixtures.successResponse(tokenResponse)

        val result = authRepository.cibaToken("auth-req-id")

        assertTrue(result.isSuccess)
        verify { settingsRepository.saveAccessToken("test-access-token", 3600) }
    }

    @Test
    fun `cibaToken should return failure on error`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.cibaToken(any(), any(), any(), any()) } returns TestFixtures.errorResponse(400, """{"error":"authorization_pending"}""")

        val result = authRepository.cibaToken("auth-req-id")

        assertTrue(result.isFailure)
    }

    // --- getCachedConfig ---

    @Test
    fun `getCachedConfig should return null initially`() {
        assertNull(authRepository.getCachedConfig())
    }

    @Test
    fun `getCachedConfig should return config after successful checkServerAvailability`() = runTest {
        val config = TestFixtures.openIdConfiguration(issuer = "https://my-server.com")
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)

        authRepository.checkServerAvailability()

        assertEquals("https://my-server.com", authRepository.getCachedConfig()?.issuer)
    }

    // --- WebAuthn ---

    @Test
    fun `webAuthnRegistrationBegin should return success on successful response`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        val registrationResponse = WebAuthnRegistrationResponse(
            challenge = "test-challenge",
            rp = RelyingParty("example.com", "Example"),
            user = User("user-id", "testuser", "Test User"),
            pubKeyCredParams = listOf(PubKeyCredParam("public-key", -7))
        )
        coEvery { authApi.webAuthnRegistrationBegin(any(), any(), any()) } returns TestFixtures.successResponse(registrationResponse)

        val result = authRepository.webAuthnRegistrationBegin("testuser")

        assertTrue(result.isSuccess)
        assertEquals("test-challenge", result.getOrNull()?.challenge)
    }

    @Test
    fun `webAuthnRegistrationBegin should return failure on error response`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.webAuthnRegistrationBegin(any(), any(), any()) } returns TestFixtures.errorResponse(400)

        val result = authRepository.webAuthnRegistrationBegin("testuser")

        assertTrue(result.isFailure)
    }

    @Test
    fun `webAuthnAuthenticationComplete should save tokens on success`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        val tokenResponse = TestFixtures.tokenResponse()
        coEvery { authApi.webAuthnAuthenticationComplete(any(), any(), any()) } returns TestFixtures.successResponse(tokenResponse)

        val verification = WebAuthnAuthenticationVerification(
            username = "testuser",
            response = CredentialAssertionResponse(
                id = "cred-id", rawId = "raw-id", type = "public-key",
                response = AuthenticatorAssertionResponse(
                    clientDataJSON = "data", authenticatorData = "auth-data", signature = "sig"
                )
            )
        )
        val result = authRepository.webAuthnAuthenticationComplete(verification)

        assertTrue(result.isSuccess)
        verify { settingsRepository.saveAccessToken("test-access-token", 3600) }
    }

    @Test
    fun `webAuthnAuthenticationComplete should return failure on error`() = runTest {
        val config = TestFixtures.openIdConfiguration()
        coEvery { authApi.fetchOpenIdConfiguration(any()) } returns TestFixtures.successResponse(config)
        authRepository.checkServerAvailability()

        coEvery { authApi.webAuthnAuthenticationComplete(any(), any(), any()) } returns TestFixtures.errorResponse(401)

        val verification = WebAuthnAuthenticationVerification(
            username = "testuser",
            response = CredentialAssertionResponse(
                id = "cred-id", rawId = "raw-id", type = "public-key",
                response = AuthenticatorAssertionResponse(
                    clientDataJSON = "data", authenticatorData = "auth-data", signature = "sig"
                )
            )
        )
        val result = authRepository.webAuthnAuthenticationComplete(verification)

        assertTrue(result.isFailure)
    }
}
