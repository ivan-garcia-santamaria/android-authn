package com.masstack.authn.data.models

import com.google.gson.annotations.SerializedName

/**
 * Response from WebAuthn registration/authentication completion endpoints
 */
data class WebAuthnVerificationResponse(
    @SerializedName("verified")
    val verified: Boolean
)
