package com.masstack.authn.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.masstack.authn.TestFixtures
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthorizationCodeServiceTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var service: AuthorizationCodeService

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        context = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        every { settingsRepository.getSettings() } returns TestFixtures.settings()

        service = AuthorizationCodeService(context, settingsRepository, authRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkAll()
    }

    @Test
    fun `checkServerAvailability should delegate to authRepository`() = runTest {
        coEvery { authRepository.checkServerAvailability() } returns Result.success(Unit)

        val result = service.checkServerAvailability()

        assertTrue(result.isSuccess)
        coVerify { authRepository.checkServerAvailability() }
    }

    @Test
    fun `handleCallback should return failure when error parameter present`() = runTest {
        val uri = mockk<Uri>()
        every { uri.getQueryParameter("error") } returns "access_denied"
        every { uri.getQueryParameter("error_description") } returns "User denied"
        every { uri.getQueryParameter("code") } returns null
        every { uri.getQueryParameter("state") } returns null
        every { uri.toString() } returns "com.masstack.authn://callback?error=access_denied"

        val result = service.handleCallback(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("access_denied") == true)
    }

    @Test
    fun `handleCallback should return failure when code is missing`() = runTest {
        val uri = mockk<Uri>()
        every { uri.getQueryParameter("error") } returns null
        every { uri.getQueryParameter("error_description") } returns null
        every { uri.getQueryParameter("code") } returns null
        every { uri.getQueryParameter("state") } returns null
        every { uri.toString() } returns "com.masstack.authn://callback"

        val result = service.handleCallback(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `handleCallback should return failure when code is blank`() = runTest {
        val uri = mockk<Uri>()
        every { uri.getQueryParameter("error") } returns null
        every { uri.getQueryParameter("error_description") } returns null
        every { uri.getQueryParameter("code") } returns ""
        every { uri.getQueryParameter("state") } returns null
        every { uri.toString() } returns "com.masstack.authn://callback?code="

        val result = service.handleCallback(uri)

        assertTrue(result.isFailure)
    }

    @Test
    fun `handleCallback should return failure when state does not match`() = runTest {
        val uri = mockk<Uri>()
        every { uri.getQueryParameter("error") } returns null
        every { uri.getQueryParameter("error_description") } returns null
        every { uri.getQueryParameter("code") } returns "auth-code"
        every { uri.getQueryParameter("state") } returns "wrong-state"
        every { uri.toString() } returns "com.masstack.authn://callback?code=auth-code&state=wrong-state"

        val result = service.handleCallback(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("State mismatch") == true)
    }

    @Test
    fun `handleCallback should return failure when PKCE verifier is null`() = runTest {
        val uri = mockk<Uri>()
        every { uri.getQueryParameter("error") } returns null
        every { uri.getQueryParameter("error_description") } returns null
        every { uri.getQueryParameter("code") } returns "auth-code"
        every { uri.getQueryParameter("state") } returns null
        every { uri.toString() } returns "com.masstack.authn://callback?code=auth-code"

        val result = service.handleCallback(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Code verifier not found") == true)
    }

    @Test
    fun `startAuthorizationFlow should throw when config is not cached`() {
        every { authRepository.getCachedConfig() } returns null

        assertThrows(IllegalStateException::class.java) {
            service.startAuthorizationFlow()
        }
    }

    @Test
    fun `startAuthorizationFlow should throw when auth code not configured`() {
        every { authRepository.getCachedConfig() } returns TestFixtures.openIdConfiguration()
        every { settingsRepository.getSettings() } returns TestFixtures.settings(authCodeClientId = "")

        assertThrows(IllegalStateException::class.java) {
            service.startAuthorizationFlow()
        }
    }

    @Test
    fun `clearFlow should reset state so handleCallback fails with missing verifier`() = runTest {
        service.clearFlow()

        val uri = mockk<Uri>()
        every { uri.getQueryParameter("error") } returns null
        every { uri.getQueryParameter("error_description") } returns null
        every { uri.getQueryParameter("code") } returns "auth-code"
        every { uri.getQueryParameter("state") } returns null
        every { uri.toString() } returns "com.masstack.authn://callback?code=auth-code"

        val result = service.handleCallback(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Code verifier") == true)
    }

    @Test
    fun `logout should throw when config is not cached`() {
        every { authRepository.getCachedConfig() } returns null

        assertThrows(IllegalStateException::class.java) {
            service.logout()
        }
    }
}
