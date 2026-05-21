package com.mameen.isomessage.ui.screens.refund

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.PaymentResult
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.UiState
import com.mameen.isomessage.domain.usecase.ProcessRefundUseCase
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class RefundViewModel @Inject constructor(
    private val refundUseCase: ProcessRefundUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<Transaction>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    var pan by mutableStateOf("5413330089010012")
    var amount by mutableStateOf("5.00")
    var originalRrn by mutableStateOf("")

    fun processRefund() {
        val amountCents = (amount.toDoubleOrNull() ?: 0.0).times(100).toLong()
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = refundUseCase(
                pan = pan.filter { it.isDigit() },
                amountCents = amountCents,
                originalRrn = originalRrn
            )) {
                is PaymentResult.Success -> UiState.Success(result.transaction)
                is PaymentResult.ValidationError -> UiState.Error("${result.field}: ${result.message}")
                is PaymentResult.NetworkError -> UiState.Error(result.message, result.isRetryable)
                is PaymentResult.Offline -> UiState.Error(result.message, false)
                is PaymentResult.UnknownError -> UiState.Error(result.message ?: "Unknown error")
            }
        }
    }
    fun resetState() { _uiState.value = UiState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun RefundScreen(
    onBack: () -> Unit,
    onTransactionComplete: (String) -> Unit,
    viewModel: RefundViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onTransactionComplete((uiState as UiState.Success).data.id)
        }
    }

    PosScaffold(title = "Refund", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = PosAmber400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Refund (Credit Return) — Processing Code: 200000\n" +
                            "Returns funds to cardholder AFTER settlement has occurred.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextSecondary
                        )
                    }
                }
            }
            item { SectionHeader("Card & Amount", "Enter original card and refund amount") }
            item {
                PosCard {
                    OutlinedTextField(
                        value = viewModel.pan,
                        onValueChange = { viewModel.pan = it.filter { c -> c.isDigit() }.take(19) },
                        label = { Text("Card Number (PAN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = posTextFieldColors(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.amount,
                        onValueChange = { viewModel.amount = it },
                        label = { Text("Refund Amount") },
                        prefix = { Text("EGP ", color = PosTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = posTextFieldColors(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.originalRrn,
                        onValueChange = { viewModel.originalRrn = it.take(12) },
                        label = { Text("Original RRN (optional)") },
                        placeholder = { Text("DE37 from original transaction") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = posTextFieldColors(),
                        singleLine = true
                    )
                }
            }
            if (uiState is UiState.Error) {
                item {
                    ErrorCard((uiState as UiState.Error).message)
                }
            }
            item {
                PaymentButton(
                    text = "Send Refund Request  →  MTI 0200 (PC:200000)",
                    onClick = { viewModel.processRefund() },
                    isLoading = uiState is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Undo,
                    color = PosGreen500
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2A0A0A))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = PosRed500)
            Spacer(Modifier.width(8.dp))
            Text(message, color = PosTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun posTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PosGreen500,
    unfocusedBorderColor = PosNavy500,
    focusedLabelColor = PosGreen500,
    unfocusedLabelColor = PosTextSecondary,
    cursorColor = PosGreen500,
    focusedTextColor = PosTextPrimary,
    unfocusedTextColor = PosTextPrimary
)
