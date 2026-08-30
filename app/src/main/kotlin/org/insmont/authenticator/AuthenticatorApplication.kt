package org.insmont.authenticator

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class AuthenticatorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin<AuthenticatorApplication> {
            androidContext(this@AuthenticatorApplication)
        }
    }
}