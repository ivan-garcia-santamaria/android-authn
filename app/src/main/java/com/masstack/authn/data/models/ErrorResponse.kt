package com.masstack.authn.data.models

import com.google.gson.annotations.SerializedName

/**
 * OAuth error response
 */
data class ErrorResponse(
    @SerializedName("error")
    val error: String,

    @SerializedName("error_description")
    val errorDescription: String? = null
)
