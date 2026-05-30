package com.masstack.authn

import com.masstack.authn.data.models.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

object TestFixtures {

    fun settings(
        scope: String = "openid profile api:everything",
        username: String = "testuser",
        openidConfigurationUrl: String = "https://authn.example.com/.well-known/openid-configuration",
        authCodeClientId: String = "auth-code-client",
        authCodeClientSecret: String = "auth-code-secret",
        authCodeRedirectUri: String = "com.masstack.authn://oauth/callback",
        cibaClientId: String = "ciba-client",
        cibaClientSecret: String = "ciba-secret",
        cibaPollingInterval: Int = 5,
        webauthnClientId: String = "webauthn-client",
        webauthnClientSecret: String = "webauthn-secret"
    ) = Settings(
        scope = scope,
        username = username,
        openidConfigurationUrl = openidConfigurationUrl,
        authCodeClientId = authCodeClientId,
        authCodeClientSecret = authCodeClientSecret,
        authCodeRedirectUri = authCodeRedirectUri,
        cibaClientId = cibaClientId,
        cibaClientSecret = cibaClientSecret,
        cibaPollingInterval = cibaPollingInterval,
        webauthnClientId = webauthnClientId,
        webauthnClientSecret = webauthnClientSecret
    )

    fun openIdConfiguration(
        issuer: String = "https://authn.example.com",
        authorizationEndpoint: String = "https://authn.example.com/oauth/authorize",
        tokenEndpoint: String = "https://authn.example.com/oauth/token",
        endSessionEndpoint: String = "https://authn.example.com/oauth/logout",
        backchannelAuthenticationEndpoint: String = "https://authn.example.com/bc-authorize",
        webauthnRegistrationOptionsEndpoint: String = "https://authn.example.com/webauthn/registration/options",
        webauthnRegistrationVerificationEndpoint: String = "https://authn.example.com/webauthn/registration/verification",
        webauthnAuthenticationOptionsEndpoint: String = "https://authn.example.com/webauthn/authentication/options",
        webauthnAuthenticationVerificationEndpoint: String = "https://authn.example.com/webauthn/authentication/verification"
    ) = OpenIdConfiguration(
        issuer = issuer,
        authorizationEndpoint = authorizationEndpoint,
        tokenEndpoint = tokenEndpoint,
        endSessionEndpoint = endSessionEndpoint,
        backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint,
        webauthnRegistrationOptionsEndpoint = webauthnRegistrationOptionsEndpoint,
        webauthnRegistrationVerificationEndpoint = webauthnRegistrationVerificationEndpoint,
        webauthnAuthenticationOptionsEndpoint = webauthnAuthenticationOptionsEndpoint,
        webauthnAuthenticationVerificationEndpoint = webauthnAuthenticationVerificationEndpoint
    )

    fun tokenResponse(
        accessToken: String = "test-access-token",
        expiresIn: Int = 3600,
        refreshToken: String? = "test-refresh-token",
        tokenType: String = "Bearer",
        scope: String? = "openid profile",
        idToken: String? = null
    ) = TokenResponse(
        accessToken = accessToken,
        expiresIn = expiresIn,
        refreshToken = refreshToken,
        tokenType = tokenType,
        scope = scope,
        idToken = idToken
    )

    fun cibaResponse(
        authReqId: String = "test-auth-req-id",
        expiresIn: Int = 120,
        interval: Int = 5
    ) = CibaResponse(
        authReqId = authReqId,
        expiresIn = expiresIn,
        interval = interval
    )

    fun <T> successResponse(body: T): Response<T> = Response.success(body)

    fun <T> errorResponse(code: Int, body: String = """{"error":"test_error"}"""): Response<T> {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return Response.error(code, responseBody)
    }
}
