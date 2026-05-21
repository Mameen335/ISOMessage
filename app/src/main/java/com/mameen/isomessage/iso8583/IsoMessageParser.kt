package com.mameen.isomessage.iso8583

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mameen.isomessage.data.remote.dto.PaymentResponse

/**
 * ISO8583 Message Parser
 *
 * In real implementations, ISO8583 messages arrive as raw bytes over a TCP socket.
 * For this educational simulator, messages travel as JSON (via the Mockoon HTTP backend),
 * and this parser converts between JSON and [IsoMessage].
 *
 * Real parsing flow:
 *   1. Read 2/4 byte length header (TPDU or BCD-encoded)
 *   2. Parse 4-char MTI
 *   3. Parse 8-byte primary bitmap (and optional 8-byte secondary)
 *   4. For each bit set in bitmap, parse the corresponding DE
 *      - Fixed: read exactly N chars/bytes
 *      - LLVAR: read 2-char length prefix, then read that many chars
 *      - LLLVAR: read 3-char length prefix, then read that many chars
 *
 * This simulator shows the LOGICAL structure while HTTP handles transport.
 */
object IsoMessageParser {

    private val gson = Gson()

    /**
     * Parse a [PaymentResponse] DTO (from Retrofit/JSON) into an [IsoMessage].
     */
    fun fromResponse(response: PaymentResponse): IsoMessage {
        val fields = mutableMapOf<Int, String>()

        response.fields.forEach { (key, value) ->
            key.toIntOrNull()?.let { de ->
                fields[de] = value
            }
        }

        return IsoMessage(
            mti = response.mti,
            fields = fields.toSortedMap()
        )
    }

    /**
     * Parse a raw JSON string into an [IsoMessage].
     * The JSON format mirrors the Mockoon backend: { "mti": "0210", "fields": { "39": "00", ... } }
     */
    fun fromJson(json: String): IsoMessage {
        val jsonObj = gson.fromJson(json, JsonObject::class.java)
        val mti = jsonObj.get("mti")?.asString ?: "0000"
        val fields = mutableMapOf<Int, String>()

        jsonObj.getAsJsonObject("fields")?.entrySet()?.forEach { (key, value) ->
            key.toIntOrNull()?.let { de ->
                fields[de] = value.asString
            }
        }

        return IsoMessage(mti = mti, fields = fields.toSortedMap())
    }

    /**
     * Convert an [IsoMessage] to the JSON format expected by the Mockoon backend.
     */
    fun toJson(message: IsoMessage): String {
        val obj = JsonObject()
        obj.addProperty("mti", message.mti)
        val fieldsObj = JsonObject()
        message.fields.forEach { (de, value) ->
            fieldsObj.addProperty(de.toString(), value)
        }
        obj.add("fields", fieldsObj)
        return gson.toJson(obj)
    }

    /**
     * Produce a pretty-printed "ISO field inspector" view of the message.
     * This is what the ISO Message Viewer screen displays.
     */
    fun toPrettyString(message: IsoMessage): String {
        val sb = StringBuilder()
        sb.appendLine("┌─────────────────────────────────────────────────────┐")
        sb.appendLine("│ ISO8583 Message                                      │")
        sb.appendLine("├─────────────────────────────────────────────────────┤")
        sb.appendLine("│ MTI: ${message.mti}  →  ${message.mtiEnum?.description ?: "Unknown"}".padEnd(53) + "│")
        sb.appendLine("│ Timestamp: ${message.formattedTimestamp}".padEnd(53) + "│")
        sb.appendLine("├──────┬────────────────────────────────┬──────────────┤")
        sb.appendLine("│  DE  │ Field Name                     │ Value        │")
        sb.appendLine("├──────┼────────────────────────────────┼──────────────┤")

        message.fields.forEach { (de, value) ->
            val def = IsoDataElements.getDefinition(de)
            val displayValue = when (de) {
                2 -> message.maskedPan ?: value  // Always mask PAN
                35 -> "****TRACK2 REDACTED****"   // Never show Track 2
                else -> value.take(14)             // Truncate long values
            }
            val fieldName = def?.name?.take(30) ?: "DE$de"
            sb.appendLine("│  %-3d │ %-30s │ %-12s │".format(de, fieldName, displayValue))
        }

        sb.appendLine("└──────┴────────────────────────────────┴──────────────┘")
        sb.appendLine("Bitmap: ${message.bitmap.toHexString()}")
        return sb.toString()
    }

    /**
     * Parse a hex-encoded ISO8583 byte stream (educational simulation).
     * Real terminals communicate over TCP with actual binary encoding.
     *
     * This demonstrates the logical parse flow without the full byte-level implementation.
     */
    fun parseHexStream(hex: String): ParseResult {
        if (hex.length < 8) return ParseResult.Error("Hex stream too short: ${hex.length} chars")

        return try {
            // Step 1: MTI (first 4 hex chars = 2 bytes in ASCII encoding, or 8 chars for BCD)
            // For educational purposes we treat each char pair as one byte
            val mtiHex = hex.take(8)
            val mti = mtiHex.chunked(2).map { it.toInt(16).toChar() }.joinToString("")

            // Step 2: Bitmap (next 16 hex chars = 8 bytes primary)
            val bitmapHex = hex.substring(8, 24)
            val bitmap = BitMap.parseHex(bitmapHex)

            ParseResult.Success(
                mti = mti,
                bitmapHex = bitmapHex,
                presentDEs = bitmap.toVisualGrid()
                    .flatMap { it.bits }
                    .filter { it.isSet }
                    .map { it.deNumber }
                    .toSet()
            )
        } catch (e: Exception) {
            ParseResult.Error("Parse failed: ${e.message}")
        }
    }

    sealed class ParseResult {
        data class Success(
            val mti: String,
            val bitmapHex: String,
            val presentDEs: Set<Int>
        ) : ParseResult()

        data class Error(val message: String) : ParseResult()
    }
}
