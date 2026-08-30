package org.insmont.authenticator.core.datastore

import androidx.datastore.core.DataStore
import com.google.protobuf.kotlin.toByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.insmont.authenticator.core.model.Account
import org.insmont.authenticator.core.model.AccountType
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class AccountDataSource(
    @Named("accounts") private val accountDataStore: DataStore<AccountListProto>
) {
    val accounts: Flow<List<Account>> = accountDataStore.data.map { proto ->
        proto.accountsList.map { it.asExternalModel() }
    }

    suspend fun addAccount(account: Account) {
        accountDataStore.updateData { current ->
            val maxId = current.accountsList.maxOfOrNull { it.id } ?: 0
            current.toBuilder()
                .addAccounts(account.asProto(maxId + 1))
                .build()
        }
    }

    suspend fun updateAccount(account: Account) {
        accountDataStore.updateData { current ->
            val index = current.accountsList.indexOfFirst { it.id == account.id }
            if (index != -1) {
                current.toBuilder()
                    .setAccounts(index, account.asProto(account.id))
                    .build()
            } else {
                current
            }
        }
    }

    suspend fun deleteAccount(id: Long) {
        accountDataStore.updateData { current ->
            val index = current.accountsList.indexOfFirst { it.id == id }
            if (index != -1) {
                current.toBuilder()
                    .removeAccounts(index)
                    .build()
            } else {
                current
            }
        }
    }
}

private fun AccountProto.asExternalModel() = Account(
    id = id,
    issuer = issuer,
    accountName = accountName,
    secret = encryptedSecret.toByteArray(),
    algorithm = algorithm,
    digits = digits,
    period = period,
    type = AccountType.valueOf(type)
)

private fun Account.asProto(id: Long) = AccountProto.newBuilder()
    .setId(id)
    .setIssuer(issuer)
    .setAccountName(accountName)
    .setEncryptedSecret(secret.toByteString())
    .setAlgorithm(algorithm)
    .setDigits(digits)
    .setPeriod(period)
    .setType(type.name)
    .build()