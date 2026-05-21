package com.mameen.isomessage.domain.model

import com.mameen.isomessage.iso8583.IsoMessage

/**
 * Sealed class hierarchy for payment operation results.
 *
 * This pattern (sealed class for results) is idiomatic Kotlin/Clean Architecture.
 * ViewModels observe these results and update UI state accordingly.
 *
 * Benefits over exceptions:
 * - Forces callers to handle all cases
 * - No unchecked exception propagation
 * - Easy to map to UI states
 */
sealed class PaymentResult {

    /** Transaction completed — both approved and declined are "success" at the network level */
    data class Success(
        val transaction: Transaction,
        val isoResponse: IsoMessage
    ) : PaymentResult()

    /** Network/timeout error — the host did not respond */
    data class NetworkError(
        val message: String,
        val isTimeout: Boolean = false,
        val isRetryable: Boolean = true
    ) : PaymentResult()

    /** Validation error — bad input before even sending to host */
    data class ValidationError(
        val field: String,
        val message: String
    ) : PaymentResult()

    /** Offline mode — no network available */
    data class Offline(
        val message: String = "No network connection. Transaction stored for later processing."
    ) : PaymentResult()

    /** Unexpected error */
    data class UnknownError(
        val message: String,
        val cause: Throwable? = null
    ) : PaymentResult()
}

/** Generic resource wrapper for use with StateFlow in ViewModels */
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val isRetryable: Boolean = true) : UiState<Nothing>()
}
