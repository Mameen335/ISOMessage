package com.mameen.isomessage.iso8583

/**
 * ISO8583 Message Type Indicator (MTI)
 *
 * The MTI is a 4-digit numeric code that defines the overall function of the message.
 * Format: [Version][Class][Function][Origin]
 *
 * Example: 0200
 *   0 = Version (ISO 8583-1:1987)
 *   2 = Class (Financial messages)
 *   0 = Function (Request)
 *   0 = Origin (Acquirer)
 *
 * This is the FIRST thing parsed in every ISO8583 message.
 */
enum class Mti(
    val code: String,
    val description: String,
    val isRequest: Boolean
) {
    // ── Authorization ─────────────────────────────────────────────────────────
    AUTH_REQUEST("0100", "Authorization Request", true),
    AUTH_RESPONSE("0110", "Authorization Response", false),
    AUTH_ADVICE("0120", "Authorization Advice", true),
    AUTH_ADVICE_RESPONSE("0130", "Authorization Advice Response", false),

    // ── Financial (Purchase) ──────────────────────────────────────────────────
    FINANCIAL_REQUEST("0200", "Financial Transaction Request (Purchase)", true),
    FINANCIAL_RESPONSE("0210", "Financial Transaction Response", false),
    FINANCIAL_ADVICE("0220", "Financial Advice", true),
    FINANCIAL_ADVICE_RESPONSE("0230", "Financial Advice Response", false),

    // ── Reversal ──────────────────────────────────────────────────────────────
    REVERSAL_REQUEST("0400", "Reversal Request", true),
    REVERSAL_RESPONSE("0410", "Reversal Response", false),
    REVERSAL_ADVICE("0420", "Reversal Advice", true),
    REVERSAL_ADVICE_RESPONSE("0430", "Reversal Advice Response", false),

    // ── Reconciliation (Settlement) ───────────────────────────────────────────
    RECONCILIATION_REQUEST("0500", "Reconciliation Request (Settlement)", true),
    RECONCILIATION_RESPONSE("0510", "Reconciliation Response", false),

    // ── Administrative ────────────────────────────────────────────────────────
    ADMIN_REQUEST("0600", "Administrative Request (Balance Inquiry)", true),
    ADMIN_RESPONSE("0610", "Administrative Response", false),

    // ── Network Management ────────────────────────────────────────────────────
    NETWORK_MANAGEMENT_REQUEST("0800", "Network Management Request (Echo/Sign-On)", true),
    NETWORK_MANAGEMENT_RESPONSE("0810", "Network Management Response", false);

    companion object {
        /**
         * Parse an MTI code string into an [Mti] enum, or return null if unknown.
         * Unknown MTIs still occur in real networks — always handle gracefully.
         */
        fun fromCode(code: String): Mti? = entries.find { it.code == code }

        /**
         * Describe the version digit semantics (first digit of MTI)
         */
        fun describeVersion(code: String): String = when (code.firstOrNull()) {
            '0' -> "ISO 8583-1:1987"
            '1' -> "ISO 8583-2:1993"
            '2' -> "ISO 8583-3:2003"
            else -> "Unknown version"
        }

        /**
         * Describe the message class (second digit of MTI)
         */
        fun describeClass(code: String): String = when (code.getOrNull(1)) {
            '1' -> "Authorization"
            '2' -> "Financial"
            '3' -> "File Action"
            '4' -> "Reversal / Chargeback"
            '5' -> "Reconciliation"
            '6' -> "Administrative"
            '7' -> "Fee Collection"
            '8' -> "Network Management"
            '9' -> "Reserved"
            else -> "Unknown class"
        }

        /**
         * Describe the function (third digit of MTI)
         */
        fun describeFunction(code: String): String = when (code.getOrNull(2)) {
            '0' -> "Request"
            '1' -> "Response"
            '2' -> "Advice"
            '3' -> "Advice Response"
            '4' -> "Notification"
            '5' -> "Notification Acknowledgement"
            else -> "Unknown function"
        }

        /**
         * Describe the origin (fourth digit of MTI)
         */
        fun describeOrigin(code: String): String = when (code.getOrNull(3)) {
            '0' -> "Acquirer"
            '1' -> "Acquirer Repeat"
            '2' -> "Issuer"
            '3' -> "Issuer Repeat"
            '4' -> "Other"
            '5' -> "Other Repeat"
            else -> "Unknown origin"
        }
    }
}
