package org.insmont.authenticator.feature.settings

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.LocaleList
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.insmont.authenticator.core.data.repository.AccountRepository
import org.insmont.authenticator.core.datastore.UserPreferencesDataSource
import org.insmont.authenticator.core.model.BackupData
import org.insmont.authenticator.core.model.ThemeConfig
import org.insmont.authenticator.core.model.UserPreferences
import org.insmont.authenticator.core.security.BackupCryptoManager
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    appContext: Context,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val accountRepository: AccountRepository,
    private val backupCryptoManager: BackupCryptoManager
) : ViewModel() {
    private val contentResolver = appContext.contentResolver
    private val localeManager = appContext.getSystemService(LocaleManager::class.java)

    val events: SharedFlow<SettingsEvent>
        field = MutableSharedFlow<SettingsEvent>()

    val language: StateFlow<String>
        field = MutableStateFlow(getCurrentLanguage())

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        userPreferencesDataSource.userPreferences,
        language
    ) { preferences, language ->
        SettingsUiState.Success(
            settings = preferences,
            language = language
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState.Loading
    )

    fun updateThemeConfig(themeConfig: ThemeConfig) = viewModelScope.launch {
        userPreferencesDataSource.setThemeConfig(themeConfig)
    }

    fun updateDynamicColorPreference(enabled: Boolean) = viewModelScope.launch {
        userPreferencesDataSource.setDynamicColorEnabled(enabled)
    }

    fun updateHapticFeedbackPreference(enabled: Boolean) = viewModelScope.launch {
        userPreferencesDataSource.setHapticFeedbackEnabled(enabled)
    }

    fun updateAppLockPreference(enabled: Boolean) = viewModelScope.launch {
        userPreferencesDataSource.setAppLockEnabled(enabled)
    }

    fun exportData(uri: Uri, password: String) = viewModelScope.launch {
        try {
            val accounts = accountRepository.getAccounts().first()
            val decryptedAccounts = accounts.map { account ->
                account.copy(secret = accountRepository.decryptSecret(account.secret))
            }
            val backupData = BackupData(accounts = decryptedAccounts)
            val jsonString = Json.encodeToString(backupData)
            val jsonBytes = jsonString.encodeToByteArray()
            val encryptedData = backupCryptoManager.encrypt(
                jsonBytes,
                password.toCharArray()
            )

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(encryptedData)
            }

            decryptedAccounts.forEach { it.clearSecret() }
            jsonBytes.fill(0)
            encryptedData.fill(0)

            events.emit(SettingsEvent.ExportSuccess)
        } catch (e: Exception) {
            events.emit(SettingsEvent.ExportFailed(e.message ?: "未知错误"))
        }
    }

    fun importData(uri: Uri, password: String) = viewModelScope.launch {
        try {
            val encryptedData = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw Exception("读取文件失败")

            val decryptedBytes = backupCryptoManager.decrypt(
                encryptedData,
                password.toCharArray()
            )
            val decryptedData = decryptedBytes.decodeToString()

            val backupData = Json.decodeFromString<BackupData>(decryptedData)
            val existingAccounts = accountRepository.getAccounts().first()
            var importedCount = 0

            backupData.accounts.forEach { account ->
                val existingAccount = existingAccounts.find {
                    it.issuer == account.issuer && it.accountName == account.accountName
                }

                val encryptedAccount = account.copy(
                    secret = accountRepository.encryptSecret(account.secret)
                )

                if (existingAccount != null) {
                    accountRepository.updateAccount(
                        encryptedAccount.copy(id = existingAccount.id)
                    )
                } else {
                    accountRepository.addAccount(encryptedAccount)
                }
                importedCount++
                account.clearSecret()
            }

            decryptedBytes.fill(0)
            encryptedData.fill(0)

            events.emit(SettingsEvent.ImportSuccess(importedCount))
        } catch (_: BackupCryptoManager.InvalidPasswordException) {
            events.emit(SettingsEvent.InvalidPassword)
        } catch (e: Exception) {
            events.emit(SettingsEvent.ImportFailed(e.message ?: "未知错误"))
        }
    }

    fun viewSource(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Insmont/Authenticator".toUri())
        context.startActivity(intent)
    }

    fun updateLanguage(language: String) {
        val locales = if (language.isEmpty()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(language)
        }
        localeManager.applicationLocales = locales
        this.language.value = language
    }

    private fun getCurrentLanguage(): String = localeManager.applicationLocales.toLanguageTags()
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val settings: UserPreferences,
        val language: String
    ) : SettingsUiState
}

sealed interface SettingsEvent {
    data object ExportSuccess : SettingsEvent
    data class ExportFailed(val message: String) : SettingsEvent
    data object InvalidPassword : SettingsEvent
    data class ImportSuccess(val count: Int) : SettingsEvent
    data class ImportFailed(val message: String) : SettingsEvent
}