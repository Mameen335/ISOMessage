package com.mameen.isomessage.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * JSON request body sent to the Mockoon simulator backend.
 * Mirrors the ISO8583 logical structure but transported via HTTP/JSON.
 *
 * This is the DTO (Data Transfer Object) used by Retrofit.
 * It is intentionally separate from the domain [IsoMessage] model.
 */
data class PaymentRequest(
    @SerializedName("mti") val mti: String,
    @SerializedName("fields") val fields: Map<String, String>,
    @SerializedName("meta") val meta: RequestMeta? = null
)

data class RequestMeta(
    @SerializedName("simulateTimeout") val simulateTimeout: Boolean = false,
    @SerializedName("simulateOffline") val simulateOffline: Boolean = false,
    @SerializedName("delayMs") val delayMs: Int = 0,
    @SerializedName("clientTimestamp") val clientTimestamp: String = ""
)
