package com.mameen.isomessage.ui.screens.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.PaymentResult
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.model.UiState
import com.mameen.isomessage.domain.usecase.ProcessPurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseFormState(
    val pan: String = "5413330089010012",
    val amount: String = "10.00",
    val terminalId: String = "TERM0001",
    val currencyCode: String = "818",
    val posEntryMode: String = "051",
    val includeEmvData: Boolean = true,
    val simulateTimeout: Boolean = false,
    val simulateDecline: Boolean = false
)

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val processPurchaseUseCase: ProcessPurchaseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Transaction>>(UiState.Idle)
    val uiState: StateFlow<UiState<Transaction>> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(PurchaseFormState())
    val formState: StateFlow<PurchaseFormState> = _formState.asStateFlow()

    fun updatePan(pan: String) = _formState.value.let { _formState.value = it.copy(pan = pan) }
    fun updateAmount(amount: String) = _formState.value.let { _formState.value = it.copy(amount = amount) }
    fun updateTerminalId(id: String) = _formState.value.let { _formState.value = it.copy(terminalId = id) }
    fun updatePosEntryMode(mode: String) = _formState.value.let { _formState.value = it.copy(posEntryMode = mode) }
    fun toggleEmvData(include: Boolean) = _formState.value.let { _formState.value = it.copy(includeEmvData = include) }
    fun toggleSimulateTimeout(timeout: Boolean) = _formState.value.let { _formState.value = it.copy(simulateTimeout = timeout) }
    fun toggleSimulateDecline(decline: Boolean) {
        // Decline is triggered by using amount > 50.00 EGP (5000 cents) per Mockoon config
        val declineAmount = if (decline) "100.00" else "10.00"
        _formState.value = _formState.value.copy(simulateDecline = decline, amount = declineAmount)
    }

    fun processPurchase() {
        val form = _formState.value
        val amountCents = (form.amount.toDoubleOrNull() ?: 0.0).times(100).toLong()

        _uiState.value = UiState.Loading

        viewModelScope.launch {
            val result = processPurchaseUseCase(
                pan = form.pan.filter { it.isDigit() },
                amountCents = amountCents,
                terminalId = form.terminalId,
                currencyCode = form.currencyCode,
                posEntryMode = form.posEntryMode,
                includeEmvData = form.includeEmvData,
                simulateTimeout = form.simulateTimeout
            )

            _uiState.value = when (result) {
                is PaymentResult.Success -> UiState.Success(result.transaction)
                is PaymentResult.ValidationError -> UiState.Error("${result.field}: ${result.message}")
                is PaymentResult.NetworkError -> UiState.Error(result.message, result.isRetryable)
                is PaymentResult.Offline -> UiState.Error(result.message, false)
                is PaymentResult.UnknownError -> UiState.Error(result.message ?: "Unknown error")
            }
        }
    }

    fun resetState() { _uiState.value = UiState.Idle }
}
