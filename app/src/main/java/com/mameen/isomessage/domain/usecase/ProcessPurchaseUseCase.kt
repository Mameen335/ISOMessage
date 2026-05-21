package com.mameen.isomessage.domain.usecase

import com.mameen.isomessage.domain.model.PaymentResult
import com.mameen.isomessage.domain.repository.PaymentRepository
import com.mameen.isomessage.emv.De55Builder
import com.mameen.isomessage.iso8583.IsoMessageBuilder
import javax.inject.Inject

/**
 * Use Case: Process a Purchase Transaction
 *
 * Clean Architecture: Use Cases contain business logic.
 * This use case validates input, builds the ISO8583 0200 message,
 * and delegates network communication to the repository.
 *
 * Purchase flow (simplified):
 * 1. Cardholder presents card (swipe/chip/NFC)
 * 2. Terminal reads card data and prompts for PIN
 * 3. Terminal builds ISO8583 0200 request with card + amount + terminal data
 * 4. Terminal sends 0200 to acquirer host → issuer host
 * 5. Issuer validates: card valid? funds available? not blocked? risk rules?
 * 6. Issuer responds with 0210: RC=00 (approved) or RC=51 (declined)
 * 7. Terminal shows result to cardholder and prints receipt
 */
class ProcessPurchaseUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    /**
     * Execute a purchase transaction.
     *
     * @param pan Card PAN (will be masked before logging)
     * @param amountCents Amount in minor units (e.g., 1000 = 10.00 EGP)
     * @param terminalId Terminal identifier
     * @param merchantId Merchant identifier
     * @param currencyCode ISO 4217 numeric code (818 = EGP)
     * @param posEntryMode How card was read (051=chip, 071=NFC, 021=swipe)
     * @param includeEmvData Whether to include fake DE55 EMV data
     * @param simulateTimeout Trigger timeout simulation via Mockoon
     */
    suspend operator fun invoke(
        pan: String,
        amountCents: Long,
        terminalId: String = "TERM0001",
        merchantId: String = "MERCHANT001    ",
        currencyCode: String = "818",
        posEntryMode: String = "051",
        includeEmvData: Boolean = true,
        simulateTimeout: Boolean = false
    ): PaymentResult {
        // Validate inputs
        if (pan.filter { it.isDigit() }.length < 13)
            return PaymentResult.ValidationError("pan", "PAN must be at least 13 digits")
        if (amountCents <= 0)
            return PaymentResult.ValidationError("amount", "Amount must be greater than zero")

        // Build the ISO8583 0200 request
        val emvData = if (includeEmvData) De55Builder.buildFakeDe55(amountCents, currencyCode) else null
        val request = IsoMessageBuilder.purchaseRequest(
            pan = pan,
            amountCents = amountCents,
            terminalId = terminalId,
            merchantId = merchantId,
            currencyCode = currencyCode,
            posEntryMode = posEntryMode,
            emvData = emvData
        )

        return repository.sendTransaction(request, simulateTimeout)
    }
}

/**
 * Use Case: Process a Refund Transaction
 *
 * Refund (also called "Credit Return") reverses a charge to the cardholder's account.
 * Unlike a reversal (which cancels before settlement), a refund happens AFTER settlement.
 * Processing code: 200000 (vs. 000000 for purchase).
 */
class ProcessRefundUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(
        pan: String,
        amountCents: Long,
        originalRrn: String = "",
        terminalId: String = "TERM0001"
    ): PaymentResult {
        if (pan.filter { it.isDigit() }.length < 13)
            return PaymentResult.ValidationError("pan", "PAN must be at least 13 digits")
        if (amountCents <= 0)
            return PaymentResult.ValidationError("amount", "Refund amount must be greater than zero")

        val request = IsoMessageBuilder.refundRequest(
            pan = pan,
            amountCents = amountCents,
            originalRrn = originalRrn,
            terminalId = terminalId
        )
        return repository.sendTransaction(request)
    }
}

/**
 * Use Case: Process a Reversal
 *
 * Reversals undo an approved transaction BEFORE it settles.
 * They must be sent when the terminal approves a transaction but can't complete it
 * (e.g., network drops after approval, printer jams before receipt).
 * MTI: 0400 (vs. 0200 for financial requests).
 */
class ProcessReversalUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(
        pan: String,
        amountCents: Long,
        originalStan: String,
        originalRrn: String,
        terminalId: String = "TERM0001"
    ): PaymentResult {
        if (originalStan.isBlank())
            return PaymentResult.ValidationError("stan", "Original STAN is required for reversal")

        val request = IsoMessageBuilder.reversalRequest(
            pan = pan,
            amountCents = amountCents,
            originalStan = originalStan,
            originalRrn = originalRrn,
            terminalId = terminalId
        )
        return repository.sendReversal(request)
    }
}

/**
 * Use Case: Balance Inquiry
 *
 * Allows the cardholder to check their available balance.
 * Processing code: 900000
 */
class BalanceInquiryUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(
        pan: String,
        terminalId: String = "TERM0001"
    ): PaymentResult {
        if (pan.filter { it.isDigit() }.length < 13)
            return PaymentResult.ValidationError("pan", "PAN must be at least 13 digits")

        val request = IsoMessageBuilder.balanceInquiryRequest(pan, terminalId)
        return repository.sendTransaction(request)
    }
}

/**
 * Use Case: End-of-Day Settlement
 *
 * Submits all pending transactions to the acquirer for clearing.
 * The host reconciles terminal totals against its own records.
 * MTI: 0500 (Reconciliation/Settlement Request)
 */
class ProcessSettlementUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(
        terminalId: String = "TERM0001"
    ): PaymentResult {
        val request = IsoMessageBuilder.settlementRequest(terminalId)
        return repository.sendSettlement(request)
    }
}
