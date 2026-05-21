package com.mameen.isomessage.ui.screens.balance

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.PaymentResult
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.UiState
import com.mameen.isomessage.domain.usecase.BalanceInquiryUseCase
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val balanceUseCase: BalanceInquiryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<Transaction>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()
    var pan by mutableStateOf("5413330089010012")

    fun checkBalance() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = balanceUseCase(pan = pan.filter { it.isDigit() })) {
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
fun BalanceInquiryScreen(
    onBack: () -> Unit,
    onTransactionComplete: (String) -> Unit,
    viewModel: BalanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) onTransactionComplete((uiState as UiState.Success).data.id)
    }

    PosScaffold(title = "Balance Inquiry", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = IsoBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Balance Inquiry — MTI 0200 (PC: 900000)\nChecks available balance without any financial movement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextSecondary
                        )
                    }
                }
            }

            item { SectionHeader("Card Details", "Enter card to check balance") }
            item {
                PosCard {
                    OutlinedTextField(
                        value = viewModel.pan,
                        onValueChange = { viewModel.pan = it.filter { c -> c.isDigit() }.take(19) },
                        label = { Text("Card Number (PAN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IsoBlue, unfocusedBorderColor = PosNavy500,
                            focusedLabelColor = IsoBlue, unfocusedLabelColor = PosTextSecondary,
                            cursorColor = IsoBlue, focusedTextColor = PosTextPrimary, unfocusedTextColor = PosTextPrimary
                        ),
                        singleLine = true
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
                    text = "Check Balance  →  MTI 0200 (PC:900000)",
                    onClick = { viewModel.checkBalance() },
                    isLoading = uiState is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.AccountBalanceWallet,
                    color = IsoBlue
                )
            }
        }
    }
}
