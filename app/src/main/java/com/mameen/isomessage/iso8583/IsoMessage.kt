package com.mameen.isomessage.iso8583

import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a fully populated ISO8583 message.
 *
 * An ISO8583 message consists of:
 *  1. MTI    — Message Type Indicator (4 characters, e.g. "0200")
 *  2. Bitmap — Binary field indicating which DEs are present
 *  3. Fields — The actual Data Elements (DE2, DE3, DE4, etc.)
 *
 * This class is the domain model — it's independent of JSON or byte encoding.
 * Use [IsoMessageBuilder] to construct it and [IsoMessageParser] to decode it.
 */
data class IsoMessage(
    val mti: String,
    val fields: Map<Int, String>,
    val rawHex: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val mtiEnum: Mti? get() = Mti.fromCode(mti)
    val bitmap: BitMap get() = BitMap.fromDEs(fields.keys.toSet())

    fun getField(de: Int): String? = fields[de]

    // Convenience accessors for the most common fields
    val pan: String? get() = getField(2)
    val processingCode: String? get() = getField(3)
    val amount: String? get() = getField(4)
    val stan: String? get() = getField(11)
    val posEntryMode: String? get() = getField(22)
    val rrn: String? get() = getField(37)
    val authCode: String? get() = getField(38)
    val responseCode: String? get() = getField(39)
    val terminalId: String? get() = getField(41)
    val merchantId: String? get() = getField(42)
    val currencyCode: String? get() = getField(49)
    val emvData: String? get() = getField(55)

    /**
     * Mask the PAN for display (PCI DSS requirement — never show full PAN in logs/UI).
     * Shows first 6 and last 4 digits only: 541333******0012
     */
    val maskedPan: String?
        get() = pan?.let { p ->
            if (p.length >= 13) {
                "${p.take(6)}${"*".repeat(p.length - 10)}${p.takeLast(4)}"
            } else p
        }

    /**
     * Parse DE4 amount into a human-readable decimal string.
     * ISO8583 amount is in minor units (cents) — divide by 100 for display.
     */
    val amountFormatted: String
        get() = amount?.toLongOrNull()?.let { "%.2f".format(it / 100.0) } ?: "0.00"

    /** Decode the response code into a human-readable message */
    val responseMessage: String
        get() = responseCode?.let { IsoDataElements.responseCodes[it] } ?: "Unknown"

    /** Decode DE3 processing code */
    val transactionTypeName: String
        get() = processingCode?.let { IsoDataElements.processingCodes[it] } ?: processingCode ?: "Unknown"

    val isApproved: Boolean get() = responseCode == "00"
    val isDeclined: Boolean get() = responseCode != null && responseCode != "00"
    val isRequest: Boolean get() = mtiEnum?.isRequest ?: false

    val formattedTimestamp: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
}
