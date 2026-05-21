package com.mameen.isomessage.iso8583

import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * ISO8583 Message Builder
 *
 * Fluent DSL for constructing ISO8583 messages.
 * This builder pattern ensures all required fields are set before building.
 *
 * Usage example (Purchase 0200):
 * ```kotlin
 * val msg = IsoMessageBuilder()
 *     .mti("0200")
 *     .pan("5413330089010012")
 *     .processingCode("000000")
 *     .amount(1000L)           // 10.00 in minor units
 *     .stan(generateStan())
 *     .terminalId("TERM0001")
 *     .currencyCode("818")     // EGP
 *     .build()
 * ```
 */
class IsoMessageBuilder {

    private var mti: String = ""
    private val fields = mutableMapOf<Int, String>()

    fun mti(mti: String) = apply { this.mti = mti }
    fun mtiEnum(mti: Mti) = apply { this.mti = mti.code }

    // ── Convenience setters ───────────────────────────────────────────────────

    /** DE2 — PAN (up to 19 digits, LLVAR) */
    fun pan(pan: String) = field(2, pan)

    /** DE3 — Processing Code (exactly 6 digits) */
    fun processingCode(code: String) = field(3, code.padStart(6, '0'))

    /**
     * DE4 — Transaction Amount (12 digits, minor currency units).
     * Pass the amount in cents: 1000 = 10.00 of whatever currency.
     */
    fun amount(amountMinorUnits: Long) = field(4, amountMinorUnits.toString().padStart(12, '0'))

    /** DE7 — Transmission Date/Time (MMDDHHmmss) */
    fun transmissionDateTime(dateTime: String = nowMMDDHHmmss()) = field(7, dateTime)

    /** DE11 — STAN (System Trace Audit Number, 6 digits) */
    fun stan(stan: String = generateStan()) = field(11, stan)

    /** DE12 — Local Transaction Time (HHmmss) */
    fun localTime(time: String = nowHHmmss()) = field(12, time)

    /** DE13 — Local Transaction Date (MMDD) */
    fun localDate(date: String = nowMMDD()) = field(13, date)

    /** DE14 — Card Expiry (YYMM) */
    fun expiryDate(yymm: String) = field(14, yymm)

    /**
     * DE22 — POS Entry Mode.
     * "051" = Chip/ICC read, no PIN bypass
     * "071" = Contactless NFC
     * "021" = Magnetic stripe fallback
     * "011" = Manual key entry
     */
    fun posEntryMode(mode: String) = field(22, mode)

    /** DE37 — Retrieval Reference Number (12 alphanumeric) */
    fun rrn(rrn: String = generateRrn()) = field(37, rrn)

    /** DE38 — Authorization Code (filled by issuer in response) */
    fun authCode(code: String) = field(38, code.padEnd(6))

    /** DE39 — Response Code (filled by issuer in response) */
    fun responseCode(code: String) = field(39, code)

    /** DE41 — Terminal ID (8 characters) */
    fun terminalId(id: String) = field(41, id.padEnd(8))

    /** DE42 — Merchant ID (15 characters) */
    fun merchantId(id: String) = field(42, id.padEnd(15))

    /** DE49 — Currency Code (3 digits, ISO 4217 numeric) */
    fun currencyCode(code: String) = field(49, code)

    /** DE55 — EMV/ICC Data (hex encoded TLV data) */
    fun emvData(hexTlv: String) = field(55, hexTlv)

    /** Set any field directly */
    fun field(de: Int, value: String) = apply { fields[de] = value }

    /**
     * Build the [IsoMessage].
     * @throws IllegalArgumentException if MTI is missing.
     */
    fun build(): IsoMessage {
        require(mti.isNotBlank()) { "MTI must be set before building an ISO message" }
        require(mti.length == 4 && mti.all { it.isDigit() }) {
            "MTI must be exactly 4 digits, got: '$mti'"
        }
        return IsoMessage(
            mti = mti,
            fields = fields.toSortedMap(),
            rawHex = encodeToHex()
        )
    }

