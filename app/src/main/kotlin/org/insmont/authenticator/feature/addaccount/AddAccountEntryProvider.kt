package org.insmont.authenticator.feature.addaccount

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

fun EntryProviderScope<NavKey>.addAccountEntry(
    onBackClick: () -> Unit
) {
    entry<AddAccount> {
        AddAccountScreen(
            onBackClick = onBackClick
        )
    }
}

@Serializable
data object AddAccount : NavKey