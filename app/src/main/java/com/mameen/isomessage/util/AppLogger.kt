package com.mameen.isomessage.util

import android.util.Log
import com.mameen.isomessage.BuildConfig
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralised in-memory logger for the POS simulator.
 *
 * In a real terminal application:
 * - Logs are written to a tamper-evident local storage
 * - Uploaded to a centralized log management system (Splunk, ELK, etc.)
 * - Subject to PCI DSS log retention requirements (typically 1 year)
 * - Never contain full PANs, PINs, or CVV values
 *
 * This simulator logs to memory + Android logcat for educational purposes.
 */
@Singleton
class AppLogger @Inject constructor() {

    private val _logs = mutableListOf<LogEntry>()
    val logs: List<LogEntry> get() = _logs.toList()

    companion object {
        private const val TAG = "ISOMessage"
        const val MAX_LOG_ENTRIES = 500
    }

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, message + (throwable?.let { " — ${it.message}" } ?: ""))

    fun logIsoRequest(message: String, mti: String, stan: String?) =
        log(LogLevel.ISO_REQUEST, "ISO-REQ", "MTI=$mti STAN=$stan | $message")

    fun logIsoResponse(message: String, mti: String, rc: String?, stan: String?) =
        log(LogLevel.ISO_RESPONSE, "ISO-RES", "MTI=$mti RC=$rc STAN=$stan | $message")

    fun logNetworkError(message: String) = log(LogLevel.ERROR, "NETWORK", message)
    fun logTransaction(message: String) = log(LogLevel.TRANSACTION, "TXN", message)

    private fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        // Write to Android logcat (debug builds only)
        if (BuildConfig.ENABLE_LOGGING) {
            when (level) {
                LogLevel.ERROR -> Log.e(TAG, "[$tag] $message")
                LogLevel.WARN -> Log.w(TAG, "[$tag] $message")
                LogLevel.DEBUG -> Log.d(TAG, "[$tag] $message")
                else -> Log.i(TAG, "[$tag] $message")
            }
        }

        // Add to in-memory list (with circular buffer limit)
        synchronized(_logs) {
            _logs.add(entry)
            if (_logs.size > MAX_LOG_ENTRIES) {
                _logs.removeAt(0)
            }
        }
    }

    fun clearLogs() = synchronized(_logs) { _logs.clear() }

    fun getLogsByLevel(level: LogLevel): List<LogEntry> = _logs.filter { it.level == level }

    fun exportLogs(): String {
        val sb = StringBuilder()
        sb.appendLine("=== ISO8583 POS Simulator Log Export ===")
        sb.appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        sb.appendLine("Total entries: ${_logs.size}")
        sb.appendLine("=========================================")
        _logs.forEach { entry ->
            sb.appendLine("${entry.formattedTimestamp} [${entry.level.label}] ${entry.tag}: ${entry.message}")
        }
        return sb.toString()
    }
}

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTimestamp: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

enum class LogLevel(val label: String, val emoji: String) {
    DEBUG("DEBUG", "🔍"),
    INFO("INFO", "ℹ️"),
    WARN("WARN", "⚠️"),
    ERROR("ERROR", "❌"),
    ISO_REQUEST("ISO-REQ", "📤"),
    ISO_RESPONSE("ISO-RES", "📥"),
    TRANSACTION("TXN", "💳")
}
