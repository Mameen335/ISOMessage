package com.mameen.isomessage.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mameen.isomessage.domain.model.Transaction
import com.mameen.isomessage.domain.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {

    val recentTransactions: StateFlow<List<Transaction>> =
        repository.getTransactionHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { repository.clearTransactions() }
    }
}