    /**
     * Educational hex encoding of the ISO message structure.
     * Real implementations encode to bytes according to the DE type (N, AN, B, etc.)
     * This simulation produces a human-readable hex representation.
     */
    private fun encodeToHex(): String {
        val sb = StringBuilder()
        // MTI as ASCII hex
        sb.append(mti.map { it.code.toString(16).padStart(2, '0') }.joinToString(""))
        // Bitmap
        sb.append(BitMap.fromDEs(fields.keys.toSet()).toHexString())
        // Fields (simplified — real encoding uses DE-specific rules)
        fields.forEach { (_, value) ->
            sb.append(value.map { it.code.toString(16).padStart(2, '0') }.joinToString(""))
        }
        return sb.toString().uppercase()
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    companion object {
        /** Generate a random 6-digit STAN */
        fun generateStan(): String = Random.nextInt(100000, 999999).toString()

        /** Generate a 12-character RRN based on current timestamp */
        fun generateRrn(): String {
            val ts = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())
            return ts
        }

        private fun nowMMDDHHmmss(): String =
            SimpleDateFormat("MMddHHmmss", Locale.getDefault()).format(Date())

        private fun nowHHmmss(): String =
            SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())

        private fun nowMMDD(): String =
            SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())

        /**
         * Quick factory: build a standard Purchase 0200 request with sensible defaults.
         */
        fun purchaseRequest(
            pan: String,
            amountCents: Long,
            terminalId: String = "TERM0001",
            merchantId: String = "MERCHANT001    ",
            currencyCode: String = "818",
            posEntryMode: String = "051",
            emvData: String? = null
        ): IsoMessage = IsoMessageBuilder()
            .mti("0200")
            .pan(pan)
            .processingCode("000000")
            .amount(amountCents)
            .transmissionDateTime()
            .stan()
            .localTime()
            .localDate()
            .posEntryMode(posEntryMode)
            .terminalId(terminalId)
            .merchantId(merchantId)
            .currencyCode(currencyCode)
            .apply { emvData?.let { emvData(it) } }
            .build()

        /** Quick factory: Refund 0200 with processing code 200000 */
        fun refundRequest(
            pan: String,
            amountCents: Long,
            originalRrn: String,
            terminalId: String = "TERM0001"
        ): IsoMessage = IsoMessageBuilder()
            .mti("0200")
            .pan(pan)
            .processingCode("200000")
            .amount(amountCents)
            .transmissionDateTime()
            .stan()
            .localTime()
            .localDate()
            .rrn(originalRrn)
            .terminalId(terminalId)
            .currencyCode("818")
            .build()

        /** Quick factory: Reversal 0400 */
        fun reversalRequest(
            pan: String,
            amountCents: Long,
            originalStan: String,
            originalRrn: String,
            terminalId: String = "TERM0001"
        ): IsoMessage = IsoMessageBuilder()
            .mti("0400")
            .pan(pan)
            .processingCode("000000")
            .amount(amountCents)
            .transmissionDateTime()
            .stan()
            .localTime()
            .localDate()
            .field(56, originalStan) // DE56: Original data elements
            .rrn(originalRrn)
            .terminalId(terminalId)
            .currencyCode("818")
            .build()

        /** Quick factory: Balance Inquiry 0200 with processing code 900000 */
        fun balanceInquiryRequest(
            pan: String,
            terminalId: String = "TERM0001"
        ): IsoMessage = IsoMessageBuilder()
            .mti("0200")
            .pan(pan)
            .processingCode("900000")
            .amount(0L)
            .transmissionDateTime()
            .stan()
            .localTime()
            .localDate()
            .terminalId(terminalId)
            .currencyCode("818")
            .build()

        /** Quick factory: Settlement / Reconciliation 0500 */
        fun settlementRequest(
            terminalId: String = "TERM0001"
        ): IsoMessage = IsoMessageBuilder()
            .mti("0500")
            .processingCode("920000")
            .transmissionDateTime()
            .stan()
            .localTime()
            .localDate()
            .terminalId(terminalId)
            .build()
    }
}
