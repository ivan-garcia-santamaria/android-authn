package com.masstack.authn.data.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.masstack.authn.data.models.Settings
import com.masstack.authn.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    }

    private val securePreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            Constants.PREF_SECURE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getSettings(): Settings {
        return Settings(
            scope = sharedPreferences.getString(Constants.KEY_SCOPE, "openid profile api:everything") ?: "openid profile api:everything",
            username = sharedPreferences.getString(Constants.KEY_USERNAME, "") ?: "",
            openidConfigurationUrl = sharedPreferences.getString(Constants.KEY_OPENID_CONFIGURATION_URL, "https://authn.sta.masstack.com/v1/.well-known/openid-configuration") ?: "https://authn.sta.masstack.com/v1/.well-known/openid-configuration",
            enableServerDownScreen = sharedPreferences.getBoolean(Constants.KEY_ENABLE_SERVER_DOWN_SCREEN, true),
            serverDownGame = sharedPreferences.getString(Constants.KEY_SERVER_DOWN_GAME, "SNAKE") ?: "SNAKE",
            authCodeClientId = sharedPreferences.getString(Constants.KEY_AUTH_CODE_CLIENT_ID, "") ?: "",
            authCodeClientSecret = securePreferences.getString(Constants.KEY_AUTH_CODE_CLIENT_SECRET, "") ?: "",
            authCodeRedirectUri = sharedPreferences.getString(Constants.KEY_AUTH_CODE_REDIRECT_URI, "com.masstack.authn://oauth/callback") ?: "com.masstack.authn://oauth/callback",
            cibaClientId = sharedPreferences.getString(Constants.KEY_CIBA_CLIENT_ID, "") ?: "",
            cibaClientSecret = securePreferences.getString(Constants.KEY_CIBA_CLIENT_SECRET, "") ?: "",
            cibaPollingInterval = sharedPreferences.getInt(Constants.KEY_CIBA_POLLING_INTERVAL, 5),
            webauthnClientId = sharedPreferences.getString(Constants.KEY_WEBAUTHN_CLIENT_ID, "") ?: "",
            webauthnClientSecret = securePreferences.getString(Constants.KEY_WEBAUTHN_CLIENT_SECRET, "") ?: ""
        )
    }

    fun saveSettings(settings: Settings) {
        sharedPreferences.edit().apply {
            putString(Constants.KEY_SCOPE, settings.scope)
            putString(Constants.KEY_USERNAME, settings.username)
            putString(Constants.KEY_OPENID_CONFIGURATION_URL, settings.openidConfigurationUrl)
            putBoolean(Constants.KEY_ENABLE_SERVER_DOWN_SCREEN, settings.enableServerDownScreen)
            putString(Constants.KEY_SERVER_DOWN_GAME, settings.serverDownGame)
            putString(Constants.KEY_AUTH_CODE_CLIENT_ID, settings.authCodeClientId)
            putString(Constants.KEY_AUTH_CODE_REDIRECT_URI, settings.authCodeRedirectUri)
            putString(Constants.KEY_CIBA_CLIENT_ID, settings.cibaClientId)
            putInt(Constants.KEY_CIBA_POLLING_INTERVAL, settings.cibaPollingInterval)
            putString(Constants.KEY_WEBAUTHN_CLIENT_ID, settings.webauthnClientId)
            apply()
        }

        securePreferences.edit().apply {
            putString(Constants.KEY_AUTH_CODE_CLIENT_SECRET, settings.authCodeClientSecret)
            putString(Constants.KEY_CIBA_CLIENT_SECRET, settings.cibaClientSecret)
            putString(Constants.KEY_WEBAUTHN_CLIENT_SECRET, settings.webauthnClientSecret)
            apply()
        }
    }

    fun saveAccessToken(token: String, expiresIn: Int) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
        securePreferences.edit().apply {
            putString(Constants.KEY_ACCESS_TOKEN, token)
            putLong(Constants.KEY_TOKEN_EXPIRY, expiryTime)
            apply()
        }
    }

    fun saveRefreshToken(token: String?) {
        securePreferences.edit().apply {
            if (token != null) {
                putString(Constants.KEY_REFRESH_TOKEN, token)
            } else {
                remove(Constants.KEY_REFRESH_TOKEN)
            }
            apply()
        }
    }

    fun getAccessToken(): String? {
        val token = securePreferences.getString(Constants.KEY_ACCESS_TOKEN, null)
        val expiry = securePreferences.getLong(Constants.KEY_TOKEN_EXPIRY, 0)
        if (System.currentTimeMillis() > expiry) {
            return null
        }
        return token
    }

    fun getRefreshToken(): String? {
        return securePreferences.getString(Constants.KEY_REFRESH_TOKEN, null)
    }

    fun clearTokens() {
        securePreferences.edit().apply {
            remove(Constants.KEY_ACCESS_TOKEN)
            remove(Constants.KEY_REFRESH_TOKEN)
            remove(Constants.KEY_TOKEN_EXPIRY)
            apply()
        }
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        securePreferences.edit().clear().apply()
    }
}
