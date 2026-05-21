package com.mameen.isomessage.ui.screens.settings

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
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var baseUrl by remember { mutableStateOf("http://10.0.2.2:5000/") }
    var terminalId by remember { mutableStateOf("TERM0001") }
    var merchantId by remember { mutableStateOf("MERCHANT001    ") }
    var timeoutSeconds by remember { mutableStateOf("30") }
    var enableLogging by remember { mutableStateOf(true) }

    PosScaffold(title = "Settings", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Network Configuration", "Mockoon simulator connection") }
            item {
                PosCard {
                    Text("Base URL", style = MaterialTheme.typography.labelMedium, color = PosTextSecondary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true,
                        supportingText = {
                            Text(
                                "Use 10.0.2.2:5000 for Android emulator\nlocalhost:5000 for physical device with USB",
                                color = PosTextHint
                            )
                        }
                    )
                }
            }

            item { SectionHeader("Terminal Configuration", "DE41 and DE42 values") }
            item {
                PosCard {
                    OutlinedTextField(
                        value = terminalId,
                        onValueChange = { terminalId = it.take(8) },
                        label = { Text("Terminal ID (DE41, 8 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = merchantId,
                        onValueChange = { merchantId = it.take(15) },
                        label = { Text("Merchant ID (DE42, 15 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true
                    )
                }
            }

            item { SectionHeader("Connection Timeouts", "ISO8583 timing parameters") }
            item {
                PosCard {
                    OutlinedTextField(
                        value = timeoutSeconds,
                        onValueChange = { timeoutSeconds = it },
                        label = { Text("Response Timeout (seconds)") },
                        suffix = { Text("s", color = PosTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Industry standard: 30s for online auth, 60s for settlement",
                        style = MaterialTheme.typography.labelSmall,
                        color = PosTextHint
                    )
                }
            }

            item { SectionHeader("Developer Options") }
            item {
                PosCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Enable Logging", style = MaterialTheme.typography.bodyMedium, color = PosTextPrimary)
                            Text("Log ISO messages and network calls", style = MaterialTheme.typography.labelSmall, color = PosTextHint)
                        }
                        Switch(
                            checked = enableLogging,
                            onCheckedChange = { enableLogging = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PosNavy900, checkedTrackColor = PosCyan400)
                        )
                    }
                }
            }

            item {
                PosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = PosAmber400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Settings are not persisted in this demo version. " +
                            "Changes will reset on app restart.\n\n" +
                            "For production: implement SharedPreferences or DataStore with encrypted storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextHint
                        )
                    }
                }
            }

            item {
                Text(
                    text = "ISO8583 POS Simulator v1.0.0\n" +
                           "Educational/Demo Purpose Only\n" +
                           "Not for production payment processing",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextHint
                )
            }
        }
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PosCyan400,
    unfocusedBorderColor = PosNavy500,
    focusedLabelColor = PosCyan400,
    unfocusedLabelColor = PosTextSecondary,
    cursorColor = PosCyan400,
    focusedTextColor = PosTextPrimary,
    unfocusedTextColor = PosTextPrimary
)
