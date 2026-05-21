package com.mameen.isomessage.security

import kotlin.random.Random

/**
 * Fake Security Simulator (Educational Only)
 *
 * This class simulates the security operations that occur in real payment terminals.
 * ALL values generated here are FAKE and NOT cryptographically secure.
 *
 * ⚠️ DO NOT use any of this code in a real payment system.
 * Real payment security uses:
 * - HSMs (Hardware Security Modules) for key management
 * - 3DES or AES encryption for PIN blocks
 * - HMAC-SHA256 or CMAC for MACs
 * - EMV ARQC/TC/AAC cryptograms via certified EMV kernels
 * - PCI HSM certified hardware
 *
 * Purpose: Education and demonstration of payment security CONCEPTS.
 */
object FakeSecuritySimulator {

    /**
     * Generate a fake PIN Block (ISO 9564 Format 0 / Format 1)
     *
     * Real PIN block construction:
     * - Format 0 (ISO): PIN block = PIN data XOR PAN data
     *   PIN data:  0 + PIN_length + PIN_digits + padding (F's)
     *   PAN data:  0000 + rightmost 12 digits of PAN (excluding check digit)
     * - Result is 8 bytes (16 hex chars)
     * - This is then encrypted with 3DES under the PIN Encryption Key (PEK)
     *   before transmission — the terminal NEVER sends plaintext PINs.
     *
     * @param pin The cardholder's PIN (4-8 digits)
     * @param pan The card PAN
     * @return Fake PIN block hex string (NOT encrypted, NOT usable in production)
     */
    fun generateFakePinBlock(pin: String = "1234", pan: String = "5413330089010012"): String {
        // Step 1: Build PIN data field
        // Format: 0 + length(1) + PIN digits + F padding to 16 hex chars
        val pinData = "0${pin.length}${pin}${"F".repeat(14 - pin.length)}"

        // Step 2: Build PAN data field
        // Format: 0000 + 12 rightmost PAN digits (excluding check digit)
        val panDigits = pan.dropLast(1).takeLast(12)
        val panData = "0000$panDigits"

        // Step 3: XOR the two (simulated — real would then 3DES encrypt this)
        val pinBytes = HexUtils.hexToBytes(pinData)
        val panBytes = HexUtils.hexToBytes(panData)
        val xorResult = HexUtils.xorBytes(pinBytes, panBytes)

        return HexUtils.bytesToHex(xorResult)
    }

    /**
     * Generate a fake MAC (Message Authentication Code)
     *
     * Real MAC calculation in ISO8583:
     * - Takes selected DE fields (defined by the card scheme spec)
     * - Applies HMAC-SHA256 or CMAC-AES128 using the MAC Session Key
     * - Session key is derived from the Master Session Key loaded in the terminal's HSM
     * - MAC protects message integrity — any modification changes the MAC
     * - DE64 (primary) or DE128 (secondary) carries the MAC in the ISO message
     *
     * @return Fake 8-byte MAC as hex string
     */
    fun generateFakeMac(messageData: String = ""): String {
        // Fake: just XOR the data bytes with a static "key" and take last 8 bytes
        val fakeKey = "0102030405060708"
        val keyBytes = HexUtils.hexToBytes(fakeKey)
        val dataBytes = messageData.toByteArray()

        val macBytes = ByteArray(8)
        dataBytes.forEachIndexed { i, b ->
            macBytes[i % 8] = (macBytes[i % 8].toInt() xor b.toInt() xor keyBytes[i % 8].toInt()).toByte()
        }

        return HexUtils.bytesToHex(macBytes)
    }

    /**
     * Simulate a session key derivation (educational)
     *
     * Real session key derivation (per EMV/Visa/MC spec):
     * 1. Start with a Master Derivation Key (loaded in HSM at personalisation)
     * 2. Derive Unique Derivation Key per card using ATC
     * 3. Derive Session Key from UDK using random diversification data
     *
     * Each transaction uses a fresh session key, so even if one transaction
     * is compromised, the attacker cannot decrypt past or future transactions.
     *
     * @return Fake 16-byte session key as hex
     */
    fun generateFakeSessionKey(atc: Int = Random.nextInt(1, 9999)): FakeSessionKey {
        val fakeMasterKey = "00112233445566778899AABBCCDDEEFF"
        val atcHex = "%04X".format(atc)

        // Fake derivation — just XOR chunks of master key with ATC
        val masterBytes = HexUtils.hexToBytes(fakeMasterKey)
        val derivedBytes = ByteArray(16) { i ->
            if (i < 2) (masterBytes[i].toInt() xor atcHex[i % 4].code).toByte()
            else masterBytes[i]
        }

        return FakeSessionKey(
            keyHex = HexUtils.bytesToHex(derivedBytes),
            atc = atc,
            description = "Derived from fake master key with ATC=0x$atcHex. " +
                    "Real keys use 3DES/AES derivation in a PCI-certified HSM."
        )
    }

    /**
     * Simulate terminal key exchange (DUKPT — Derived Unique Key Per Transaction)
     *
     * DUKPT is the standard key management scheme for POS terminals.
     * Each terminal is loaded with a Base Derivation Key (BDK) during injection.
     * For each transaction, a unique key is derived using the terminal's KSN
     * (Key Serial Number, which includes the terminal ID + transaction counter).
     * Once a key is used, it's discarded — it can never be used again.
     */
    fun simulateDukptKeyInfo(terminalId: String = "TERM0001"): DukptKeyInfo {
        val ksn = "${terminalId.take(8).padEnd(8, '0')}${"%010X".format(Random.nextLong(0, 0xFFFFFFFFF))}"
        return DukptKeyInfo(
            ksn = ksn,
            currentKey = HexUtils.bytesToHex(Random.nextBytes(16)),
            transactionCount = Random.nextInt(1, 9999),
            description = "DUKPT Key Serial Number. The KSN is transmitted with the PIN block " +
                    "so the HSM can derive the correct transaction key to decrypt the PIN."
        )
    }
}

data class FakeSessionKey(
    val keyHex: String,
    val atc: Int,
    val description: String
)

data class DukptKeyInfo(
    val ksn: String,
    val currentKey: String,
    val transactionCount: Int,
    val description: String
)
