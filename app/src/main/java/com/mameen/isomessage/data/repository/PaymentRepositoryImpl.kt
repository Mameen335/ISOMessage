package com.mameen.isomessage.data.repository

import com.mameen.isomessage.data.remote.PaymentApiService
import com.mameen.isomessage.data.remote.dto.PaymentRequest
import com.mameen.isomessage.data.remote.dto.PaymentResponse
import com.mameen.isomessage.domain.model.*
import com.mameen.isomessage.domain.repository.PaymentRepository
import com.mameen.isomessage.iso8583.IsoMessage
import com.mameen.isomessage.iso8583.IsoMessageBuilder
import com.mameen.isomessage.iso8583.IsoMessageParser
import com.mameen.isomessage.security.HexUtils
import com.mameen.isomessage.util.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [PaymentRepository].
 *
 * Responsibilities:
 * 1. Convert domain [IsoMessage] → JSON DTO → Retrofit call
 * 2. Convert JSON response DTO → domain [IsoMessage] → [Transaction]
 * 3. Maintain in-memory transaction history (no database for this demo)
 * 4. Map network errors to [PaymentResult] types
 * 5. Log all requests and responses
 *
 * Data flow:
 * UseCase → Repository → ApiService → Mockoon → ApiService → Repository → UseCase
 */
