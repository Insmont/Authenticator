package org.insmont.authenticator.feature.home

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.insmont.authenticator.core.data.repository.AccountRepository
import org.insmont.authenticator.core.model.Account
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val internalState = MutableStateFlow(HomeInternalState())

    val uiState: StateFlow<HomeUiState> = combine(
        flow = accountRepository.getAccounts(),
        flow2 = internalState,
        flow3 = flow { emit(accountRepository.isSecurityReliable()) },
        flow4 = snapshotFlow { editIssuerState.text },
        flow5 = snapshotFlow { editAccountNameState.text }
    ) { accountList, internal, isSecurityReliable, currentIssuer, currentAccountName ->
        val query = internal.searchQuery.trim()
        val filtered = if (query.isEmpty()) {
            accountList
        } else {
            accountList.filter { account ->
                account.issuer.contains(other = query, ignoreCase = true) || account.accountName.contains(other = query, ignoreCase = true)
            }
        }

        val original = internal.accountToEdit
        val canSave = if (original != null) {
            val issuerChanged = currentIssuer.toString().trim() != original.issuer
            val nameChanged = currentAccountName.toString().trim() != original.accountName
            (issuerChanged || nameChanged) && currentIssuer.isNotBlank() && currentAccountName.isNotBlank()
        } else false

        HomeUiState(
            accounts = filtered,
            searchQuery = internal.searchQuery,
            selectedAccounts = internal.selectedAccounts,
            accountToEdit = internal.accountToEdit,
            isSecurityReliable = isSecurityReliable,
            canSaveEdit = canSave
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    val editIssuerState = TextFieldState("")
    val editAccountNameState = TextFieldState("")

    fun decryptSecret(encryptedSecret: ByteArray): ByteArray = accountRepository.decryptSecret(encryptedSecret)

    fun onSearchQueryChanged(query: String) = internalState.update { it.copy(searchQuery = query) }

    fun toggleSelection(account: Account) = internalState.update { state ->
        val newSelected = if (state.selectedAccounts.contains(account)) {
            state.selectedAccounts - account
        } else {
            state.selectedAccounts + account
        }
        state.copy(selectedAccounts = newSelected)
    }

    fun selectAll() {
        val currentAccounts = uiState.value.accounts
        internalState.update { state ->
            if (state.selectedAccounts.size == currentAccounts.size) {
                state.copy(selectedAccounts = emptySet())
            } else {
                state.copy(selectedAccounts = currentAccounts.toSet())
            }
        }
    }

    fun clearSelection() = internalState.update { it.copy(selectedAccounts = emptySet()) }

    fun onDeleteAccount(account: Account) = viewModelScope.launch {
        accountRepository.deleteAccount(account)
        internalState.update { it.copy(selectedAccounts = it.selectedAccounts - account) }
    }

    fun deleteSelectedAccounts() = viewModelScope.launch {
        val toDelete = internalState.value.selectedAccounts
        toDelete.forEach { accountRepository.deleteAccount(it) }
        internalState.update { it.copy(selectedAccounts = emptySet()) }
    }

    fun onEditAccount(account: Account) {
        internalState.update { it.copy(accountToEdit = account) }
        editIssuerState.setTextAndPlaceCursorAtEnd(account.issuer)
        editAccountNameState.setTextAndPlaceCursorAtEnd(account.accountName)
    }

    fun onUpdateAccount() {
        val current = internalState.value.accountToEdit ?: return
        val issuer = editIssuerState.text.toString().trim()
        val accountName = editAccountNameState.text.toString().trim()

        if (issuer.isBlank() || accountName.isBlank()) return

        val updated = current.copy(
            issuer = issuer,
            accountName = accountName
        )
        viewModelScope.launch {
            accountRepository.updateAccount(updated)
            dismissEdit()
        }
    }

    fun dismissEdit() {
        internalState.update { it.copy(accountToEdit = null) }
        editIssuerState.clearText()
        editAccountNameState.clearText()
    }
}

private data class HomeInternalState(
    val searchQuery: String = "",
    val selectedAccounts: Set<Account> = emptySet(),
    val accountToEdit: Account? = null
)

data class HomeUiState(
    val accounts: List<Account> = emptyList(),
    val searchQuery: String = "",
    val selectedAccounts: Set<Account> = emptySet(),
    val accountToEdit: Account? = null,
    val isSecurityReliable: Boolean = true,
    val canSaveEdit: Boolean = false
) {
    val isSearching: Boolean get() = searchQuery.isNotEmpty()
    val isInSelectionMode: Boolean get() = selectedAccounts.isNotEmpty()
}