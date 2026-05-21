package com.mameen.isomessage.domain.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Domain model representing a completed payment transaction.
 *
 * This is the core business object in our domain layer.
 * It's populated after a successful ISO8583 request/response cycle.
 *
 * Clean Architecture principle: This model has NO dependencies on Android,
 * Retrofit, or Room — it's pure Kotlin. It can be tested in isolation.
 */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val status: TransactionStatus,
    val amount: Long,           // In minor currency units (cents)
    val currency: String = "EGP",
    val currencyCode: String = "818",
    val pan: String,            // Masked: 541333******0012
    val cardBrand: CardBrand = CardBrand.fromPan(pan),
    val terminalId: String,
    val merchantId: String,
    val stan: String,
    val rrn: String = "",
    val authCode: String = "",
    val responseCode: String,
    val responseMessage: String,
    val mtiRequest: String,
    val mtiResponse: String,
    val isoRequestJson: String = "",
    val isoResponseJson: String = "",
    val posEntryMode: PosEntryMode = PosEntryMode.CHIP,
    val timestamp: Long = System.currentTimeMillis(),
    val processingTimeMs: Long = 0L
) {
    val amountFormatted: String
        get() = "%.2f %s".format(amount / 100.0, currency)

    val isApproved: Boolean get() = status == TransactionStatus.APPROVED
    val isDeclined: Boolean get() = status == TransactionStatus.DECLINED

    val formattedTimestamp: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))

    val formattedDate: String
        get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

enum class TransactionType(val displayName: String, val processingCode: String, val icon: String) {
    PURCHASE("Purchase", "000000", "💳"),
    REFUND("Refund", "200000", "↩️"),
    REVERSAL("Reversal", "000000", "🔄"),
    SETTLEMENT("Settlement", "920000", "🏦"),
    BALANCE_INQUIRY("Balance Inquiry", "900000", "💰"),
    ECHO("Network Echo", "000000", "📡");

    companion object {
        fun fromProcessingCode(code: String) = entries.find { it.processingCode == code } ?: PURCHASE
    }
}

enum class TransactionStatus(val displayName: String) {
    APPROVED("Approved"),
    DECLINED("Declined"),
    TIMEOUT("Timeout"),
    ERROR("Error"),
    PENDING("Pending"),
    REVERSED("Reversed"),
    REFUNDED("Refunded")
}

enum class PosEntryMode(val code: String, val displayName: String) {
    CHIP("051", "Chip (ICC)"),
    CONTACTLESS("071", "Contactless (NFC)"),
    MAGNETIC_STRIPE("021", "Magnetic Stripe"),
    MANUAL("011", "Manual Entry"),
    FALLBACK("801", "Mag. Stripe Fallback");

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code } ?: CHIP
    }
}

enum class CardBrand(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    UNKNOWN("Unknown");

    companion object {
        /**
         * Detect card brand from BIN (first 4-6 digits of PAN).
         * Real BIN tables are thousands of entries — this is a simplified demo.
         */
        fun fromPan(pan: String): CardBrand {
            val cleaned = pan.filter { it.isDigit() }
            return when {
                cleaned.startsWith("4") -> VISA
                cleaned.startsWith("51") || cleaned.startsWith("52") ||
                        cleaned.startsWith("53") || cleaned.startsWith("54") ||
                        cleaned.startsWith("55") -> MASTERCARD
                cleaned.startsWith("34") || cleaned.startsWith("37") -> AMEX
                else -> UNKNOWN
            }
        }
    }
}
