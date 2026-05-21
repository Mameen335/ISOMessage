package com.mameen.isomessage.ui.screens.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mameen.isomessage.domain.model.UiState
import com.mameen.isomessage.security.HexUtils
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import androidx.compose.ui.graphics.Color

@Composable
fun PurchaseScreen(
    onBack: () -> Unit,
    onTransactionComplete: (String) -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.formState.collectAsStateWithLifecycle()

    // Navigate to receipt on success
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val txn = (uiState as UiState.Success).data
            onTransactionComplete(txn.id)
        }
    }

    PosScaffold(
        title = "Purchase",
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Card Information ─────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Card Information",
                    subtitle = "DE2 — Primary Account Number"
                )
            }

            item {
                PosCard {
                    OutlinedTextField(
                        value = form.pan,
                        onValueChange = { viewModel.updatePan(it.filter { c -> c.isDigit() }.take(19)) },
                        label = { Text("Card Number (PAN)") },
                        placeholder = { Text("16-digit card number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = posTextFieldColors(),
                        trailingIcon = {
                            if (form.pan.length >= 13) {
                                val isValid = HexUtils.isValidLuhn(form.pan)
                                Icon(
                                    if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isValid) PosGreen500 else PosRed500
                                )
                            }
                        },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    if (form.pan.length >= 6) {
                        Text(
                            text = HexUtils.maskPan(form.pan),
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextHint,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ── Transaction Amount ────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Transaction Amount",
                    subtitle = "DE4 — Amount in EGP (tip: >50 triggers decline)"
                )
            }

            item {
                PosCard {
                    OutlinedTextField(
                        value = form.amount,
                        onValueChange = { viewModel.updateAmount(it) },
                        label = { Text("Amount") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("EGP ", color = PosTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = posTextFieldColors(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(4.dp))
                    val cents = (form.amount.toDoubleOrNull() ?: 0.0).times(100).toLong()
                    Text(
                        text = "ISO8583 DE4 value: ${cents.toString().padStart(12, '0')}  (12-digit, minor units)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PosTextHint,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ── POS Entry Mode ────────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Card Entry Method",
                    subtitle = "DE22 — Point of Service Entry Mode"
                )
            }

            item {
                PosCard {
                    val modes = listOf(
                        "051" to "🔵 Chip (ICC)",
                        "071" to "📡 Contactless (NFC)",
                        "021" to "💳 Magnetic Stripe",
                        "011" to "⌨️ Manual Entry"
                    )
                    modes.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = form.posEntryMode == code,
                                onClick = { viewModel.updatePosEntryMode(code) },
                                colors = RadioButtonDefaults.colors(selectedColor = PosCyan400)
                            )
                            Text(
                                text = "$label  (DE22=$code)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PosTextPrimary
                            )
                        }
                    }
                }
            }

            // ── Simulation Controls ───────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Simulation Controls",
                    subtitle = "These settings are for testing different scenarios"
                )
            }

            item {
                PosCard {
                    SwitchRow(
                        label = "Include EMV/Chip Data (DE55)",
                        sublabel = "Adds fake TLV data to simulate chip transaction",
                        checked = form.includeEmvData,
                        onCheckedChange = { viewModel.toggleEmvData(it) }
                    )
                    HorizontalDivider(color = PosNavy600, modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow(
                        label = "Simulate Decline (amount → EGP 100)",
                        sublabel = "Mockoon declines amounts over EGP 50",
                        checked = form.simulateDecline,
                        onCheckedChange = { viewModel.toggleSimulateDecline(it) }
                    )
                    HorizontalDivider(color = PosNavy600, modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow(
                        label = "Simulate Timeout (RC=68)",
                        sublabel = "Host returns timeout after 5s delay",
                        checked = form.simulateTimeout,
                        onCheckedChange = { viewModel.toggleSimulateTimeout(it) }
                    )
                }
            }

            // ── Error Message ─────────────────────────────────────────────────
            if (uiState is UiState.Error) {
                item {
                    val error = uiState as UiState.Error
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0A0A))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = PosRed500)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Transaction Failed",
                                    color = PosRed500,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(error.message, color = PosTextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ── Submit Button ─────────────────────────────────────────────────
            item {
                PaymentButton(
                    text = "Send Purchase Request  →  MTI 0200",
                    onClick = { viewModel.processPurchase() },
                    isLoading = uiState is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Send,
                    color = PosCyan400
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = PosTextPrimary)
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = PosTextHint)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PosNavy900,
                checkedTrackColor = PosCyan400
            )
        )
    }
}

@Composable
private fun posTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PosCyan400,
    unfocusedBorderColor = PosNavy500,
    focusedLabelColor = PosCyan400,
    unfocusedLabelColor = PosTextSecondary,
    cursorColor = PosCyan400,
    focusedTextColor = PosTextPrimary,
    unfocusedTextColor = PosTextPrimary
)

