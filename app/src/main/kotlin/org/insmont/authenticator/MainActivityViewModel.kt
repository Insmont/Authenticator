package org.insmont.authenticator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.insmont.authenticator.core.datastore.UserPreferencesDataSource
import org.insmont.authenticator.core.model.ThemeConfig
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainActivityViewModel(
    userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {
    private val isAppUnlocked = MutableStateFlow(false)
    private var isAutoAuthAttempted = false
    private var hasWindowFocus = false

    private val _authRequest = Channel<Unit>(Channel.CONFLATED)
    val authRequest: Flow<Unit> = _authRequest.receiveAsFlow()

    val uiState: StateFlow<MainActivityUiState> = combine(
        userPreferencesDataSource.userPreferences,
        isAppUnlocked
    ) { prefs, unlocked ->
        MainActivityUiState(
            themeConfig = prefs.themeConfig,
            isDynamicColorEnabled = prefs.dynamicColorEnabled,
            isHapticFeedbackEnabled = prefs.hapticFeedbackEnabled,
            isAppLocked = prefs.appLockEnabled && !unlocked,
            isBiometricAuthRequired = prefs.appLockEnabled && !unlocked,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainActivityUiState()
    )

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.isBiometricAuthRequired) {
                    checkAndTriggerAuth()
                }
            }
        }
    }

    fun unlockApp() {
        isAppUnlocked.value = true
        isAutoAuthAttempted = false
    }

    fun lockApp() {
        isAppUnlocked.value = false
        isAutoAuthAttempted = false
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        this.hasWindowFocus = hasFocus
        checkAndTriggerAuth()
    }

    fun onAuthStarted() {
        isAutoAuthAttempted = true
    }

    private fun checkAndTriggerAuth() {
        if (hasWindowFocus && !isAutoAuthAttempted && uiState.value.isBiometricAuthRequired) {
            isAutoAuthAttempted = true
            _authRequest.trySend(Unit)
        }
    }
}

data class MainActivityUiState(
    val themeConfig: ThemeConfig = ThemeConfig.FOLLOW_SYSTEM,
    val isDynamicColorEnabled: Boolean = false,
    val isHapticFeedbackEnabled: Boolean = false,
    val isAppLocked: Boolean = true,
    val isBiometricAuthRequired: Boolean = false,
    val isLoading: Boolean = true
)