package com.mameen.isomessage.iso8583

/**
 * ISO8583 Data Element definitions
 *
 * Each DE has:
 *  - number       : DE number (1–128, or 129–192 for secondary/tertiary bitmaps)
 *  - name         : human-readable field name
 *  - type         : encoding type (N=numeric, AN=alphanumeric, ANS=alphanumeric+special, B=binary, Z=track-2)
 *  - length       : fixed length, or max length if variable
 *  - lengthType   : FIXED, LLVAR (2-digit prefix), LLLVAR (3-digit prefix)
 *  - description  : educational explanation of what this field carries and WHY it exists
 *  - example      : sample value so developers can understand formatting
 *
 * We implement the most common DEs for a POS scenario. Real ISO8583 has up to 192 DEs
 * across primary, secondary and tertiary bitmaps.
 */
data class DataElementDefinition(
    val number: Int,
    val name: String,
    val type: DeType,
    val length: Int,
    val lengthType: LengthType,
    val description: String,
    val example: String
)

enum class DeType { N, AN, ANS, B, Z }
enum class LengthType { FIXED, LLVAR, LLLVAR }

/**
 * A parsed/populated Data Element value in an ISO message.
 */
data class DataElement(
    val number: Int,
    val value: String,
    val definition: DataElementDefinition? = IsoDataElements.getDefinition(number)
)

/**
 * Registry of ISO8583 Data Element definitions for the fields used in POS scenarios.
 *
 * Real-world implementations use a full DE table from the card scheme specification
 * (Visa, Mastercard, Amex, etc.). This covers the most important DE fields.
 */
object IsoDataElements {

