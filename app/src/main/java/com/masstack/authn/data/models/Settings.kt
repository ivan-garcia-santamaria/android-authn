package com.masstack.authn.data.models

data class Settings(
    // Global settings
    val scope: String = "openid profile api:everything",
    val username: String = "",
    val openidConfigurationUrl: String = "https://authn.sta.masstack.com/v1/.well-known/openid-configuration",
    val enableServerDownScreen: Boolean = true,
    val serverDownGame: String = "SNAKE",

    // Authorization Code Flow configuration
    val authCodeClientId: String = "",
    val authCodeClientSecret: String = "",
    val authCodeRedirectUri: String = "com.masstack.authn://oauth/callback",

    // CIBA Flow configuration
    val cibaClientId: String = "",
    val cibaClientSecret: String = "",
    val cibaPollingInterval: Int = 5,

    // WebAuthn Flow configuration
    val webauthnClientId: String = "",
    val webauthnClientSecret: String = ""
) {
    fun isAuthCodeConfigured(): Boolean {
        return authCodeClientId.isNotBlank() &&
               authCodeRedirectUri.isNotBlank()
    }

    fun isCibaConfigured(): Boolean {
        return cibaClientId.isNotBlank() &&
               cibaClientSecret.isNotBlank() &&
               username.isNotBlank()
    }

    fun isWebAuthnConfigured(): Boolean {
        return webauthnClientId.isNotBlank() &&
               username.isNotBlank()
    }
}
