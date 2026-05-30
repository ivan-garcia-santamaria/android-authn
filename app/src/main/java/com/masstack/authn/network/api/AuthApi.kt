package com.masstack.authn.network.api

import com.masstack.authn.data.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for OAuth2 endpoints
 */
interface AuthApi {

    @GET
    suspend fun fetchOpenIdConfiguration(
        @Url url: String
    ): Response<OpenIdConfiguration>

    /**
     * Exchange authorization code for tokens
     * Uses dynamic URL to support custom token endpoint
     */
    @FormUrlEncoded
    @POST
    suspend fun exchangeCodeForToken(
        @Url url: String,
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String? = null,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String? = null
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST
    suspend fun refreshToken(
        @Url url: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Header("Authorization") authorization: String
    ): Response<TokenResponse>

    /**
     * CIBA - Backchannel Authorization
     * Uses dynamic URL to support custom backchannel endpoint
     */
    @FormUrlEncoded
    @POST
    suspend fun cibaAuthorize(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Field("login_hint") loginHint: String,
        @Field("scope") scope: String? = null,
        @Field("binding_message") bindingMessage: String? = null
    ): Response<CibaResponse>

    /**
     * CIBA - Poll for token
     * Uses dynamic URL to support custom token endpoint
     */
    @FormUrlEncoded
    @POST
    suspend fun cibaToken(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String,
        @Field("auth_req_id") authReqId: String
    ): Response<TokenResponse>

    /**
     * WebAuthn - Registration Challenge
     * Uses dynamic URL to support custom registration endpoint
     */
    @POST
    suspend fun webAuthnRegistrationBegin(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: WebAuthnRegistrationRequest
    ): Response<WebAuthnRegistrationResponse>

    /**
     * WebAuthn - Complete Registration
     * Uses dynamic URL to support custom registration endpoint
     */
    @POST
    suspend fun webAuthnRegistrationComplete(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body verification: WebAuthnRegistrationVerification
    ): Response<WebAuthnVerificationResponse>

    /**
     * WebAuthn - Authentication Challenge
     * Uses dynamic URL to support custom authentication endpoint
     */
    @POST
    suspend fun webAuthnAuthenticationBegin(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: WebAuthnAuthenticationRequest
    ): Response<WebAuthnAuthenticationResponse>

    /**
     * WebAuthn - Complete Authentication
     * Uses dynamic URL to support custom authentication endpoint
     */
    @POST
    suspend fun webAuthnAuthenticationComplete(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body verification: WebAuthnAuthenticationVerification
    ): Response<TokenResponse>
}
