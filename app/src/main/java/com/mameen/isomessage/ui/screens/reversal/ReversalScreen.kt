package com.mameen.isomessage.ui.screens.reversal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.PaymentResult
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.UiState
import com.mameen.isomessage.domain.usecase.ProcessReversalUseCase
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReversalViewModel @Inject constructor(
    private val reversalUseCase: ProcessReversalUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<Transaction>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    var pan by mutableStateOf("5413330089010012")
    var amount by mutableStateOf("10.00")
    var originalStan by mutableStateOf("")
    var originalRrn by mutableStateOf("")

    fun processReversal() {
        val amountCents = (amount.toDoubleOrNull() ?: 0.0).times(100).toLong()
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = reversalUseCase(
                pan = pan.filter { it.isDigit() },
                amountCents = amountCents,
                originalStan = originalStan,
                originalRrn = originalRrn
            )) {
                is PaymentResult.Success -> UiState.Success(r.transaction)
                is PaymentResult.ValidationError -> UiState.Error("${r.field}: ${r.message}")
                is PaymentResult.NetworkError -> UiState.Error(r.message, r.isRetryable)
                is PaymentResult.Offline -> UiState.Error(r.message, false)
                is PaymentResult.UnknownError -> UiState.Error(r.message ?: "Error")
            }
        }
    }
}

@Composable
fun ReversalScreen(
    onBack: () -> Unit,
    onTransactionComplete: (String) -> Unit,
    viewModel: ReversalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) onTransactionComplete((uiState as UiState.Success).data.id)
    }

    PosScaffold(title = "Reversal", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = PosAmber400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reversal — MTI 0400\nUndoes a previous 0200 transaction BEFORE settlement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextSecondary
                        )
                    }
                }
            }
            item { SectionHeader("Reversal Details", "Original transaction data is required") }
            item {
                PosCard {
                    val colors = textColors()
                    OutlinedTextField(
                        value = viewModel.pan,
                        onValueChange = { viewModel.pan = it.filter { c -> c.isDigit() }.take(19) },
                        label = { Text("Card Number (PAN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), colors = colors, singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.amount,
                        onValueChange = { viewModel.amount = it },
                        label = { Text("Original Amount") },
                        prefix = { Text("EGP ", color = PosTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), colors = colors, singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.originalStan,
                        onValueChange = { viewModel.originalStan = it.take(6) },
                        label = { Text("Original STAN (DE11) *") },
                        placeholder = { Text("6-digit system trace number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), colors = colors, singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.originalRrn,
                        onValueChange = { viewModel.originalRrn = it.take(12) },
                        label = { Text("Original RRN (DE37)") },
                        modifier = Modifier.fillMaxWidth(), colors = colors, singleLine = true
                    )
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
                    text = "Send Reversal  →  MTI 0400",
                    onClick = { viewModel.processReversal() },
                    isLoading = uiState is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Replay,
                    color = PosAmber400
                )
            }
        }
    }
}

@Composable
private fun textColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PosAmber400,
    unfocusedBorderColor = PosNavy500,
    focusedLabelColor = PosAmber400,
    unfocusedLabelColor = PosTextSecondary,
    cursorColor = PosAmber400,
    focusedTextColor = PosTextPrimary,
    unfocusedTextColor = PosTextPrimary
)
