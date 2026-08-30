package org.insmont.authenticator

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.BiometricManager
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.insmont.authenticator.core.designsystem.theme.AuthenticatorTheme
import org.insmont.authenticator.core.model.ThemeConfig
import org.insmont.authenticator.ui.AuthenticatorApp
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        window.isNavigationBarContrastEnforced = false
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { (themeConfig) -> themeConfig }
                    .distinctUntilChanged()
                    .collect { themeConfig ->
                        val darkTheme = when (themeConfig) {
                            ThemeConfig.FOLLOW_SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                            ThemeConfig.LIGHT -> false
                            ThemeConfig.DARK -> true
                        }

                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.auto(
                                lightScrim = Color.TRANSPARENT,
                                darkScrim = Color.TRANSPARENT
                            ) { darkTheme },
                            navigationBarStyle = SystemBarStyle.auto(
                                lightScrim = Color.TRANSPARENT,
                                darkScrim = Color.TRANSPARENT
                            ) { darkTheme }
                        )
                        window.isNavigationBarContrastEnforced = false
                    }
            }
        }

        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.isLoading
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (uiState.themeConfig) {
                ThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                ThemeConfig.LIGHT -> false
                ThemeConfig.DARK -> true
            }

            val authLauncher = rememberAuthenticationLauncher { result ->
                if (result is AuthenticationResult.Success) {
                    viewModel.unlockApp()
                }
            }

            val authTitle = stringResource(R.string.biometric_prompt_title)
            val appName = stringResource(R.string.app_name)
            val authSubtitle = stringResource(R.string.biometric_prompt_subtitle, appName)

            val authRequest = remember(key1 = authTitle, key2 = authSubtitle) {
                AuthenticationRequest.biometricRequest(
                    title = authTitle,
                    AuthenticationRequest.Biometric.Fallback.DeviceCredential,
                ) {
                    setSubtitle(authSubtitle)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.authRequest.collect {
                    if (isBiometricAvailable) {
                        viewModel.onAuthStarted()
                        authLauncher.launch(authRequest)
                    } else {
                        viewModel.unlockApp()
                    }
                }
            }

            AuthenticatorTheme(
                darkTheme = darkTheme,
                dynamicColor = uiState.isDynamicColorEnabled,
                hapticFeedbackEnabled = uiState.isHapticFeedbackEnabled
            ) {
                AuthenticatorApp(
                    isLocked = uiState.isAppLocked,
                    onUnlock = {
                        if (isBiometricAvailable) {
                            viewModel.onAuthStarted()
                            authLauncher.launch(authRequest)
                        } else {
                            viewModel.unlockApp()
                        }
                    }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        viewModel.onWindowFocusChanged(hasFocus)
    }

    override fun onStop() {
        super.onStop()
        viewModel.lockApp()
    }
}

val Context.isBiometricAvailable: Boolean
    get() = BiometricManager.from(this).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS