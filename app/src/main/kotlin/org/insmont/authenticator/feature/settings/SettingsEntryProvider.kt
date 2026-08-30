package org.insmont.authenticator.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

fun EntryProviderScope<NavKey>.settingsEntry(onBackClick: () -> Unit) {
    entry<Settings> {
        SettingsScreen(onBackClick = onBackClick)
    }
}

@Serializable
data object Settings : NavKey