package org.insmont.authenticator.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.insmont.authenticator.feature.addaccount.AddAccount
import org.insmont.authenticator.feature.addaccount.addAccountEntry
import org.insmont.authenticator.feature.home.Home
import org.insmont.authenticator.feature.home.homeEntry
import org.insmont.authenticator.feature.settings.Settings
import org.insmont.authenticator.feature.settings.settingsEntry

@Composable
fun AuthenticatorApp(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLocked) {
        LockScreen(onUnlockClick = onUnlock, modifier = modifier)
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus()
        }
    }

    val backStack = rememberNavBackStack(Home)
    val entryProvider = remember {
        entryProvider {
            homeEntry(
                onAddClick = { backStack.add(AddAccount) },
                onSettingsClick = { backStack.add(Settings) }
            )
            addAccountEntry(
                onBackClick = { backStack.removeLastOrNull() }
            )
            settingsEntry(onBackClick = { backStack.removeLastOrNull() })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            }
        )
    }
}