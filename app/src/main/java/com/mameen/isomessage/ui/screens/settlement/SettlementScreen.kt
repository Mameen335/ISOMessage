package com.mameen.isomessage.ui.screens.settlement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.PaymentResult
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.UiState
import com.mameen.isomessage.domain.usecase.ProcessSettlementUseCase
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val settlementUseCase: ProcessSettlementUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<Transaction>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun processSettlement(terminalId: String = "TERM0001") {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = settlementUseCase(terminalId)) {
                is PaymentResult.Success -> UiState.Success(r.transaction)
                is PaymentResult.NetworkError -> UiState.Error(r.message, r.isRetryable)
                is PaymentResult.Offline -> UiState.Error(r.message, false)
                else -> UiState.Error("Settlement failed")
            }
        }
    }
}

@Composable
fun SettlementScreen(
    onBack: () -> Unit,
    onTransactionComplete: (String) -> Unit,
    viewModel: SettlementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) onTransactionComplete((uiState as UiState.Success).data.id)
    }

    PosScaffold(title = "End-of-Day Settlement", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PosCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.AccountBalance, null, tint = IsoPurple, modifier = Modifier.size(24.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("End-of-Day Settlement", style = MaterialTheme.typography.titleSmall, color = IsoPurple, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Sends all pending transactions to the acquirer for clearing.\n\n" +
                                "MTI: 0500 → 0510 (Reconciliation Request/Response)\n" +
                                "The host verifies totals match the terminal's batch.\n" +
                                "RC=95 = Out of Balance, RC=00 = Balanced.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PosTextSecondary
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Settlement Summary (Demo)", "Simulated daily totals") }
            item {
                PosCard {
                    InfoRow("Terminal ID", "TERM0001", monospace = true)
                    InfoRow("Merchant ID", "MERCHANT001    ", monospace = true)
                    InfoRow("Date", java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date()))
                    HorizontalDivider(color = PosNavy600, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Purchases", "EGP 0.00 (0 txn)")
                    InfoRow("Refunds", "EGP 0.00 (0 txn)")
                    InfoRow("Net Total", "EGP 0.00", valueColor = PosCyan400)
                }
            }

            if (uiState is UiState.Error) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2A0A0A))) {
                        Row(Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Error, null, tint = PosRed500)
                            Spacer(Modifier.width(8.dp))
                            Text((uiState as UiState.Error).message, color = PosTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                PaymentButton(
                    text = "Initiate Settlement  →  MTI 0500",
                    onClick = { viewModel.processSettlement() },
                    isLoading = uiState is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.AccountBalance,
                    color = IsoPurple
                )
            }
        }
    }
}
