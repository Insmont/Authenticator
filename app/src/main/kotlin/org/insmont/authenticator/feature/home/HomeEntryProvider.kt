package org.insmont.authenticator.feature.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

fun EntryProviderScope<NavKey>.homeEntry(
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    entry<Home> {
        HomeScreen(
            onAddClick = onAddClick,
            onSettingsClick = onSettingsClick
        )
    }
}

@Serializable
data object Home : NavKey