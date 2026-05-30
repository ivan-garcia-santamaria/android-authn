package com.masstack.authn.data.models

import com.google.gson.annotations.SerializedName

data class OpenIdConfiguration(
    @SerializedName("issuer")
    val issuer: String? = null,

    @SerializedName("authorization_endpoint")
    val authorizationEndpoint: String? = null,

    @SerializedName("token_endpoint")
    val tokenEndpoint: String? = null,

    @SerializedName("end_session_endpoint")
    val endSessionEndpoint: String? = null,

    @SerializedName("backchannel_authentication_endpoint")
    val backchannelAuthenticationEndpoint: String? = null,

    @SerializedName("webauthn_registration_options_endpoint")
    val webauthnRegistrationOptionsEndpoint: String? = null,

    @SerializedName("webauthn_registration_verification_endpoint")
    val webauthnRegistrationVerificationEndpoint: String? = null,

    @SerializedName("webauthn_authentication_options_endpoint")
    val webauthnAuthenticationOptionsEndpoint: String? = null,

    @SerializedName("webauthn_authentication_verification_endpoint")
    val webauthnAuthenticationVerificationEndpoint: String? = null
)
