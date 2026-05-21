package com.mameen.isomessage.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.TransactionType
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPurchase: () -> Unit,
    onNavigateToRefund: () -> Unit,
    onNavigateToReversal: () -> Unit,
    onNavigateToSettlement: () -> Unit,
    onNavigateToBalance: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDeveloperTools: () -> Unit,
    onNavigateToTransaction: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val transactions by viewModel.recentTransactions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ISO8583 POS Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            color = PosTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Educational Payment Demo",
                            style = MaterialTheme.typography.labelSmall,
                            color = PosCyan400
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(Icons.Default.List, contentDescription = "Logs", tint = PosTextSecondary)
                    }
                    IconButton(onClick = onNavigateToDeveloperTools) {
                        Icon(Icons.Default.Code, contentDescription = "Dev Tools", tint = PosTextSecondary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PosTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PosSurface)
            )
        },
        containerColor = PosNavy900
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Terminal Status Banner ──────────────────────────────────────────
            item {
                TerminalStatusBanner()
            }

            // ── Quick Action Grid ──────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Transactions",
                    subtitle = "Select a transaction type to simulate"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TransactionTypeButton(
                        type = TransactionType.PURCHASE,
                        onClick = onNavigateToPurchase,
                        modifier = Modifier.weight(1f)
                    )
                    TransactionTypeButton(
                        type = TransactionType.REFUND,
                        onClick = onNavigateToRefund,
                        modifier = Modifier.weight(1f)
                    )
                    TransactionTypeButton(
                        type = TransactionType.REVERSAL,
                        onClick = onNavigateToReversal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TransactionTypeButton(
                        type = TransactionType.SETTLEMENT,
                        onClick = onNavigateToSettlement,
                        modifier = Modifier.weight(1f)
                    )
                    TransactionTypeButton(
                        type = TransactionType.BALANCE_INQUIRY,
                        onClick = onNavigateToBalance,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Recent Transactions ────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Recent Transactions",
                        subtitle = "${transactions.size} transaction(s)"
                    )
                    if (transactions.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear", color = PosTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyTransactionsCard()
                }
            } else {
                items(transactions.take(10)) { txn ->
                    TransactionListItem(
                        transaction = txn,
                        onClick = { onNavigateToTransaction(txn.id) }
                    )
                }
            }

            // ── ISO8583 Info Card ──────────────────────────────────────────────
            item {
                Iso8583InfoCard()
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TerminalStatusBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PosCardBg
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(PosGreen500, shape = RoundedCornerShape(50))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Terminal: TERM0001",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PosTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Merchant: DEMO MERCHANT  •  EGP",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosTextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "ONLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosGreen500,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v1.0.0-demo",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosTextHint
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    PosCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = PosTextHint,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyMedium,
                color = PosTextSecondary
            )
            Text(
                text = "Tap a transaction type above to start",
                style = MaterialTheme.typography.bodySmall,
                color = PosTextHint
            )
        }
    }
}

@Composable
private fun Iso8583InfoCard() {
    PosCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = PosCyan400,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "About ISO8583",
                style = MaterialTheme.typography.titleSmall,
                color = PosCyan400,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ISO8583 is the global standard for card payment messaging. " +
                    "Every card transaction — Purchase, Refund, Reversal — travels as an ISO8583 message " +
                    "between your terminal, the acquiring bank, and the card issuer.",
            style = MaterialTheme.typography.bodySmall,
            color = PosTextSecondary,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This simulator uses Mockoon as the payment host backend. " +
                    "Start Mockoon with ISO8583_POS_Payment_Host.json to enable real HTTP communication.",
            style = MaterialTheme.typography.bodySmall,
            color = PosTextHint,
            lineHeight = 18.sp
        )
    }
}
