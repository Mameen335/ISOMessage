package com.mameen.isomessage.domain.repository

import com.mameen.isomessage.domain.model.PaymentResult
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.iso8583.IsoMessage
import kotlinx.coroutines.flow.Flow

/**
 * Payment Repository interface — the contract between domain and data layers.
 *
 * Clean Architecture: The domain layer defines this interface.
 * The data layer provides the implementation (PaymentRepositoryImpl).
 * ViewModels and UseCases depend ONLY on this interface, not the implementation.
 *
 * This means we can swap the real implementation with a FakePaymentRepository
 * for unit tests without changing any business logic.
 */
interface PaymentRepository {

    /**
     * Send any ISO8583 financial transaction (Purchase, Refund, Balance Inquiry).
     * The MTI and processing code in the [IsoMessage] determine the transaction type.
     *
     * @param request The constructed ISO8583 request message
     * @param simulateTimeout If true, sends the timeout trigger header to Mockoon
     * @return [PaymentResult] — Success, NetworkError, or UnknownError
     */
    suspend fun sendTransaction(
        request: IsoMessage,
        simulateTimeout: Boolean = false
    ): PaymentResult

    /**
     * Send a Reversal request (MTI 0400) to reverse a previously approved transaction.
     *
     * In real systems, reversals are sent automatically when:
     * - The terminal crashes after approval but before receipt printing
     * - The merchant cancels after approval
     * Reversals MUST be sent to prevent the cardholder being charged for nothing.
     *
     * @param request The 0400 reversal message
     * @return [PaymentResult]
     */
    suspend fun sendReversal(request: IsoMessage): PaymentResult

    /**
     * Send a Settlement/Reconciliation request (MTI 0500).
     *
     * Settlement clears the day's transactions with the acquiring bank.
     * Typically done at end-of-day. The terminal sends totals and the host
     * confirms they match (balanced) or flags a discrepancy.
     *
     * @param request The 0500 settlement message
     * @return [PaymentResult]
     */
    suspend fun sendSettlement(request: IsoMessage): PaymentResult

    /**
     * Send a Network Echo (0800) to test host connectivity.
     * Used before the first transaction of the day.
     */
    suspend fun sendEcho(): PaymentResult

    /**
     * Get a Flow of all stored transactions (in-memory for this demo).
     * In a real terminal: transactions are stored in encrypted local SQLite/Room.
     */
    fun getTransactionHistory(): Flow<List<Transaction>>

    /**
     * Get a single transaction by ID.
     */
    suspend fun getTransactionById(id: String): Transaction?

    /**
     * Store a transaction locally after receiving the host response.
     */
    suspend fun saveTransaction(transaction: Transaction)

    /**
     * Clear all stored transactions (for demo/testing purposes).
     */
    suspend fun clearTransactions()

    /**
     * Check current host connectivity status.
     */
    suspend fun checkHostConnectivity(): Boolean

    /**
     * Update the base URL used by Retrofit (for Settings screen configuration).
     */
    fun updateBaseUrl(newUrl: String)
}
