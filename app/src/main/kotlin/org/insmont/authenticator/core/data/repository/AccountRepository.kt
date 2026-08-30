package org.insmont.authenticator.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.insmont.authenticator.core.datastore.AccountDataSource
import org.insmont.authenticator.core.model.Account
import org.insmont.authenticator.core.security.CryptoManager
import org.insmont.authenticator.core.util.RootUtils
import org.koin.core.annotation.Single

@Single
class AccountRepository(
    private val accountDataSource: AccountDataSource,
    private val cryptoManager: CryptoManager
) {
    fun getAccounts(): Flow<List<Account>> = accountDataSource.accounts

    suspend fun addAccount(account: Account) = accountDataSource.addAccount(account)

    suspend fun updateAccount(account: Account) = accountDataSource.updateAccount(account)

    suspend fun deleteAccount(account: Account) = accountDataSource.deleteAccount(account.id)

    fun encryptSecret(secret: ByteArray): ByteArray = cryptoManager.encrypt(secret)

    fun decryptSecret(encryptedSecret: ByteArray): ByteArray = cryptoManager.decrypt(encryptedSecret)

    private val isSecurityReliableCached by lazy {
        cryptoManager.isHardwareBacked() && !RootUtils.isRooted()
    }

    fun isSecurityReliable(): Boolean = isSecurityReliableCached
}