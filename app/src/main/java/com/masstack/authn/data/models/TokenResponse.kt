package com.masstack.authn.data.models

import com.google.gson.annotations.SerializedName

/**
 * Response from OAuth token endpoint
 */
data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("expires_in")
    val expiresIn: Int,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("scope")
    val scope: String? = null,

    @SerializedName("id_token")
    val idToken: String? = null
)
