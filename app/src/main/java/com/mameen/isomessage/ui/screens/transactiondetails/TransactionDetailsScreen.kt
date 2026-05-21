package com.mameen.isomessage.ui.screens.transactiondetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.repository.PaymentRepository
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {
    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction = _transaction.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch { _transaction.value = repository.getTransactionById(id) }
    }
}

@Composable
fun TransactionDetailsScreen(
    transactionId: String,
    onBack: () -> Unit,
    onViewIso: (String) -> Unit,
    onViewReceipt: (String) -> Unit,
    viewModel: TransactionDetailsViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    LaunchedEffect(transactionId) { viewModel.load(transactionId) }

    PosScaffold(
        title = "Transaction Details",
        onBack = onBack
    ) { padding ->
        transaction?.let { txn ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Summary ──────────────────────────────────────────────────
                item {
                    PosCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(txn.type.displayName, style = MaterialTheme.typography.titleMedium, color = PosTextPrimary)
                                Text(txn.formattedTimestamp, style = MaterialTheme.typography.bodySmall, color = PosTextSecondary)
                            }
                            StatusBadge(txn.status)
                        }
                        Spacer(Modifier.height(12.dp))
                        AmountDisplay(txn.amountFormatted.substringBefore(" "), txn.currency)
                    }
                }

                // ── ISO8583 Fields ────────────────────────────────────────────
                item { SectionHeader("ISO8583 Message Info", "Key fields from the transaction") }
                item {
                    PosCard {
                        InfoRow("MTI Request", txn.mtiRequest, monospace = true)
                        InfoRow("MTI Response", txn.mtiResponse, monospace = true)
                        InfoRow("STAN (DE11)", txn.stan, monospace = true)
                        InfoRow("RRN (DE37)", txn.rrn.ifBlank { "—" }, monospace = true)
                        InfoRow("Auth Code (DE38)", txn.authCode.ifBlank { "—" }, monospace = true)
                        InfoRow("Response Code (DE39)", "${txn.responseCode} — ${txn.responseMessage}", valueColor = if (txn.isApproved) PosGreen500 else PosRed500)
                        InfoRow("Terminal (DE41)", txn.terminalId.trim(), monospace = true)
                        InfoRow("Merchant (DE42)", txn.merchantId.trim(), monospace = true)
                        InfoRow("Currency (DE49)", txn.currencyCode, monospace = true)
                        InfoRow("POS Entry Mode (DE22)", "${txn.posEntryMode.code} — ${txn.posEntryMode.displayName}", monospace = true)
                    }
                }

                // ── Card Info ─────────────────────────────────────────────────
                item { SectionHeader("Card Information") }
                item {
                    PosCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CardBrandBadge(txn.cardBrand)
                            Spacer(Modifier.width(8.dp))
                            Text(txn.pan, style = MaterialTheme.typography.bodyMedium, color = PosTextPrimary)
                        }
                    }
                }

                // ── Actions ───────────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onViewIso(txn.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosCyan400)
                        ) {
                            Icon(Icons.Default.DataObject, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ISO Viewer")
                        }
                        OutlinedButton(
                            onClick = { onViewReceipt(txn.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosGreen500)
                        ) {
                            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Receipt")
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PosCyan400)
        }
    }
}