    private val definitions = listOf(
        DataElementDefinition(
            number = 1,
            name = "Secondary Bitmap",
            type = DeType.B,
            length = 8,
            lengthType = LengthType.FIXED,
            description = "Indicates presence of a secondary bitmap (DE65–DE128). " +
                    "Bit 1 of the primary bitmap is always 1 if DE1 (secondary bitmap) exists.",
            example = "8000000000000000"
        ),
        DataElementDefinition(
            number = 2,
            name = "Primary Account Number (PAN)",
            type = DeType.N,
            length = 19,
            lengthType = LengthType.LLVAR,
            description = "The card number. First 6 digits = BIN (Bank Identification Number). " +
                    "Last digit = Luhn check digit. This identifies WHICH card was used. " +
                    "In real processing, the PAN is encrypted or tokenised to protect cardholder data.",
            example = "5413330089010012"
        ),
        DataElementDefinition(
            number = 3,
            name = "Processing Code",
            type = DeType.N,
            length = 6,
            lengthType = LengthType.FIXED,
            description = "6-digit code describing what financial action to perform. " +
                    "Positions 1-2 = transaction type (00=Purchase, 20=Refund, 90=Balance Inquiry). " +
                    "Positions 3-4 = from account type (00=default). " +
                    "Positions 5-6 = to account type (00=default).",
            example = "000000"
        ),
        DataElementDefinition(
            number = 4,
            name = "Transaction Amount",
            type = DeType.N,
            length = 12,
            lengthType = LengthType.FIXED,
            description = "Transaction amount in the smallest currency unit (e.g., cents for USD/EGP). " +
                    "Always 12 digits, zero-padded on the left. Amount 10.00 EGP → '000000001000'. " +
                    "DE49 (Currency Code) tells you which currency this amount is in.",
            example = "000000001000"
        ),
        DataElementDefinition(
            number = 7,
            name = "Transmission Date and Time",
            type = DeType.N,
            length = 10,
            lengthType = LengthType.FIXED,
            description = "Date and time the message was sent by the originator. " +
                    "Format: MMDDHHmmss. Used for audit trails and duplicate detection.",
            example = "0517143022"
        ),
        DataElementDefinition(
            number = 11,
            name = "System Trace Audit Number (STAN)",
            type = DeType.N,
            length = 6,
            lengthType = LengthType.FIXED,
            description = "A unique 6-digit sequence number assigned by the terminal or acquirer host " +
                    "to each transaction. Used to match requests with responses and for audit trails. " +
                    "The SAME STAN must appear in both the request (0200) and response (0210).",
            example = "123456"
        ),
        DataElementDefinition(
            number = 12,
            name = "Time, Local Transaction",
            type = DeType.N,
            length = 6,
            lengthType = LengthType.FIXED,
            description = "Local time at the terminal when the transaction occurred. " +
                    "Format: HHmmss.",
            example = "143022"
        ),
        DataElementDefinition(
            number = 13,
            name = "Date, Local Transaction",
            type = DeType.N,
            length = 4,
            lengthType = LengthType.FIXED,
            description = "Local date at the terminal. Format: MMDD.",
            example = "0517"
        ),
        DataElementDefinition(
            number = 14,
            name = "Date, Expiration",
            type = DeType.N,
            length = 4,
            lengthType = LengthType.FIXED,
            description = "Card expiry date. Format: YYMM. Used to validate the card hasn't expired.",
            example = "2612"
        ),
        DataElementDefinition(
            number = 22,
            name = "Point of Service Entry Mode",
            type = DeType.N,
            length = 3,
            lengthType = LengthType.FIXED,
            description = "How the card data was captured. " +
                    "First 2 digits = PAN entry mode (01=Manual, 05=ICC/Chip, 07=Contactless NFC, 02=Magnetic Stripe). " +
                    "Third digit = PIN entry capability (0=Unknown, 1=Terminal can accept PIN, 2=Terminal cannot accept PIN). " +
                    "This affects interchange rates — chip transactions are cheaper than swipe.",
            example = "051"
        ),
        DataElementDefinition(
            number = 35,
            name = "Track 2 Data",
            type = DeType.Z,
            length = 37,
            lengthType = LengthType.LLVAR,
            description = "Magnetic stripe Track 2 data. Contains PAN, expiry, service code. " +
                    "Format: PAN=ExpiryYYMM=ServiceCode. NEVER store this data — PCI DSS prohibits it.",
            example = "5413330089010012=26120000000000000"
        ),
        DataElementDefinition(
            number = 37,
            name = "Retrieval Reference Number (RRN)",
            type = DeType.AN,
            length = 12,
            lengthType = LengthType.FIXED,
            description = "Unique reference number assigned by the acquirer. " +
                    "Used to identify and retrieve the transaction later (e.g., for reversals, chargebacks). " +
                    "Appears on receipts and in reconciliation files.",
            example = "202405170001"
        ),
        DataElementDefinition(
            number = 38,
            name = "Authorization Identification Response",
            type = DeType.AN,
            length = 6,
            lengthType = LengthType.FIXED,
            description = "The authorization code returned by the issuer for approved transactions. " +
                    "This 6-character code is printed on the receipt and proves the issuer approved the transaction. " +
                    "Empty or spaces for declined transactions.",
            example = "AUTH01"
        ),
        DataElementDefinition(
            number = 39,
            name = "Response Code",
            type = DeType.AN,
            length = 2,
            lengthType = LengthType.FIXED,
            description = "The most important field in the response! " +
                    "00 = Approved. 51 = Insufficient Funds. 05 = Do Not Honour. " +
                    "14 = Invalid Card Number. 54 = Expired Card. 55 = Incorrect PIN. " +
                    "61 = Exceeds Withdrawal Amount Limit. 68 = Response Received Too Late (timeout). " +
                    "The terminal shows the customer-facing message based on this code.",
            example = "00"
        ),
        DataElementDefinition(
            number = 41,
            name = "Card Acceptor Terminal ID",
            type = DeType.ANS,
            length = 8,
            lengthType = LengthType.FIXED,
            description = "The 8-character unique ID of the POS terminal. " +
                    "Assigned by the acquirer when the terminal is registered. " +
                    "Used for terminal identification, routing, and fraud detection.",
            example = "TERM0001"
        ),
        DataElementDefinition(
            number = 42,
            name = "Card Acceptor Identification Code (Merchant ID)",
            type = DeType.ANS,
            length = 15,
            lengthType = LengthType.FIXED,
            description = "The 15-character unique merchant ID assigned by the acquirer. " +
                    "Identifies WHICH merchant processed the transaction. " +
                    "Used for merchant settlement, reporting, and fraud analysis.",
            example = "MERCHANT0000001"
        ),
        DataElementDefinition(
            number = 49,
            name = "Transaction Currency Code",
            type = DeType.N,
            length = 3,
            lengthType = LengthType.FIXED,
            description = "ISO 4217 numeric currency code. " +
                    "840 = USD, 818 = EGP (Egyptian Pound), 978 = EUR, 826 = GBP. " +
                    "Combined with DE4 (Amount) to know the actual monetary value.",
            example = "818"
        ),
        DataElementDefinition(
            number = 55,
            name = "ICC Data (EMV/Chip Data)",
            type = DeType.B,
            length = 255,
            lengthType = LengthType.LLLVAR,
            description = "Contains EMV chip data in TLV (Tag-Length-Value) format. " +
                    "This is the heart of EMV security — contains the cryptogram (ARQC) " +
                    "generated by the card's secure element, proving the physical card was present. " +
                    "The issuer validates this cryptogram to prevent counterfeit fraud. " +
                    "Required for chip transactions to qualify for liability shift protection.",
            example = "9F260812345678901234569F2701809F101307A0000000041010200000000000000000"
        ),
        DataElementDefinition(
            number = 60,
            name = "Additional POS Data",
            type = DeType.ANS,
            length = 7,
            lengthType = LengthType.LLLVAR,
            description = "Additional terminal and transaction data. " +
                    "Content varies by card scheme and acquirer specification.",
            example = "POS0001"
        )
    )

    fun getDefinition(de: Int): DataElementDefinition? = definitions.find { it.number == de }
    fun getAllDefinitions(): List<DataElementDefinition> = definitions

    /**
     * Processing codes for the most common transaction types (DE3).
     */
    val processingCodes = mapOf(
        "000000" to "Purchase",
        "200000" to "Refund / Return",
        "900000" to "Balance Inquiry",
        "010000" to "Cash Advance",
        "280000" to "Void"
    )

    /**
     * Response codes (DE39) with customer-facing messages.
     */
    val responseCodes = mapOf(
        "00" to "Approved",
        "01" to "Refer to Card Issuer",
        "03" to "Invalid Merchant",
        "04" to "Pick Up Card",
        "05" to "Do Not Honour",
        "12" to "Invalid Transaction",
        "13" to "Invalid Amount",
        "14" to "Invalid Card Number",
        "30" to "Format Error",
        "40" to "Invalid Amount (Format Error)",
        "41" to "Lost Card",
        "43" to "Stolen Card",
        "51" to "Insufficient Funds",
        "54" to "Expired Card",
        "55" to "Incorrect PIN",
        "57" to "Transaction Not Permitted to Cardholder",
        "61" to "Exceeds Withdrawal Amount Limit",
        "62" to "Restricted Card",
        "65" to "Exceeds Withdrawal Frequency Limit",
        "68" to "Response Received Too Late (Timeout)",
        "75" to "PIN Tries Exceeded",
        "91" to "Issuer Unavailable",
        "96" to "System Malfunction"
    )
}
