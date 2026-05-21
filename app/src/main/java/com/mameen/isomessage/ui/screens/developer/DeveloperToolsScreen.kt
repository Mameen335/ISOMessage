package com.mameen.isomessage.ui.screens.developer

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.data.model.MockoonEnvironment
import com.mameen.isomessage.data.model.MockoonRoute
import com.mameen.isomessage.emv.De55Builder
import com.mameen.isomessage.emv.TlvParser
import com.mameen.isomessage.iso8583.IsoDataElements
import com.mameen.isomessage.iso8583.IsoMessageBuilder
import com.mameen.isomessage.iso8583.IsoMessageParser
import com.mameen.isomessage.security.FakeSecuritySimulator
import com.mameen.isomessage.security.HexUtils
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import com.mameen.isomessage.util.AssetLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperToolsViewModel @Inject constructor(
    private val assetLoader: AssetLoader
) : ViewModel() {
    private val _mockoonEnv = MutableStateFlow<MockoonEnvironment?>(null)
    val mockoonEnv = _mockoonEnv.asStateFlow()

    private val _rawJson = MutableStateFlow("")
    val rawJson = _rawJson.asStateFlow()

    var fakeDe55 by mutableStateOf(De55Builder.buildFakeDe55())
    var fakePinBlock by mutableStateOf(FakeSecuritySimulator.generateFakePinBlock())
    var fakeMac by mutableStateOf(FakeSecuritySimulator.generateFakeMac("Demo message data"))
    var fakeSessionKey by mutableStateOf(FakeSecuritySimulator.generateFakeSessionKey())

    init { loadMockoonEnv() }

    private fun loadMockoonEnv() {
        viewModelScope.launch {
            val json = assetLoader.readAssetFile("ISO8583_POS_Payment_Host.json") ?: ""
            _rawJson.value = json
            _mockoonEnv.value = assetLoader.loadMockoonEnvironment()
        }
    }

    fun refreshSecurity() {
        fakeDe55 = De55Builder.buildFakeDe55()
        fakePinBlock = FakeSecuritySimulator.generateFakePinBlock()
        fakeMac = FakeSecuritySimulator.generateFakeMac("Demo message data")
        fakeSessionKey = FakeSecuritySimulator.generateFakeSessionKey()
    }

    fun getSamplePurchaseIso(): String {
        val msg = IsoMessageBuilder.purchaseRequest("5413330089010012", 1000L)
        return IsoMessageParser.toPrettyString(msg)
    }
}

enum class DevTab { ENDPOINTS, ISO_SAMPLES, EMV_TLV, SECURITY, RAW_JSON }

@Composable
fun DeveloperToolsScreen(
    onBack: () -> Unit,
    viewModel: DeveloperToolsViewModel = hiltViewModel()
) {
    val env by viewModel.mockoonEnv.collectAsStateWithLifecycle()
    val rawJson by viewModel.rawJson.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(DevTab.ENDPOINTS) }

    PosScaffold(title = "Developer Tools", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = PosSurface,
                contentColor = PosCyan400
            ) {
                DevTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                tab.name.replace("_", " "),
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                DevTab.ENDPOINTS -> EndpointsTab(env)
                DevTab.ISO_SAMPLES -> IsoSamplesTab(viewModel)
                DevTab.EMV_TLV -> EmvTlvTab(viewModel)
                DevTab.SECURITY -> SecurityTab(viewModel)
                DevTab.RAW_JSON -> RawJsonTab(rawJson)
            }
        }
    }
}

@Composable
private fun EndpointsTab(env: MockoonEnvironment?) {
    if (env == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PosCyan400)
                Spacer(Modifier.height(8.dp))
                Text("Loading ISO8583_POS_Payment_Host.json...", color = PosTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PosCard {
                InfoRow("Environment", env.name)
                InfoRow("Host", env.fullBaseUrl, monospace = true)
                InfoRow("Port", env.port.toString(), monospace = true)
                InfoRow("Latency", "${env.latency}ms")
                InfoRow("Routes", "${env.routes.size} endpoints")
                InfoRow("TLS", if (env.tlsOptions?.enabled == true) "Enabled" else "Disabled")
            }
        }

        item { SectionHeader("API Endpoints", "Defined in Mockoon configuration") }

        items(env.routes) { route ->
            RouteCard(route)
        }
    }
}

