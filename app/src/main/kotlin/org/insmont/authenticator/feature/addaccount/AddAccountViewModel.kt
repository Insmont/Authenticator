package org.insmont.authenticator.feature.addaccount

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.insmont.authenticator.core.data.repository.AccountRepository
import org.insmont.authenticator.core.model.Account
import org.insmont.authenticator.core.model.AccountType
import org.insmont.authenticator.core.util.OtpUtils
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class AddAccountViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {
    val uiState: StateFlow<AddAccountUiState>
        field = MutableStateFlow(AddAccountUiState())

    private var lastScannedResult: String? = null

    fun onScanResult(result: String) {
        if (result == lastScannedResult || uiState.value.isLoading || uiState.value.isSuccess) return

        uiState.update {
            it.copy(isLoading = true)
        }
        lastScannedResult = result

        val account = parseOtpAuthUri(result)
        if (account != null) {
            saveAccount(account)
        } else {
            uiState.update {
                it.copy(isLoading = false)
            }
            lastScannedResult = null
        }
    }

    fun onShowManualAdd(show: Boolean) {
        uiState.update {
            it.copy(showManualAdd = show)
        }
    }

    fun onManualAdd() {
        val state = uiState.value
        val issuer = state.issuerState.text.toString().trim()
        val accountName = state.accountNameState.text.toString().trim()
        val secret = state.secretKeyState.text.toString().trim()

        if (issuer.isBlank() || accountName.isBlank() || secret.isBlank()) return

        val secretEncoded = secret.encodeToByteArray()
        val secretBytes = OtpUtils.decodeBase32(secretEncoded)
        val encryptedSecret = accountRepository.encryptSecret(secretBytes)
        secretBytes.fill(0)
        secretEncoded.fill(0)

        val account = Account(
            issuer = issuer,
            accountName = accountName,
            secret = encryptedSecret,
            algorithm = "SHA1"
        )
        saveAccount(account)

        state.issuerState.clearText()
        state.accountNameState.clearText()
        state.secretKeyState.clearText()
    }

    fun resetSuccess() {
        uiState.update {
            it.copy(isSuccess = false)
        }
    }

    private fun saveAccount(account: Account) {
        viewModelScope.launch {
            val currentAccounts = accountRepository.getAccounts().first()
            val accountWithSameSecret = currentAccounts.find { it.secret.contentEquals(account.secret) }
            val accountWithSameLabel = currentAccounts.find {
                it.issuer.equals(account.issuer, ignoreCase = true) &&
                        it.accountName.equals(other = account.accountName, ignoreCase = true)
            }

            when {
                accountWithSameSecret != null -> {
                    val updatedAccount = account.copy(id = accountWithSameSecret.id)
                    accountRepository.updateAccount(updatedAccount)
                }

                accountWithSameLabel != null -> {
                    val updatedAccount = account.copy(id = accountWithSameLabel.id)
                    accountRepository.updateAccount(updatedAccount)
                }

                else -> {
                    accountRepository.addAccount(account)
                }
            }

            uiState.update {
                it.copy(
                    isSuccess = true,
                    showManualAdd = false,
                    isLoading = false
                )
            }
        }
    }

    private fun parseOtpAuthUri(uriString: String): Account? {
        return try {
            val uri = uriString.toUri()
            if (uri.scheme != "otpauth") return null

            val type = when (uri.host) {
                "totp" -> AccountType.TOTP
                "hotp" -> AccountType.HOTP
                else -> return null
            }

            val path = uri.path?.removePrefix("/") ?: return null
            val labelParts = path.split(":", limit = 2)
            val issuerFromPath = if (labelParts.size > 1) labelParts[0].trim() else ""
            val accountName = (if (labelParts.size > 1) labelParts[1] else labelParts[0]).trim()

            val secret = uri.getQueryParameter("secret")?.uppercase()?.replace("\\s".toRegex(), "") ?: return null
            val issuer = uri.getQueryParameter("issuer")?.trim() ?: issuerFromPath
            val algorithm = uri.getQueryParameter("algorithm") ?: "SHA1"
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

            val secretEncoded = secret.encodeToByteArray()
            val secretBytes = OtpUtils.decodeBase32(secretEncoded)
            val encryptedSecret = accountRepository.encryptSecret(secretBytes)
            secretBytes.fill(0)
            secretEncoded.fill(0)

            Account(
                issuer = issuer,
                accountName = accountName,
                secret = encryptedSecret,
                algorithm = algorithm,
                digits = digits,
                period = period,
                type = type
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class AddAccountUiState(
    val issuerState: TextFieldState = TextFieldState(""),
    val accountNameState: TextFieldState = TextFieldState(""),
    val secretKeyState: TextFieldState = TextFieldState(""),
    val showManualAdd: Boolean = false,
    val isSuccess: Boolean = false,
    val isLoading: Boolean = false
)