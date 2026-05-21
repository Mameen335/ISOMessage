package com.mameen.isomessage

import com.mameen.isomessage.emv.De55Builder
import com.mameen.isomessage.emv.TlvParser
import com.mameen.isomessage.iso8583.*
import com.mameen.isomessage.security.HexUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ISO8583 Simulator core engine.
 *
 * Tests cover:
 * - MTI parsing and description
 * - IsoMessageBuilder construction
 * - Bitmap encoding/decoding
 * - TLV parsing
 * - Hex utilities (including Luhn validation)
 *
 * Run: ./gradlew :app:test
 */

// ── MTI Tests ─────────────────────────────────────────────────────────────────

class MtiTest {
    @Test
    fun `MTI 0200 is purchase request`() {
        val mti = Mti.fromCode("0200")
        assertEquals(Mti.FINANCIAL_REQUEST, mti)
        assertTrue(mti!!.isRequest)
    }

    @Test
    fun `MTI 0210 is purchase response`() {
        val mti = Mti.fromCode("0210")
        assertEquals(Mti.FINANCIAL_RESPONSE, mti)
        assertFalse(mti!!.isRequest)
    }

    @Test
    fun `MTI unknown code returns null`() {
        assertNull(Mti.fromCode("9999"))
    }

    @Test
    fun `MTI describe class`() {
        assertEquals("Financial", Mti.describeClass("0200"))
        assertEquals("Authorization", Mti.describeClass("0100"))
        assertEquals("Reversal / Chargeback", Mti.describeClass("0400"))
    }

    @Test
    fun `MTI describe function`() {
        assertEquals("Request", Mti.describeFunction("0200"))
        assertEquals("Response", Mti.describeFunction("0210"))
    }
}

// ── IsoMessageBuilder Tests ────────────────────────────────────────────────────

class IsoMessageBuilderTest {
    @Test
    fun `builder creates purchase message correctly`() {
        val msg = IsoMessageBuilder()
            .mti("0200")
            .pan("5413330089010012")
            .processingCode("000000")
            .amount(1000L)
            .stan("123456")
            .terminalId("TERM0001")
            .currencyCode("818")
            .build()

        assertEquals("0200", msg.mti)
        assertEquals("5413330089010012", msg.getField(2))
        assertEquals("000000001000", msg.getField(4))
        assertEquals("TERM0001", msg.getField(41))
    }

    @Test
    fun `amount is zero-padded to 12 digits`() {
        val msg = IsoMessageBuilder().mti("0200").amount(5L).build()
        assertEquals("000000000005", msg.getField(4))
    }

    @Test
    fun `purchase factory sets correct processing code`() {
        val msg = IsoMessageBuilder.purchaseRequest("5413330089010012", 1000L)
        assertEquals("000000", msg.getField(3))
    }

    @Test
    fun `refund factory sets correct processing code`() {
        val msg = IsoMessageBuilder.refundRequest("5413330089010012", 500L, "")
        assertEquals("200000", msg.getField(3))
    }

