package com.mameen.isomessage.iso8583

import java.util.Collections.emptySet

/**
 * ISO8583 Bitmap
 *
 * The bitmap is a 64-bit (primary) or 128-bit (primary + secondary) binary field
 * that acts as a "presence map" — each bit indicates whether a specific Data Element
 * is present in the message.
 *
 * Bit 1 (leftmost) of the PRIMARY bitmap indicates whether a SECONDARY bitmap exists.
 * If bit 1 = 1, there are 128 possible DEs (DE1–DE128).
 * If bit 1 = 0, only DE2–DE64 are possible.
 *
 * Example: Primary bitmap = 7230000000000000 (hex)
 *   Binary: 0111 0010 0011 0000 0000 0000 0000 0000 ...
 *   Bit 1=0 (no secondary), Bit 2=1 (DE2 present), Bit 3=1 (DE3 present), etc.
 *
 * This visual representation is one of the most educational parts of ISO8583.
 */
class BitMap(private val presentDEs: Set<Int> = emptySet()) {

    /**
     * Whether a secondary bitmap is needed (DEs 65–128 present)
     */
    val hasSecondaryBitmap: Boolean get() = presentDEs.any { it > 64 }

    /**
     * Encode the bitmap to a hex string.
     * Returns 16 hex chars (8 bytes) for primary only, or 32 hex chars (16 bytes) if secondary.
     */
    fun toHexString(): String {
        val bytes = ByteArray(if (hasSecondaryBitmap) 16 else 8)

        // If secondary bitmap exists, set bit 1 of primary
        if (hasSecondaryBitmap) {
            bytes[0] = (bytes[0].toInt() or 0x80).toByte()
        }

        for (de in presentDEs) {
            val adjustedDe = de - 1  // Convert to 0-based index
            val byteIndex = adjustedDe / 8
            val bitIndex = 7 - (adjustedDe % 8)  // MSB first
            if (byteIndex < bytes.size) {
                bytes[byteIndex] = (bytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
            }
        }

        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * Produce a visual grid representation of the bitmap for educational display.
     * Each cell shows the DE number and whether it's set (■) or unset (□).
     */
    fun toVisualGrid(): List<BitMapRow> {
        val rows = mutableListOf<BitMapRow>()
        val totalBits = if (hasSecondaryBitmap) 128 else 64
        val bitmapHex = toHexString()

        for (byteIdx in 0 until (totalBits / 8)) {
            val hexByte = bitmapHex.substring(byteIdx * 2, byteIdx * 2 + 2)
            val byteValue = hexByte.toInt(16)
            val bits = mutableListOf<BitCell>()

            for (bitIdx in 7 downTo 0) {
                val deNumber = byteIdx * 8 + (7 - bitIdx) + 1
                val isSet = (byteValue shr bitIdx) and 1 == 1
                bits.add(BitCell(deNumber, isSet))
            }

            rows.add(BitMapRow(byteIndex = byteIdx + 1, hexByte = hexByte, bits = bits))
        }
        return rows
    }

    companion object {
        /**
         * Parse a hex bitmap string and return the set of present DE numbers.
         */
        fun parseHex(hex: String): BitMap {
            val presentDEs = mutableSetOf<Int>()
            val normalizedHex = hex.uppercase().replace(" ", "")

            normalizedHex.chunked(2).forEachIndexed { byteIdx, hexByte ->
                val byteValue = hexByte.toInt(16)
                for (bitIdx in 7 downTo 0) {
                    if ((byteValue shr bitIdx) and 1 == 1) {
                        val deNumber = byteIdx * 8 + (7 - bitIdx) + 1
                        // Bit 1 = secondary bitmap indicator, not a real DE
                        if (deNumber != 1 || byteIdx != 0) {
                            presentDEs.add(deNumber)
                        }
                    }
                }
            }
            return BitMap(presentDEs)
        }

        /**
         * Build a BitMap from a set of DE numbers.
         */
        fun fromDEs(des: Set<Int>): BitMap = BitMap(des)
    }
}

data class BitMapRow(
    val byteIndex: Int,
    val hexByte: String,
    val bits: List<BitCell>
)

data class BitCell(
    val deNumber: Int,
    val isSet: Boolean
)
