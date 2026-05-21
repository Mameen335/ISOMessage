package com.mameen.isomessage.emv

/**
 * TLV (Tag-Length-Value) Parser
 *
 * TLV is the encoding used throughout EMV chip card communication.
 * Every data object on the chip is encoded as:
 *   [TAG bytes][LENGTH byte(s)][VALUE bytes]
 *
 * Tags can be 1 or 2 bytes.
 * Length can be 1 byte (for values up to 127 bytes) or multi-byte for longer values.
 * Values are raw bytes interpreted according to the tag definition.
 *
 * Example DE55 TLV stream (hex):
 *   9F 26 08 12 34 56 78 90 12 34 56   — Tag 9F26 (ARQC), len=8, 8 bytes of cryptogram
 *   9F 27 01 80                         — Tag 9F27 (CID), len=1, value=80
 *   9F 10 07 07 A0 00 00 04 10 10       — Tag 9F10 (IAD), len=7, issuer app data
 *
 * Understanding TLV is fundamental to EMV, NFC, and smart card development.
 */
object TlvParser {

    /**
     * Parse a hex-encoded TLV byte stream into a list of [TlvObject].
     *
     * @param hex Hex string of the TLV data (DE55 value, for example)
     * @return List of parsed TLV objects
     */
    fun parse(hex: String): List<TlvObject> {
        val cleanHex = hex.uppercase().replace(" ", "")
        if (cleanHex.isEmpty() || cleanHex.length % 2 != 0) return emptyList()

        val bytes = cleanHex.chunked(2).map { it.toInt(16) }
        val result = mutableListOf<TlvObject>()
        var index = 0

        while (index < bytes.size) {
            try {
                // ── Parse Tag ────────────────────────────────────────────────
                val tagStart = index
                var tagHex = "%02X".format(bytes[index])
                index++

                // Multi-byte tag: if low 5 bits of first byte are all 1s, read next byte(s)
                if ((bytes[tagStart] and 0x1F) == 0x1F) {
                    do {
                        tagHex += "%02X".format(bytes[index])
                        index++
                    } while (index < bytes.size && (bytes[index - 1] and 0x80) != 0)
                }

                if (index >= bytes.size) break

                // ── Parse Length ──────────────────────────────────────────────
                val length: Int
                val lengthByte = bytes[index++]

                length = if (lengthByte <= 0x7F) {
                    // Short form: length is the byte value
                    lengthByte
                } else {
                    // Long form: length byte tells how many following bytes encode the length
                    val numLengthBytes = lengthByte and 0x7F
                    var len = 0
                    for (i in 0 until numLengthBytes) {
                        len = (len shl 8) or bytes[index++]
                    }
                    len
                }

                if (index + length > bytes.size) break

                // ── Parse Value ───────────────────────────────────────────────
                val valueBytes = bytes.subList(index, index + length)
                val valueHex = valueBytes.joinToString("") { "%02X".format(it) }
                index += length

                val knownTag = EmvTag.fromHex(tagHex)
                result.add(
                    TlvObject(
                        tagHex = tagHex,
                        length = length,
                        valueHex = valueHex,
                        knownTag = knownTag
                    )
                )
            } catch (e: Exception) {
                // Stop parsing on error — return what we have so far
                break
            }
        }

        return result
    }

    /**
     * Parse a hex TLV stream and return a map of tag → value hex.
     */
    fun parseToMap(hex: String): Map<String, String> =
        parse(hex).associate { it.tagHex to it.valueHex }

    /**
     * Encode a list of [TlvObject] back to a hex string.
     */
    fun encode(tlvObjects: List<TlvObject>): String {
        val sb = StringBuilder()
        tlvObjects.forEach { tlv ->
            sb.append(tlv.tagHex)
            // Length encoding (simplified — only handles lengths up to 127 with single byte)
            if (tlv.length <= 0x7F) {
                sb.append("%02X".format(tlv.length))
            } else {
                sb.append("81")  // 1 byte follows with the length
                sb.append("%02X".format(tlv.length))
            }
            sb.append(tlv.valueHex)
        }
        return sb.toString()
    }
}

/**
 * A single parsed TLV data object.
 */
data class TlvObject(
    val tagHex: String,
    val length: Int,
    val valueHex: String,
    val knownTag: EmvTag? = null
) {
    val tagName: String get() = knownTag?.displayName ?: "Unknown Tag ($tagHex)"
    val tagDescription: String get() = knownTag?.description ?: "Proprietary or unknown EMV tag"

    /** Try to decode value as ASCII text (for string-type fields like cardholder name) */
    val valueAsAscii: String?
        get() = try {
            valueHex.chunked(2)
                .map { it.toInt(16).toChar() }
                .joinToString("")
                .takeIf { it.all { c -> c.isLetterOrDigit() || c.isWhitespace() || c == '/' } }
        } catch (e: Exception) { null }

    /** Display format for UI — ASCII if readable, otherwise raw hex */
    val displayValue: String
        get() = valueAsAscii?.let { "\"$it\"  [$valueHex]" } ?: valueHex
}
