package com.mameen.isomessage.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mameen.isomessage.domain.model.CardBrand
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.TransactionStatus
import com.mameen.isomessage.domain.model.TransactionType
import com.mameen.isomessage.ui.theme.*

// ── Section Header ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = PosCyan400,
            fontWeight = FontWeight.SemiBold
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = PosTextSecondary
            )
        }
    }
}

// ── POS Terminal Card ──────────────────────────────────────────────────────────

/**
 * Card-style container with FinTech styling.
 * Used throughout the app for grouping related content.
 */
@Composable
fun PosCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = PosCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ── Transaction Type Icon Button ───────────────────────────────────────────────

@Composable
fun TransactionTypeButton(
    type: TransactionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (type) {
        TransactionType.PURCHASE -> Icons.Default.ShoppingCart to PosCyan400
        TransactionType.REFUND -> Icons.Default.Undo to PosGreen500
        TransactionType.REVERSAL -> Icons.Default.Replay to PosAmber400
        TransactionType.SETTLEMENT -> Icons.Default.AccountBalance to IsoPurple
        TransactionType.BALANCE_INQUIRY -> Icons.Default.AccountBalanceWallet to IsoBlue
        TransactionType.ECHO -> Icons.Default.Wifi to PosTextSecondary
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PosCardBg2),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.displayName,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = PosTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(
    status: TransactionStatus,
    modifier: Modifier = Modifier
) {
    val (label, color, bgColor) = when (status) {
        TransactionStatus.APPROVED -> Triple("APPROVED", PosGreen500, Color(0xFF0A2818))
        TransactionStatus.DECLINED -> Triple("DECLINED", PosRed500, Color(0xFF2A0A0A))
        TransactionStatus.TIMEOUT  -> Triple("TIMEOUT",  PosAmber500, Color(0xFF2A1A00))
        TransactionStatus.ERROR    -> Triple("ERROR",    PosRed400, Color(0xFF2A0A0A))
        TransactionStatus.PENDING  -> Triple("PENDING",  PosTextSecondary, PosNavy600)
        TransactionStatus.REVERSED -> Triple("REVERSED", PosAmber400, Color(0xFF2A1A00))
        TransactionStatus.REFUNDED -> Triple("REFUNDED", IsoBlue, Color(0xFF0A1A2A))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ── Transaction List Item ──────────────────────────────────────────────────────

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PosCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PosNavy600),
                contentAlignment = Alignment.Center
            ) {
                Text(text = transaction.type.icon, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            // Transaction details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PosTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    StatusBadge(transaction.status)
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = transaction.pan,
                        style = MaterialTheme.typography.bodySmall,
                        color = PosTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = transaction.amountFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (transaction.isApproved) PosGreen500 else PosTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "STAN: ${transaction.stan}  •  ${transaction.formattedTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosTextHint
                )
            }
        }
    }
}

// ── Payment Button ─────────────────────────────────────────────────────────────

@Composable
fun PaymentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = PosCyan400
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = PosNavy900,
            disabledContainerColor = PosNavy600,
            disabledContentColor = PosTextHint
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PosNavy900,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Processing...", fontWeight = FontWeight.Bold)
        } else {
            icon?.let {
                Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ── ISO Field Row ─────────────────────────────────────────────────────────────

/**
 * Displays a single ISO8583 Data Element in the field inspector.
 */
@Composable
fun IsoFieldRow(
    deNumber: Int,
    fieldName: String,
    value: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (description != null) expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // DE number badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = PosNavy600,
                modifier = Modifier.width(42.dp)
            ) {
                Text(
                    text = "DE$deNumber",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = PosCyan400,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fieldName,
                    style = MaterialTheme.typography.labelMedium,
                    color = PosTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Expandable description
        AnimatedVisibility(visible = expanded && description != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(6.dp),
                color = PosNavy700
            ) {
                Text(
                    text = description ?: "",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = PosNavy600,
            thickness = 0.5.dp
        )
    }
}

// ── Amount Display ─────────────────────────────────────────────────────────────

@Composable
fun AmountDisplay(
    amount: String,
    currency: String = "EGP",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = currency,
            style = MaterialTheme.typography.titleMedium,
            color = PosTextSecondary,
            modifier = Modifier.padding(bottom = 6.dp, end = 6.dp)
        )
        Text(
            text = amount,
            style = AmountTextStyle,
            color = PosTextPrimary
        )
    }
}

// ── Screen Scaffold ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PosTextPrimary
                    )
                },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PosCyan400
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PosSurface,
                    titleContentColor = PosTextPrimary
                )
            )
        },
        containerColor = PosNavy900,
        content = content
    )
}

// ── Card Brand Icon ───────────────────────────────────────────────────────────

@Composable
fun CardBrandBadge(brand: CardBrand, modifier: Modifier = Modifier) {
    val (label, color) = when (brand) {
        CardBrand.VISA -> "VISA" to Color(0xFF1A1F71)
        CardBrand.MASTERCARD -> "MC" to Color(0xFFEB001B)
        CardBrand.AMEX -> "AMEX" to Color(0xFF007BC0)
        CardBrand.UNKNOWN -> "CARD" to PosNavy500
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Info Row (label : value) ──────────────────────────────────────────────────

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = PosTextPrimary,
    modifier: Modifier = Modifier,
    monospace: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PosTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
