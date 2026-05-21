package com.mameen.isomessage.ui.screens.logs

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.ui.components.*
import com.mameen.isomessage.ui.theme.*
import com.mameen.isomessage.util.AppLogger
import com.mameen.isomessage.util.LogEntry
import com.mameen.isomessage.util.LogLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logger: AppLogger
) : ViewModel() {
    val logs: List<LogEntry> get() = logger.logs.reversed()
    fun clearLogs() = logger.clearLogs()
}

@Composable
fun LogsScreen(
    onBack: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel()
) {
    var filterLevel by remember { mutableStateOf<LogLevel?>(null) }
    var logs by remember { mutableStateOf(viewModel.logs) }

    PosScaffold(
        title = "Logs",
        onBack = onBack,
        actions = {
            IconButton(onClick = { viewModel.clearLogs(); logs = viewModel.logs }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = PosRed400)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterLevel == null,
                    onClick = { filterLevel = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PosCyan400, selectedLabelColor = PosNavy900)
                )
                LogLevel.entries.take(4).forEach { level ->
                    FilterChip(
                        selected = filterLevel == level,
                        onClick = { filterLevel = if (filterLevel == level) null else level },
                        label = { Text(level.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PosCyan400, selectedLabelColor = PosNavy900)
                    )
                }
            }

            val filtered = if (filterLevel == null) logs else logs.filter { it.level == filterLevel }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs", color = PosTextHint)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered) { entry ->
                        LogEntryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.ERROR -> PosRed400
        LogLevel.WARN -> PosAmber400
        LogLevel.ISO_REQUEST -> PosCyan400
        LogLevel.ISO_RESPONSE -> PosGreen400
        LogLevel.TRANSACTION -> IsoPurple
        else -> PosTextSecondary
    }
    Surface(shape = RoundedCornerShape(4.dp), color = PosCardBg) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = entry.formattedTimestamp,
                style = MaterialTheme.typography.labelSmall,
                color = PosTextHint,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(72.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "[${entry.level.label}]",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.width(68.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${entry.tag}: ${entry.message}",
                style = MaterialTheme.typography.labelSmall,
                color = PosTextSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
