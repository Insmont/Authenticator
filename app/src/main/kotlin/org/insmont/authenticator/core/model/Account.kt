package org.insmont.authenticator.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Long = 0,
    val issuer: String,
    val accountName: String,
    val secret: ByteArray,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val type: AccountType = AccountType.TOTP
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id != other.id) return false
        if (issuer != other.issuer) return false
        if (accountName != other.accountName) return false
        if (!secret.contentEquals(other.secret)) return false
        if (algorithm != other.algorithm) return false
        if (digits != other.digits) return false
        if (period != other.period) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + accountName.hashCode()
        result = 31 * result + secret.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + digits
        result = 31 * result + period
        result = 31 * result + type.hashCode()
        return result
    }

    fun clearSecret() {
        secret.fill(0)
    }
}

enum class AccountType {
    TOTP, HOTP
}