@Composable
private fun RouteCard(route: MockoonRoute) {
    var expanded by remember { mutableStateOf(false) }
    PosCard(onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(4.dp), color = PosCyan400) {
                Text(
                    route.httpMethod,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = PosNavy900,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                route.fullPath,
                style = MaterialTheme.typography.bodyMedium,
                color = PosTextPrimary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = PosTextSecondary
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${route.responses.size} response variant(s)",
                style = MaterialTheme.typography.labelSmall,
                color = PosTextSecondary
            )

            route.sampleRequest?.let { req ->
                Spacer(Modifier.height(6.dp))
                Text("Sample Request:", style = MaterialTheme.typography.labelSmall, color = PosGreen400)
                Surface(shape = RoundedCornerShape(6.dp), color = PosNavy800) {
                    Text(req, modifier = Modifier.padding(8.dp).fillMaxWidth(), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = PosGreen400)
                }
            }

            route.responses.firstOrNull()?.let { res ->
                Spacer(Modifier.height(6.dp))
                Text("Sample Response (HTTP ${res.statusCode}):", style = MaterialTheme.typography.labelSmall, color = PosCyan300)
                Surface(shape = RoundedCornerShape(6.dp), color = PosNavy800) {
                    Text(res.body.take(400) + if (res.body.length > 400) "..." else "", modifier = Modifier.padding(8.dp).fillMaxWidth(), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = PosCyan300)
                }
            }
        }
    }
}

