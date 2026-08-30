package org.insmont.authenticator.core.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val accounts: List<Account>,
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)