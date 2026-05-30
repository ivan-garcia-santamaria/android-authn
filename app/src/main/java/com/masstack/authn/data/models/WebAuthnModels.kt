package com.masstack.authn.data.models

import com.google.gson.annotations.SerializedName

/**
 * WebAuthn Registration Challenge Request
 */
data class WebAuthnRegistrationRequest(
    @SerializedName("username")
    val username: String
)

/**
 * WebAuthn Registration Challenge Response
 */
data class WebAuthnRegistrationResponse(
    @SerializedName("challenge")
    val challenge: String,

    @SerializedName("rp")
    val rp: RelyingParty,

    @SerializedName("user")
    val user: User,

    @SerializedName("pubKeyCredParams")
    val pubKeyCredParams: List<PubKeyCredParam>,

    @SerializedName("timeout")
    val timeout: Long? = null,

    @SerializedName("authenticatorSelection")
    val authenticatorSelection: AuthenticatorSelection? = null
)

/**
 * WebAuthn Authentication Challenge Request
 */
data class WebAuthnAuthenticationRequest(
    @SerializedName("username")
    val username: String
)

/**
 * WebAuthn Authentication Challenge Response
 */
data class WebAuthnAuthenticationResponse(
    @SerializedName("challenge")
    val challenge: String,

    @SerializedName("timeout")
    val timeout: Long? = null,

    @SerializedName("rpId")
    val rpId: String,

    @SerializedName("allowCredentials")
    val allowCredentials: List<AllowCredential>? = null
)

/**
 * Relying Party information
 */
data class RelyingParty(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String
)

/**
 * User information for WebAuthn
 */
data class User(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("displayName")
    val displayName: String
)

/**
 * Public Key Credential Parameters
 */
data class PubKeyCredParam(
    @SerializedName("type")
    val type: String,

    @SerializedName("alg")
    val alg: Int
)

/**
 * Authenticator Selection Criteria
 */
data class AuthenticatorSelection(
    @SerializedName("authenticatorAttachment")
    val authenticatorAttachment: String? = null,

    @SerializedName("requireResidentKey")
    val requireResidentKey: Boolean = false,

    @SerializedName("userVerification")
    val userVerification: String = "preferred"
)

/**
 * Allowed Credentials for authentication
 */
data class AllowCredential(
    @SerializedName("type")
    val type: String,

    @SerializedName("id")
    val id: String
)

/**
 * WebAuthn Registration Verification Request
 * Matches server's FinishRegistrationRequest format
 */
data class WebAuthnRegistrationVerification(
    @SerializedName("username")
    val username: String,

    @SerializedName("response")
    val response: CredentialCreationResponse
)

/**
 * Credential Creation Response
 * Contains the full credential response from the authenticator
 */
data class CredentialCreationResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("rawId")
    val rawId: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("response")
    val response: AuthenticatorAttestationResponse
)

/**
 * Authenticator Attestation Response
 */
data class AuthenticatorAttestationResponse(
    @SerializedName("clientDataJSON")
    val clientDataJSON: String,

    @SerializedName("attestationObject")
    val attestationObject: String
)

/**
 * WebAuthn Authentication Verification Request
 * Matches server's FinishAuthenticationRequest format
 */
data class WebAuthnAuthenticationVerification(
    @SerializedName("username")
    val username: String,

    @SerializedName("response")
    val response: CredentialAssertionResponse
)

/**
 * Credential Assertion Response
 * Contains the full credential response from the authenticator
 */
data class CredentialAssertionResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("rawId")
    val rawId: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("response")
    val response: AuthenticatorAssertionResponse
)

/**
 * Authenticator Assertion Response
 */
data class AuthenticatorAssertionResponse(
    @SerializedName("clientDataJSON")
    val clientDataJSON: String,

    @SerializedName("authenticatorData")
    val authenticatorData: String,

    @SerializedName("signature")
    val signature: String,

    @SerializedName("userHandle")
    val userHandle: String? = null
)