@Composable
private fun IsoSamplesTab(viewModel: DeveloperToolsViewModel) {
    val samplePurchaseIso = remember { viewModel.getSamplePurchaseIso() }
    val samplePurchaseMsg = remember { IsoMessageBuilder.purchaseRequest("5413330089010012", 1000L) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader("Purchase 0200 — Field Inspector") }
        item {
            Surface(shape = RoundedCornerShape(8.dp), color = PosNavy800) {
                Text(samplePurchaseIso, modifier = Modifier.padding(12.dp).fillMaxWidth(), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = PosGreen400)
            }
        }

        item { SectionHeader("All Supported Data Elements") }
        items(IsoDataElements.getAllDefinitions()) { def ->
            PosCard {
                Row {
                    Surface(shape = RoundedCornerShape(4.dp), color = PosNavy700) {
                        Text("DE${def.number}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = PosCyan400, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(def.name, style = MaterialTheme.typography.bodySmall, color = PosTextPrimary, fontWeight = FontWeight.Medium)
                        Text("${def.type} / ${def.length} / ${def.lengthType}", style = MaterialTheme.typography.labelSmall, color = PosTextHint, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(def.description, style = MaterialTheme.typography.bodySmall, color = PosTextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("Example: ${def.example}", style = MaterialTheme.typography.labelSmall, color = PosTextHint, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun EmvTlvTab(viewModel: DeveloperToolsViewModel) {
    val tlvObjects = remember(viewModel.fakeDe55) { TlvParser.parse(viewModel.fakeDe55) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PosCard {
                Text("DE55 — EMV/ICC Data", style = MaterialTheme.typography.titleSmall, color = PosCyan400, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "DE55 carries EMV chip data as concatenated TLV (Tag-Length-Value) objects.\n" +
                    "The ARQC (tag 9F26) cryptogram is generated by the card's secure element.\n" +
                    "⚠️ All values below are FAKE for demo purposes only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextSecondary
                )
            }
        }

        item { SectionHeader("Parsed TLV Objects (${tlvObjects.size} tags)") }

        items(tlvObjects) { tlv ->
            PosCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = PosNavy700) {
                        Text(tlv.tagHex, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = IsoOrange, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(tlv.tagName, style = MaterialTheme.typography.bodySmall, color = PosTextPrimary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(4.dp))
                Text("Length: ${tlv.length} bytes  |  ${tlv.tagHex} ${"%02X".format(tlv.length)} ${tlv.valueHex}", style = MaterialTheme.typography.labelSmall, color = PosTextHint, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text(tlv.tagDescription, style = MaterialTheme.typography.bodySmall, color = PosTextSecondary)
            }
        }
    }
}

@Composable
private fun SecurityTab(viewModel: DeveloperToolsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PosCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = PosRed400, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DEMO VALUES ONLY — Not cryptographically valid", style = MaterialTheme.typography.labelMedium, color = PosRed400, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.refreshSecurity() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PosCyan400)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Regenerate Fake Values")
            }
        }

        item { SectionHeader("PIN Block (ISO 9564 Format 0)") }
        item {
            PosCard {
                Text("PIN: 1234  |  PAN: 5413330089010012", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
                Spacer(Modifier.height(4.dp))
                Text(viewModel.fakePinBlock, style = MaterialTheme.typography.bodyMedium, color = PosGreen400, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Real PIN blocks are XOR of PIN data and PAN data, then 3DES-encrypted under the PIN Encryption Key (PEK). The encrypted block (never plaintext) is placed in DE52 and transmitted to the issuer's HSM.",
                    style = MaterialTheme.typography.bodySmall, color = PosTextSecondary
                )
            }
        }

        item { SectionHeader("Message Authentication Code (MAC)") }
        item {
            PosCard {
                Text("Input: 'Demo message data'", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
                Spacer(Modifier.height(4.dp))
                Text(viewModel.fakeMac, style = MaterialTheme.typography.bodyMedium, color = PosCyan400, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Real MACs use HMAC-SHA256 or CMAC-AES128 with the MAC Session Key. The MAC goes in DE64 (primary) or DE128 (secondary). Protects message integrity — any modification changes the MAC.",
                    style = MaterialTheme.typography.bodySmall, color = PosTextSecondary
                )
            }
        }

        item { SectionHeader("Session Key (DUKPT)") }
        item {
            PosCard {
                InfoRow("Key (hex)", viewModel.fakeSessionKey.keyHex, monospace = true)
                InfoRow("ATC", viewModel.fakeSessionKey.atc.toString())
                Spacer(Modifier.height(6.dp))
                Text(viewModel.fakeSessionKey.description, style = MaterialTheme.typography.bodySmall, color = PosTextSecondary)
            }
        }

        item { SectionHeader("Hex Utilities") }
        item {
            PosCard {
                InfoRow("Luhn check '5413330089010012'", if (HexUtils.isValidLuhn("5413330089010012")) "✓ Valid" else "✗ Invalid", valueColor = PosGreen500)
                InfoRow("Masked PAN", HexUtils.maskPan("5413330089010012"), monospace = true)
                InfoRow("'TERM0001' → hex", HexUtils.stringToHex("TERM0001"), monospace = true)
                InfoRow("XOR [01,02,03] ⊕ [FF,FE,FD]", HexUtils.bytesToHex(HexUtils.xorBytes(byteArrayOf(0x01, 0x02, 0x03), byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte()))), monospace = true)
            }
        }
    }
}

@Composable
private fun RawJsonTab(rawJson: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PosCard {
                Text("ISO8583_POS_Payment_Host.json", style = MaterialTheme.typography.titleSmall, color = PosCyan400, fontWeight = FontWeight.Bold)
                Text("${rawJson.lines().size} lines  •  ${rawJson.length} chars", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(8.dp), color = PosNavy800) {
                Text(
                    text = rawJson.take(8000) + if (rawJson.length > 8000) "\n... (truncated)" else "",
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = PosGreen400
                )
            }
        }
    }
}
