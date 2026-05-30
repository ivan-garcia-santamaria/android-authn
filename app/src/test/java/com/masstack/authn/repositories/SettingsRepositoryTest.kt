package com.masstack.authn.repositories

import android.content.Context
import android.content.SharedPreferences
import com.masstack.authn.data.models.Settings
import com.masstack.authn.data.repositories.SettingsRepository
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Settings should have correct default values`() {
        val settings = Settings()

        assertEquals("openid profile api:everything", settings.scope)
        assertEquals("", settings.username)
        assertEquals("https://authn.sta.masstack.com/v1/.well-known/openid-configuration", settings.openidConfigurationUrl)
        assertTrue(settings.enableServerDownScreen)
        assertEquals("SNAKE", settings.serverDownGame)

        assertEquals("", settings.authCodeClientId)
        assertEquals("", settings.authCodeClientSecret)
        assertEquals("com.masstack.authn://oauth/callback", settings.authCodeRedirectUri)

        assertEquals("", settings.cibaClientId)
        assertEquals("", settings.cibaClientSecret)
        assertEquals(5, settings.cibaPollingInterval)

        assertEquals("", settings.webauthnClientId)
        assertEquals("", settings.webauthnClientSecret)
    }

    @Test
    fun `isAuthCodeConfigured should return true when clientId and redirectUri are present`() {
        val settings = Settings(
            authCodeClientId = "client123",
            authCodeRedirectUri = "com.masstack.authn://oauth/callback"
        )

        assertTrue(settings.isAuthCodeConfigured())
    }

    @Test
    fun `isAuthCodeConfigured should return false when clientId is blank`() {
        val settings = Settings(
            authCodeClientId = "",
            authCodeRedirectUri = "com.masstack.authn://oauth/callback"
        )

        assertFalse(settings.isAuthCodeConfigured())
    }

    @Test
    fun `isAuthCodeConfigured should return false when redirectUri is blank`() {
        val settings = Settings(
            authCodeClientId = "client123",
            authCodeRedirectUri = ""
        )

        assertFalse(settings.isAuthCodeConfigured())
    }

    @Test
    fun `isCibaConfigured should return true when all required fields are present`() {
        val settings = Settings(
            username = "testuser",
            cibaClientId = "ciba-client-123",
            cibaClientSecret = "ciba-secret"
        )

        assertTrue(settings.isCibaConfigured())
    }

    @Test
    fun `isCibaConfigured should return false when username is blank`() {
        val settings = Settings(
            username = "",
            cibaClientId = "ciba-client-123",
            cibaClientSecret = "ciba-secret"
        )

        assertFalse(settings.isCibaConfigured())
    }

    @Test
    fun `isCibaConfigured should return false when CIBA clientId is blank`() {
        val settings = Settings(
            username = "testuser",
            cibaClientId = "",
            cibaClientSecret = "ciba-secret"
        )

        assertFalse(settings.isCibaConfigured())
    }

    @Test
    fun `isCibaConfigured should return false when CIBA clientSecret is blank`() {
        val settings = Settings(
            username = "testuser",
            cibaClientId = "ciba-client-123",
            cibaClientSecret = ""
        )

        assertFalse(settings.isCibaConfigured())
    }

    @Test
    fun `isWebAuthnConfigured should return true when clientId and username are present`() {
        val settings = Settings(
            username = "testuser",
            webauthnClientId = "webauthn-client-123"
        )

        assertTrue(settings.isWebAuthnConfigured())
    }

    @Test
    fun `isWebAuthnConfigured should return false when username is blank`() {
        val settings = Settings(
            username = "",
            webauthnClientId = "webauthn-client-123"
        )

        assertFalse(settings.isWebAuthnConfigured())
    }

    @Test
    fun `isWebAuthnConfigured should return false when WebAuthn clientId is blank`() {
        val settings = Settings(
            username = "testuser",
            webauthnClientId = ""
        )

        assertFalse(settings.isWebAuthnConfigured())
    }

    @Test
    fun `Settings copy should work correctly`() {
        val original = Settings(
            scope = "openid",
            username = "testuser",
            authCodeClientId = "client123"
        )

        val copy = original.copy(authCodeClientId = "newclient")

        assertEquals("openid", copy.scope)
        assertEquals("testuser", copy.username)
        assertEquals("newclient", copy.authCodeClientId)
    }

    @Test
    fun `Settings should preserve openidConfigurationUrl`() {
        val customUrl = "https://custom.server.com/.well-known/openid-configuration"
        val settings = Settings(openidConfigurationUrl = customUrl)

        assertEquals(customUrl, settings.openidConfigurationUrl)
    }
}
