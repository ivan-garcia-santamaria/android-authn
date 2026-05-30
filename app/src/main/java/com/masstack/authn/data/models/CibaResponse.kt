package com.masstack.authn.data.models

import com.google.gson.annotations.SerializedName

/**
 * Response from CIBA backchannel authorization endpoint
 */
data class CibaResponse(
    @SerializedName("auth_req_id")
    val authReqId: String,

    @SerializedName("expires_in")
    val expiresIn: Int,

    @SerializedName("interval")
    val interval: Int
)