@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val apiService: PaymentApiService,
    private val logger: AppLogger
) : PaymentRepository {

    // In-memory transaction store — use Room for production
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getTransactionHistory(): Flow<List<Transaction>> =
        _transactions.asStateFlow()

    override suspend fun getTransactionById(id: String): Transaction? =
        _transactions.value.find { it.id == id }

    override suspend fun saveTransaction(transaction: Transaction) {
        _transactions.value = listOf(transaction) + _transactions.value
    }

    override suspend fun clearTransactions() {
        _transactions.value = emptyList()
    }

    override suspend fun sendTransaction(
        request: IsoMessage,
        simulateTimeout: Boolean
    ): PaymentResult {
        val startTime = System.currentTimeMillis()
        val dto = isoMessageToDto(request)

        logger.logIsoRequest(
            "Sending ${request.mtiEnum?.description ?: request.mti}",
            request.mti,
            request.stan
        )
        logger.d("REPO", "Request JSON: ${IsoMessageParser.toJson(request)}")

        return try {
            val response = apiService.sendIsoTransaction(dto, simulateTimeout)
            val processingTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val body = response.body() ?: return PaymentResult.UnknownError("Empty response body")
                handleSuccessResponse(request, body, processingTime)
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                logger.logNetworkError(errorMsg)
                PaymentResult.NetworkError(errorMsg, isRetryable = response.code() >= 500)
            }
        } catch (e: SocketTimeoutException) {
            logger.logNetworkError("Timeout: ${e.message}")
            PaymentResult.NetworkError("Connection timed out", isTimeout = true, isRetryable = true)
        } catch (e: IOException) {
            logger.logNetworkError("IO Error: ${e.message}")
            PaymentResult.Offline()
        } catch (e: HttpException) {
            logger.logNetworkError("HTTP Error: ${e.message}")
            PaymentResult.NetworkError("HTTP Error ${e.code()}", isRetryable = e.code() >= 500)
        } catch (e: Exception) {
            logger.e("REPO", "Unexpected error", e)
            PaymentResult.UnknownError(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun sendReversal(request: IsoMessage): PaymentResult {
        val startTime = System.currentTimeMillis()
        val dto = isoMessageToDto(request)

        logger.logIsoRequest("Sending Reversal 0400", request.mti, request.stan)

        return try {
            val response = apiService.sendReversal(dto)
            val processingTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val body = response.body() ?: return PaymentResult.UnknownError("Empty response")
                handleSuccessResponse(request, body, processingTime)
            } else {
                PaymentResult.NetworkError("HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: SocketTimeoutException) {
            PaymentResult.NetworkError("Reversal timed out", isTimeout = true, isRetryable = true)
        } catch (e: IOException) {
            PaymentResult.Offline()
        } catch (e: Exception) {
            PaymentResult.UnknownError(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun sendSettlement(request: IsoMessage): PaymentResult {
        val startTime = System.currentTimeMillis()
        val dto = isoMessageToDto(request)

        logger.logIsoRequest("Sending Settlement 0500", request.mti, request.stan)

        return try {
            val response = apiService.sendSettlement(dto)
            val processingTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val body = response.body() ?: return PaymentResult.UnknownError("Empty response")
                handleSuccessResponse(request, body, processingTime)
            } else {
                PaymentResult.NetworkError("HTTP ${response.code()}")
            }
        } catch (e: IOException) {
            PaymentResult.Offline()
        } catch (e: Exception) {
            PaymentResult.UnknownError(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun sendEcho(): PaymentResult {
        val echoRequest = IsoMessageBuilder()
            .mti("0800")
            .stan()
            .transmissionDateTime()
            .terminalId("TERM0001")
            .build()

        return try {
            val response = apiService.sendEcho(isoMessageToDto(echoRequest))
            if (response.isSuccessful) {
                val body = response.body() ?: return PaymentResult.UnknownError("Empty echo response")
                handleSuccessResponse(echoRequest, body, 0L)
            } else {
                PaymentResult.NetworkError("Echo failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            PaymentResult.NetworkError("Host unreachable: ${e.message}", isRetryable = true)
        }
    }

    override suspend fun checkHostConnectivity(): Boolean = try {
        val response = apiService.checkHealth()
        response.isSuccessful
    } catch (e: Exception) {
        false
    }

    override fun updateBaseUrl(newUrl: String) {
        // In this demo, URL changes require restart. In production, use a dynamic OkHttp interceptor.
        logger.i("REPO", "Base URL update requested: $newUrl (requires app restart in demo)")
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun isoMessageToDto(isoMessage: IsoMessage): PaymentRequest =
        PaymentRequest(
            mti = isoMessage.mti,
            fields = isoMessage.fields.mapKeys { it.key.toString() }
        )

    private suspend fun handleSuccessResponse(
        request: IsoMessage,
        response: PaymentResponse,
        processingTimeMs: Long
    ): PaymentResult {
        val isoResponse = IsoMessageParser.fromResponse(response)

        logger.logIsoResponse(
            "${isoResponse.mtiEnum?.description ?: isoResponse.mti} — RC=${isoResponse.responseCode} ${isoResponse.responseMessage}",
            isoResponse.mti,
            isoResponse.responseCode,
            isoResponse.stan
        )

        val transaction = buildTransaction(request, isoResponse, processingTimeMs)
        saveTransaction(transaction)

        return PaymentResult.Success(transaction, isoResponse)
    }

    private fun buildTransaction(
        request: IsoMessage,
        response: IsoMessage,
        processingTimeMs: Long
    ): Transaction {
        val responseCode = response.responseCode ?: "99"
        val status = when {
            responseCode == "00" -> TransactionStatus.APPROVED
            responseCode == "68" -> TransactionStatus.TIMEOUT
            else -> TransactionStatus.DECLINED
        }

        return Transaction(
            type = TransactionType.fromProcessingCode(request.processingCode ?: "000000"),
            status = status,
            amount = request.amount?.toLongOrNull() ?: 0L,
            currency = "EGP",
            currencyCode = request.currencyCode ?: "818",
            pan = request.maskedPan ?: "****",
            terminalId = request.terminalId ?: "UNKNOWN",
            merchantId = request.merchantId ?: "UNKNOWN",
            stan = request.stan ?: response.stan ?: "000000",
            rrn = response.rrn ?: request.rrn ?: IsoMessageBuilder.generateRrn(),
            authCode = response.authCode?.trim() ?: "",
            responseCode = responseCode,
            responseMessage = response.responseMessage,
            mtiRequest = request.mti,
            mtiResponse = response.mti,
            isoRequestJson = IsoMessageParser.toJson(request),
            isoResponseJson = IsoMessageParser.toJson(response),
            posEntryMode = PosEntryMode.fromCode(request.posEntryMode ?: "051"),
            processingTimeMs = processingTimeMs
        )
    }
}
