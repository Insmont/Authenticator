package org.insmont.authenticator.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import org.koin.core.annotation.Single

@Single
class CryptoManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    fun isHardwareBacked(): Boolean {
        return try {
            val key = getKey()
            val factory = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
            val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
            val securityLevel = keyInfo.securityLevel
            securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT || securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
        } catch (e: Exception) {
            Log.e("CryptoManager", "检查硬件支持失败", e)
            false
        }
    }

    private val encryptCipher
        get() = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }

    private fun getDecryptCipherForIv(iv: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .setInvalidatedByBiometricEnrollment(false)
            .setRandomizedEncryptionRequired(true)

        return try {
            keyGenerator.init(builder.setIsStrongBoxBacked(true).build())
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.w("CryptoManager", "StrongBox 不可用，回退到 TEE：${e.message}")
            keyGenerator.init(builder.setIsStrongBoxBacked(false).build())
            keyGenerator.generateKey()
        }
    }

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = encryptCipher
        val ciphertext = cipher.doFinal(data)
        return cipher.iv + ciphertext
    }

    fun decrypt(combined: ByteArray): ByteArray {
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)
        val cipher = getDecryptCipherForIv(iv)
        return cipher.doFinal(ciphertext)
    }

    companion object {
        private const val ALIAS = "authenticator_secret_key"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    }
}