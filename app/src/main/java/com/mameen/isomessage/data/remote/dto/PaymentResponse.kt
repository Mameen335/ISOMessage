package com.mameen.isomessage.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * JSON response body from the Mockoon simulator backend.
 * Carries the ISO8583 response fields plus simulator metadata.
 */
data class PaymentResponse(
    @SerializedName("mti") val mti: String,
    @SerializedName("fields") val fields: Map<String, String> = emptyMap(),
    @SerializedName("error") val error: ResponseError? = null,
    @SerializedName("log") val log: ResponseLog? = null,
    @SerializedName("balance") val balance: BalanceData? = null
)

data class ResponseError(
    @SerializedName("code") val code: String,
    @SerializedName("detail") val detail: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("retryable") val retryable: Boolean = false
)

data class ResponseLog(
    @SerializedName("level") val level: String,
    @SerializedName("event") val event: String,
    @SerializedName("mti_request") val mtiRequest: String?,
    @SerializedName("mti_response") val mtiResponse: String?,
    @SerializedName("rc") val responseCode: String?,
    @SerializedName("latency_ms") val latencyMs: Int?,
    @SerializedName("terminal") val terminal: String?,
    @SerializedName("stan") val stan: String?,
    @SerializedName("ts") val timestamp: String?
)

data class BalanceData(
    @SerializedName("available") val available: String,
    @SerializedName("ledger") val ledger: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("formatted") val formatted: String
)

/** Wrapper for settlement totals in a reconciliation response */
data class SettlementTotals(
    @SerializedName("purchases_count") val purchasesCount: Int,
    @SerializedName("purchases_amount") val purchasesAmount: String,
    @SerializedName("refunds_count") val refundsCount: Int,
    @SerializedName("refunds_amount") val refundsAmount: String,
    @SerializedName("net_amount") val netAmount: String,
    @SerializedName("currency") val currency: String
)
