package com.mameen.isomessage.security

/**
 * Hex utility functions used throughout payment processing.
 *
 * Hexadecimal encoding is ubiquitous in payment systems — bitmaps, PINs,
 * cryptograms, keys, and many data fields are all represented in hex.
 */
object HexUtils {

    /** Convert a byte array to a hex string */
    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }

    /** Convert a hex string to a byte array */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.uppercase().replace(" ", "")
        require(cleanHex.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /** Convert a string to its hex representation (ASCII encoding) */
    fun stringToHex(str: String): String =
        str.map { "%02X".format(it.code) }.joinToString("")

    /** Convert a hex string back to an ASCII string */
    fun hexToString(hex: String): String =
        hex.chunked(2).map { it.toInt(16).toChar() }.joinToString("")

    /**
     * XOR two byte arrays (used in MAC calculation and PIN block operations).
     * Arrays must be the same length.
     */
    fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
        require(a.size == b.size) { "Arrays must be same length for XOR: ${a.size} vs ${b.size}" }
        return ByteArray(a.size) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }
    }

    /**
     * Format a hex string with spaces every 2 characters for readability.
     * Example: "9F2608123456789012" → "9F 26 08 12 34 56 78 90 12"
     */
    fun formatHex(hex: String, groupSize: Int = 2): String =
        hex.chunked(groupSize).joinToString(" ")

    /**
     * Produce a "hex dump" view like a terminal debugger:
     * Offset | Hex bytes               | ASCII representation
     */
    fun hexDump(hex: String): String {
        val bytes = hex.chunked(2)
        val sb = StringBuilder()
        sb.appendLine("Offset   Hex Dump                              ASCII")
        sb.appendLine("─────────────────────────────────────────────────────")

        bytes.chunked(16).forEachIndexed { lineIdx, lineBytes ->
            val offset = "%04X".format(lineIdx * 16)
            val hexPart = lineBytes.joinToString(" ").padEnd(47)
            val asciiPart = lineBytes.joinToString("") { byteHex ->
                val c = byteHex.toInt(16).toChar()
                if (c.isISOControl()) "." else c.toString()
            }
            sb.appendLine("$offset     $hexPart  $asciiPart")
        }

        return sb.toString()
    }

    /** Pad a string on the left with zeros to reach target length (common in ISO8583) */
    fun leftPadZero(value: String, length: Int): String = value.padStart(length, '0')

    /** Pad a string on the right with spaces to reach target length */
    fun rightPadSpace(value: String, length: Int): String = value.padEnd(length, ' ')

    /** Validate a PAN using the Luhn algorithm */
    fun isValidLuhn(pan: String): Boolean {
        val digits = pan.filter { it.isDigit() }.map { it.digitToInt() }
        if (digits.size < 13) return false

        var sum = 0
        val shouldDouble = digits.reversed().mapIndexed { index, digit ->
            if (index % 2 == 1) {
                val doubled = digit * 2
                if (doubled > 9) doubled - 9 else doubled
            } else digit
        }
        sum = shouldDouble.sum()
        return sum % 10 == 0
    }

    /** Mask a PAN for display (PCI DSS: show first 6 and last 4 only) */
    fun maskPan(pan: String): String {
        if (pan.length < 13) return pan
        return "${pan.take(6)}${"*".repeat(pan.length - 10)}${pan.takeLast(4)}"
    }
}
