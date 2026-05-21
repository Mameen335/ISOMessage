package com.mameen.isomessage.data.remote

import com.mameen.isomessage.data.remote.dto.PaymentRequest
import com.mameen.isomessage.data.remote.dto.PaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit API Service — defines the HTTP endpoints exposed by the Mockoon simulator.
 *
 * All endpoints mirror the routes defined in ISO8583_POS_Payment_Host.json.
 *
 * Base URL: http://10.0.2.2:5000/ (Android emulator localhost)
 *           http://localhost:5000/ (physical device with Mockoon on same machine)
 *
 * The [X-Simulate-Timeout] header triggers a 5-second delay + timeout response from Mockoon.
 */
interface PaymentApiService {

    /**
     * ISO8583 Financial Transaction — covers Purchase, Refund, Balance Inquiry.
     * MTI 0200 → responds with MTI 0210.
     * Approval logic in Mockoon: amount ≤ 5000 cents → approved (00),
     * amount > 5000 cents → insufficient funds (51).
     */
    @POST("iso")
    suspend fun sendIsoTransaction(
        @Body request: PaymentRequest,
        @Header("X-Simulate-Timeout") simulateTimeout: Boolean = false
    ): Response<PaymentResponse>

    /**
     * Reversal request.
     * MTI 0400 → responds with MTI 0410.
     */
    @POST("iso/reversal")
    suspend fun sendReversal(
        @Body request: PaymentRequest
    ): Response<PaymentResponse>

    /**
     * Settlement / Reconciliation request.
     * MTI 0500 → responds with MTI 0510.
     */
    @POST("iso/settlement")
    suspend fun sendSettlement(
        @Body request: PaymentRequest
    ): Response<PaymentResponse>

    /**
     * Network echo / health check.
     * MTI 0800 → responds with MTI 0810.
     * Used to verify the connection to the host is alive before transactions.
     */
    @POST("iso/echo")
    suspend fun sendEcho(
        @Body request: PaymentRequest
    ): Response<PaymentResponse>

    /**
     * Health check endpoint — not ISO8583, just a REST ping to verify Mockoon is running.
     */
    @GET("health")
    suspend fun checkHealth(): Response<PaymentResponse>
}
