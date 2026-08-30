package org.insmont.authenticator.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.insmont.authenticator.core.model.ThemeConfig
import org.insmont.authenticator.core.model.UserPreferences
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class UserPreferencesDataSource(
    @Named("userPreferences") private val userPreferencesDataSource: DataStore<UserPreferencesProto>
) {
    val userPreferences: Flow<UserPreferences> = userPreferencesDataSource.data.map { preferencesProto ->
        UserPreferences(
            themeConfig = when (preferencesProto.themeConfig) {
                ThemeConfigProto.THEME_CONFIG_UNSPECIFIED -> ThemeConfig.FOLLOW_SYSTEM
                ThemeConfigProto.THEME_CONFIG_FOLLOW_SYSTEM -> ThemeConfig.FOLLOW_SYSTEM
                ThemeConfigProto.THEME_CONFIG_LIGHT -> ThemeConfig.LIGHT
                ThemeConfigProto.THEME_CONFIG_DARK -> ThemeConfig.DARK
                ThemeConfigProto.UNRECOGNIZED -> ThemeConfig.FOLLOW_SYSTEM
            },
            dynamicColorEnabled = preferencesProto.dynamicColorEnabled,
            hapticFeedbackEnabled = preferencesProto.hapticFeedbackEnabled,
            appLockEnabled = preferencesProto.appLockEnabled
        )
    }

    suspend fun setThemeConfig(themeConfig: ThemeConfig) {
        userPreferencesDataSource.updateData { currentProto ->
            currentProto.toBuilder()
                .setThemeConfig(
                    when (themeConfig) {
                        ThemeConfig.FOLLOW_SYSTEM -> ThemeConfigProto.THEME_CONFIG_FOLLOW_SYSTEM
                        ThemeConfig.LIGHT -> ThemeConfigProto.THEME_CONFIG_LIGHT
                        ThemeConfig.DARK -> ThemeConfigProto.THEME_CONFIG_DARK
                    }
                )
                .build()
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        userPreferencesDataSource.updateData { currentProto ->
            currentProto.toBuilder()
                .setDynamicColorEnabled(enabled)
                .build()
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        userPreferencesDataSource.updateData { currentProto ->
            currentProto.toBuilder()
                .setHapticFeedbackEnabled(enabled)
                .build()
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        userPreferencesDataSource.updateData { currentProto ->
            currentProto.toBuilder()
                .setAppLockEnabled(enabled)
                .build()
        }
    }
}