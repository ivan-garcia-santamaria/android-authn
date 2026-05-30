package com.masstack.authn.utils

object Constants {
    // OAuth Flow Types
    const val FLOW_AUTHORIZATION_CODE = "authorization_code"
    const val FLOW_WEBAUTHN = "webauthn"
    const val FLOW_CIBA = "urn:openid:params:grant-type:ciba"

    // OAuth Parameters
    const val RESPONSE_TYPE_CODE = "code"
    const val CODE_CHALLENGE_METHOD_S256 = "S256"
    const val TOKEN_TYPE_BEARER = "Bearer"

    // Redirect URI for Authorization Code flow
    const val REDIRECT_URI = "com.masstack.authn://oauth/callback"

    // SharedPreferences keys
    const val PREF_NAME = "authn_prefs"
    const val PREF_SECURE_NAME = "authn_secure_prefs"

    // Global settings keys
    const val KEY_SCOPE = "scope"
    const val KEY_USERNAME = "username"
    const val KEY_ENABLE_SERVER_DOWN_SCREEN = "enable_server_down_screen"
    const val KEY_SERVER_DOWN_GAME = "server_down_game"
    const val KEY_OPENID_CONFIGURATION_URL = "openid_configuration_url"

    // Authorization Code Flow configuration keys
    const val KEY_AUTH_CODE_CLIENT_ID = "auth_code_client_id"
    const val KEY_AUTH_CODE_CLIENT_SECRET = "auth_code_client_secret"
    const val KEY_AUTH_CODE_REDIRECT_URI = "auth_code_redirect_uri"

    // CIBA Flow configuration keys
    const val KEY_CIBA_CLIENT_ID = "ciba_client_id"
    const val KEY_CIBA_CLIENT_SECRET = "ciba_client_secret"
    const val KEY_CIBA_POLLING_INTERVAL = "ciba_polling_interval"

    // WebAuthn Flow configuration keys
    const val KEY_WEBAUTHN_CLIENT_ID = "webauthn_client_id"
    const val KEY_WEBAUTHN_CLIENT_SECRET = "webauthn_client_secret"

    // Token keys
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_TOKEN_EXPIRY = "token_expiry"

    // PKCE
    const val PKCE_CODE_VERIFIER_LENGTH = 128

    // CIBA Polling
    const val CIBA_DEFAULT_INTERVAL = 5000L // 5 seconds
    const val CIBA_MAX_ATTEMPTS = 120 // 10 minutes max

    // WebAuthn
    const val WEBAUTHN_TIMEOUT = 60000L // 60 seconds

    // Network
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // OAuth Error Codes
    const val ERROR_AUTHORIZATION_PENDING = "authorization_pending"
    const val ERROR_SLOW_DOWN = "slow_down"
    const val ERROR_EXPIRED_TOKEN = "expired_token"
    const val ERROR_ACCESS_DENIED = "access_denied"
    const val ERROR_INVALID_GRANT = "invalid_grant"
}