    @Test
    fun `blank MTI throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            IsoMessageBuilder().build()
        }
    }

    @Test
    fun `maskedPan hides middle digits`() {
        val msg = IsoMessageBuilder().mti("0200").pan("5413330089010012").build()
        assertEquals("541333******0012", msg.maskedPan)
    }

    @Test
    fun `amountFormatted converts cents correctly`() {
        val msg = IsoMessageBuilder().mti("0200").amount(1050L).build()
        assertEquals("10.50", msg.amountFormatted)
    }

    @Test
    fun `RC 00 isApproved`() {
        val msg = IsoMessageBuilder().mti("0210").responseCode("00").build()
        assertTrue(msg.isApproved)
    }

    @Test
    fun `RC 51 isDeclined`() {
        val msg = IsoMessageBuilder().mti("0210").responseCode("51").build()
        assertTrue(msg.isDeclined)
    }
}

// ── Bitmap Tests ──────────────────────────────────────────────────────────────

class BitMapTest {
    @Test
    fun `DE2 presence sets bit 2`() {
        val bitmap = BitMap.fromDEs(setOf(2))
        val hex = bitmap.toHexString()
        // Bit 2 in first byte = 0100_0000 = 0x40
        assertTrue("Expected hex to start with 40, got: $hex", hex.startsWith("40"))
    }

    @Test
    fun `hex encodes to 16 chars for primary bitmap`() {
        val bitmap = BitMap.fromDEs(setOf(2, 3, 4))
        assertEquals(16, bitmap.toHexString().length)
    }

    @Test
    fun `bitmap round-trips through hex`() {
        val des = setOf(2, 3, 4, 11, 39, 41, 49)
        val hex = BitMap.fromDEs(des).toHexString()
        val reparsed = BitMap.parseHex(hex).toHexString()
        assertEquals(hex, reparsed)
    }

    @Test
    fun `visual grid has 8 rows for primary bitmap`() {
        val grid = BitMap.fromDEs(setOf(2, 3)).toVisualGrid()
        assertEquals(8, grid.size)
    }
}

// ── TLV Parser Tests ──────────────────────────────────────────────────────────

class TlvParserTest {
    @Test
    fun `parses single TLV correctly`() {
        val hex = "9F26081234567890123456"
        val result = TlvParser.parse(hex)
        assertEquals(1, result.size)
        assertEquals("9F26", result[0].tagHex)
        assertEquals(8, result[0].length)
    }

    @Test
    fun `fake DE55 parses to multiple objects`() {
        val de55 = De55Builder.buildFakeDe55()
        val objects = TlvParser.parse(de55)
        assertTrue("Should have many tags, got ${objects.size}", objects.size > 5)
    }

    @Test
    fun `ARQC tag 9F26 is present in fake DE55`() {
        val de55 = De55Builder.buildFakeDe55()
        val arqc = TlvParser.parse(de55).find { it.tagHex == "9F26" }
        assertNotNull(arqc)
        assertEquals(8, arqc!!.length)
    }

    @Test
    fun `empty input returns empty list`() {
        assertTrue(TlvParser.parse("").isEmpty())
    }
}

// ── HexUtils Tests ────────────────────────────────────────────────────────────

class HexUtilsTest {
    @Test
    fun `bytesToHex converts correctly`() {
        assertEquals("0102FF", HexUtils.bytesToHex(byteArrayOf(0x01, 0x02, 0xFF.toByte())))
    }

    @Test
    fun `hexToBytes converts correctly`() {
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0xFF.toByte()), HexUtils.hexToBytes("0102FF"))
    }

    @Test
    fun `valid Luhn PAN accepted`() {
        assertTrue(HexUtils.isValidLuhn("5413330089010012"))
        assertTrue(HexUtils.isValidLuhn("4111111111111111"))
    }

    @Test
    fun `invalid Luhn PAN rejected`() {
        assertFalse(HexUtils.isValidLuhn("1234567890123456"))
    }

    @Test
    fun `maskPan shows first 6 and last 4`() {
        assertEquals("541333******0012", HexUtils.maskPan("5413330089010012"))
    }
}

// ── DataElement Tests ─────────────────────────────────────────────────────────

class DataElementTest {
    @Test
    fun `DE2 definition is correct`() {
        val def = IsoDataElements.getDefinition(2)
        assertNotNull(def)
        assertEquals(DeType.N, def!!.type)
        assertEquals(LengthType.LLVAR, def.lengthType)
    }

    @Test
    fun `RC 00 is Approved`() {
        assertEquals("Approved", IsoDataElements.responseCodes["00"])
    }

    @Test
    fun `RC 51 is Insufficient Funds`() {
        assertEquals("Insufficient Funds", IsoDataElements.responseCodes["51"])
    }

    @Test
    fun `Processing code 000000 is Purchase`() {
        assertEquals("Purchase", IsoDataElements.processingCodes["000000"])
    }

    @Test
    fun `Processing code 200000 is Refund`() {
        assertEquals("Refund / Return", IsoDataElements.processingCodes["200000"])
    }
}
