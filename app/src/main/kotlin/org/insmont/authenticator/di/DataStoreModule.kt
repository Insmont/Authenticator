package org.insmont.authenticator.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import org.insmont.authenticator.core.datastore.AccountListProto
import org.insmont.authenticator.core.datastore.AccountSerializer
import org.insmont.authenticator.core.datastore.UserPreferencesProto
import org.insmont.authenticator.core.datastore.UserPreferencesSerializer
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("org.insmont.authenticator")
class DataStoreModule {
    @Single
    @Named("userPreferences")
    fun provideUserPreferencesDataStore(context: Context): DataStore<UserPreferencesProto> =
        DataStoreFactory.create(UserPreferencesSerializer) {
            context.dataStoreFile("user_prefers.pb")
        }

    @Single
    @Named("accounts")
    fun provideAccountDataStore(context: Context): DataStore<AccountListProto> =
        DataStoreFactory.create(AccountSerializer) {
            context.dataStoreFile("accounts.pb")
        }

}