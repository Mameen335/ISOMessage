package com.mameen.isomessage.ui.screens.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.TransactionStatus
import com.mameen.isomessage.domain.repository.PaymentRepository
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {
    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction = _transaction.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _transaction.value = repository.getTransactionById(id)
        }
    }
}

@Composable
fun ReceiptScreen(
    transactionId: String,
    onBack: () -> Unit,
    onViewDetails: (String) -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) { viewModel.load(transactionId) }

    PosScaffold(title = "Receipt", onBack = onBack) { padding ->
        transaction?.let { txn ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { ReceiptCard(txn) }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onViewDetails(txn.id) },
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosCyan400)
                        ) {
                            Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ISO Details")
                        }
                        PaymentButton(
                            text = "New Transaction",
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Add
                        )
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PosCyan400)
        }
    }
}

@Composable
private fun ReceiptCard(txn: Transaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PosCardBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "DEMO MERCHANT",
            style = MaterialTheme.typography.titleMedium,
            color = PosTextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Terminal: ${txn.terminalId.trim()}",
            style = MaterialTheme.typography.bodySmall,
            color = PosTextSecondary
        )

        Spacer(Modifier.height(16.dp))

        // Status
        val (statusText, statusColor) = when (txn.status) {
            TransactionStatus.APPROVED -> "✓ APPROVED" to PosGreen500
            TransactionStatus.DECLINED -> "✗ DECLINED" to PosRed500
            TransactionStatus.TIMEOUT  -> "⏱ TIMEOUT"  to PosAmber500
            else -> txn.status.displayName to PosTextSecondary
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineMedium,
            color = statusColor,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = txn.amountFormatted,
            style = AmountTextStyle.copy(fontSize = 36.sp),
            color = PosTextPrimary
        )

        Text(
            text = txn.type.displayName.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = PosTextSecondary,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = PosNavy600)
        Spacer(Modifier.height(16.dp))

        // Details
        ReceiptRow("Date", txn.formattedDate)
        ReceiptRow("Time", txn.formattedTime)
        ReceiptRow("Card", txn.pan, monospace = true)
        ReceiptRow("Card Brand", txn.cardBrand.displayName)
        ReceiptRow("Entry Mode", txn.posEntryMode.displayName)
        ReceiptRow("Terminal", txn.terminalId.trim(), monospace = true)
        ReceiptRow("MTI Request", txn.mtiRequest, monospace = true)
        ReceiptRow("MTI Response", txn.mtiResponse, monospace = true)
        ReceiptRow("STAN", txn.stan, monospace = true)
        if (txn.rrn.isNotBlank()) ReceiptRow("RRN", txn.rrn, monospace = true)
        if (txn.authCode.isNotBlank()) ReceiptRow("Auth Code", txn.authCode, monospace = true)
        ReceiptRow("Response Code", "${txn.responseCode} — ${txn.responseMessage}")
        ReceiptRow("Processing Time", "${txn.processingTimeMs}ms")

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = PosNavy600)
        Spacer(Modifier.height(12.dp))

        Text(
            text = "⚠️ EDUCATIONAL DEMO ONLY\nNot a real transaction",
            style = MaterialTheme.typography.labelSmall,
            color = PosTextHint,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = PosTextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = PosTextPrimary,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium
        )
    }
}
