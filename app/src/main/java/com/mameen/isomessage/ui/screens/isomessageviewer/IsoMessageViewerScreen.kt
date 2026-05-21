package com.mameen.isomessage.ui.screens.isomessageviewer

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.repository.PaymentRepository
import com.mameen.isomessage.iso8583.BitMap
import com.mameen.isomessage.iso8583.IsoDataElements
import com.mameen.isomessage.iso8583.IsoMessage
import com.mameen.isomessage.iso8583.IsoMessageParser
import com.mameen.isomessage.iso8583.Mti
import com.mameen.isomessage.security.HexUtils
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IsoViewerViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {
    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction = _transaction.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch { _transaction.value = repository.getTransactionById(id) }
    }
}

enum class IsoViewerTab { REQUEST, RESPONSE, BITMAP, HEX }

@Composable
fun IsoMessageViewerScreen(
    transactionId: String,
    onBack: () -> Unit,
    viewModel: IsoViewerViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(IsoViewerTab.REQUEST) }

    LaunchedEffect(transactionId) { viewModel.load(transactionId) }

    PosScaffold(title = "ISO8583 Message Viewer", onBack = onBack) { padding ->
        transaction?.let { txn ->
            val reqMsg = runCatching { IsoMessageParser.fromJson(txn.isoRequestJson) }.getOrNull()
            val resMsg = runCatching { IsoMessageParser.fromJson(txn.isoResponseJson) }.getOrNull()

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = PosSurface,
                    contentColor = PosCyan400
                ) {
                    IsoViewerTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    tab.name,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    IsoViewerTab.REQUEST -> IsoFieldsTab(reqMsg, "Request (${reqMsg?.mti ?: "?"})")
                    IsoViewerTab.RESPONSE -> IsoFieldsTab(resMsg, "Response (${resMsg?.mti ?: "?"})")
                    IsoViewerTab.BITMAP -> BitmapTab(reqMsg, resMsg)
                    IsoViewerTab.HEX -> HexTab(txn)
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PosCyan400)
        }
    }
}

@Composable
private fun IsoFieldsTab(isoMsg: IsoMessage?, title: String) {
    if (isoMsg == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No $title available", color = PosTextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // MTI info
        item {
            PosCard {
                Text("MTI: ${isoMsg.mti}", style = MaterialTheme.typography.titleSmall, color = PosCyan400, fontWeight = FontWeight.Bold)
                Text(
                    isoMsg.mtiEnum?.description ?: "Unknown MTI",
                    style = MaterialTheme.typography.bodySmall, color = PosTextSecondary
                )
                Spacer(Modifier.height(4.dp))
                val mti = isoMsg.mti
                Text("Version: ${Mti.describeVersion(mti)}", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
                Text("Class: ${Mti.describeClass(mti)}", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
                Text("Function: ${Mti.describeFunction(mti)}", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
                Text("Origin: ${Mti.describeOrigin(mti)}", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
            }
        }

        item { SectionHeader("Data Elements", "Tap any field for educational description") }

        items(isoMsg.fields.entries.toList()) { (de, value) ->
            val def = IsoDataElements.getDefinition(de)
            val displayValue = if (de == 2) isoMsg.maskedPan ?: value
                               else if (de == 35) "*** TRACK2 REDACTED ***"
                               else value
            IsoFieldRow(
                deNumber = de,
                fieldName = def?.name ?: "DE$de",
                value = displayValue,
                description = def?.let { "${it.description}\n\nExample: ${it.example}\nType: ${it.type}, Length: ${it.length} (${it.lengthType})" }
            )
        }
    }
}

@Composable
private fun BitmapTab(reqMsg: IsoMessage?, resMsg: IsoMessage?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PosCard {
                Text("How Bitmaps Work", style = MaterialTheme.typography.titleSmall, color = PosCyan400, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Each bit (1–64) in the bitmap corresponds to a Data Element (DE).\n" +
                    "If the bit is SET (■), that DE is present in the message.\n" +
                    "If UNSET (□), that DE is absent.\n" +
                    "Bit 1 = 1 means a Secondary Bitmap exists (DE65–DE128).",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextSecondary
                )
            }
        }

        reqMsg?.let { msg ->
            item { SectionHeader("Request Bitmap", msg.bitmap.toHexString()) }
            item { BitmapGrid(msg.bitmap) }
        }

        resMsg?.let { msg ->
            item { SectionHeader("Response Bitmap", msg.bitmap.toHexString()) }
            item { BitmapGrid(msg.bitmap) }
        }
    }
}

@Composable
private fun BitmapGrid(bitmap: BitMap) {
    val rows = bitmap.toVisualGrid()
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "B${row.byteIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosTextHint,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = row.hexByte,
                    style = MaterialTheme.typography.labelSmall,
                    color = PosTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(32.dp)
                )
                Spacer(Modifier.width(4.dp))
                row.bits.forEach { bit ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (bit.isSet) BitSetColor.copy(alpha = 0.2f) else BitUnsetColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${bit.deNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = if (bit.isSet) BitSetColor else PosTextHint
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(BitSetColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(4.dp))
            Text("Present", style = MaterialTheme.typography.labelSmall, color = PosTextSecondary)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(12.dp).background(BitUnsetColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(4.dp))
            Text("Absent", style = MaterialTheme.typography.labelSmall, color = PosTextSecondary)
        }
    }
}

@Composable
private fun HexTab(txn: Transaction) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PosCard {
                Text("Hex Dump", style = MaterialTheme.typography.titleSmall, color = PosCyan400, fontWeight = FontWeight.Bold)
                Text(
                    "In real payment systems, ISO8583 messages travel as binary over TCP.\n" +
                    "This shows the JSON representation used by this HTTP simulator.\n" +
                    "In production, DE values are BCD or ASCII encoded based on their type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextSecondary
                )
            }
        }

        item { SectionHeader("Request JSON (sent to host)") }
        item {
            Surface(shape = RoundedCornerShape(8.dp), color = PosNavy800) {
                Text(
                    text = txn.isoRequestJson,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = PosGreen400
                )
            }
        }

        item { SectionHeader("Response JSON (received from host)") }
        item {
            Surface(shape = RoundedCornerShape(8.dp), color = PosNavy800) {
                Text(
                    text = txn.isoResponseJson,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = PosCyan300
                )
            }
        }
    }
}
