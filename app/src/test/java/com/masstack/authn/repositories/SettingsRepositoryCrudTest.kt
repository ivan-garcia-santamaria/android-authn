package com.masstack.authn.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.utils.Constants
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsRepositoryCrudTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var securePreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var secureEditor: SharedPreferences.Editor
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        secureEditor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } just Runs

        every { securePreferences.edit() } returns secureEditor
        every { secureEditor.putString(any(), any()) } returns secureEditor
        every { secureEditor.putLong(any(), any()) } returns secureEditor
        every { secureEditor.remove(any()) } returns secureEditor
        every { secureEditor.clear() } returns secureEditor
        every { secureEditor.apply() } just Runs

        mockkStatic(EncryptedSharedPreferences::class)
        mockkConstructor(MasterKey.Builder::class)
        every { anyConstructed<MasterKey.Builder>().setKeyScheme(any()) } returns mockk(relaxed = true) {
            every { build() } returns mockk(relaxed = true)
        }
        every { EncryptedSharedPreferences.create(any<Context>(), any(), any(), any(), any()) } returns securePreferences

        settingsRepository = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getSettings should return defaults when preferences are empty`() {
        every { sharedPreferences.getString(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getBoolean(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getInt(any(), any()) } answers { secondArg() }
        every { securePreferences.getString(any(), any()) } answers { secondArg() }

        val settings = settingsRepository.getSettings()

        assertEquals("openid profile api:everything", settings.scope)
        assertEquals("", settings.username)
        assertTrue(settings.enableServerDownScreen)
        assertEquals(5, settings.cibaPollingInterval)
    }

    @Test
    fun `getSettings should read values from sharedPreferences and securePreferences`() {
        every { sharedPreferences.getString(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getBoolean(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getInt(any(), any()) } answers { secondArg() }
        every { securePreferences.getString(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getString(Constants.KEY_SCOPE, any()) } returns "custom-scope"
        every { sharedPreferences.getString(Constants.KEY_USERNAME, any()) } returns "john"
        every { sharedPreferences.getString(Constants.KEY_AUTH_CODE_CLIENT_ID, any()) } returns "my-client"
        every { securePreferences.getString(Constants.KEY_AUTH_CODE_CLIENT_SECRET, any()) } returns "my-secret"

        val settings = settingsRepository.getSettings()

        assertEquals("custom-scope", settings.scope)
        assertEquals("john", settings.username)
        assertEquals("my-client", settings.authCodeClientId)
        assertEquals("my-secret", settings.authCodeClientSecret)
    }

    @Test
    fun `saveSettings should write non-secret fields to sharedPreferences`() {
        val settings = com.masstack.authn.data.models.Settings(
            scope = "openid",
            username = "testuser",
            authCodeClientId = "client-123",
            cibaPollingInterval = 10
        )

        settingsRepository.saveSettings(settings)

        verify { editor.putString(Constants.KEY_SCOPE, "openid") }
        verify { editor.putString(Constants.KEY_USERNAME, "testuser") }
        verify { editor.putString(Constants.KEY_AUTH_CODE_CLIENT_ID, "client-123") }
        verify { editor.putInt(Constants.KEY_CIBA_POLLING_INTERVAL, 10) }
    }

    @Test
    fun `saveSettings should write secret fields to securePreferences`() {
        val settings = com.masstack.authn.data.models.Settings(
            authCodeClientSecret = "auth-secret",
            cibaClientSecret = "ciba-secret",
            webauthnClientSecret = "webauthn-secret"
        )

        settingsRepository.saveSettings(settings)

        verify { secureEditor.putString(Constants.KEY_AUTH_CODE_CLIENT_SECRET, "auth-secret") }
        verify { secureEditor.putString(Constants.KEY_CIBA_CLIENT_SECRET, "ciba-secret") }
        verify { secureEditor.putString(Constants.KEY_WEBAUTHN_CLIENT_SECRET, "webauthn-secret") }
    }

    @Test
    fun `saveAccessToken should store token and calculated expiry`() {
        settingsRepository.saveAccessToken("my-token", 3600)

        verify { secureEditor.putString(Constants.KEY_ACCESS_TOKEN, "my-token") }
        verify { secureEditor.putLong(Constants.KEY_TOKEN_EXPIRY, match { it > System.currentTimeMillis() }) }
    }

    @Test
    fun `saveRefreshToken should store non-null token`() {
        settingsRepository.saveRefreshToken("refresh-123")

        verify { secureEditor.putString(Constants.KEY_REFRESH_TOKEN, "refresh-123") }
    }

    @Test
    fun `saveRefreshToken should remove token when null`() {
        settingsRepository.saveRefreshToken(null)

        verify { secureEditor.remove(Constants.KEY_REFRESH_TOKEN) }
    }

    @Test
    fun `getAccessToken should return token when not expired`() {
        every { securePreferences.getString(Constants.KEY_ACCESS_TOKEN, null) } returns "valid-token"
        every { securePreferences.getLong(Constants.KEY_TOKEN_EXPIRY, 0) } returns System.currentTimeMillis() + 3600_000

        val token = settingsRepository.getAccessToken()

        assertEquals("valid-token", token)
    }

    @Test
    fun `getAccessToken should return null when expired`() {
        every { securePreferences.getString(Constants.KEY_ACCESS_TOKEN, null) } returns "expired-token"
        every { securePreferences.getLong(Constants.KEY_TOKEN_EXPIRY, 0) } returns System.currentTimeMillis() - 1000

        val token = settingsRepository.getAccessToken()

        assertNull(token)
    }

    @Test
    fun `clearTokens should remove all token keys`() {
        settingsRepository.clearTokens()

        verify { secureEditor.remove(Constants.KEY_ACCESS_TOKEN) }
        verify { secureEditor.remove(Constants.KEY_REFRESH_TOKEN) }
        verify { secureEditor.remove(Constants.KEY_TOKEN_EXPIRY) }
    }
}
