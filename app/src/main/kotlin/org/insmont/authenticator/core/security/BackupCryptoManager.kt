package org.insmont.authenticator.core.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.koin.core.annotation.Single

@Single
class BackupCryptoManager {
    class InvalidPasswordException : Exception("无效密码")

    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_SIZE).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(IV_SIZE).apply { SecureRandom().nextBytes(this) }

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
        }

        val ciphertext = cipher.doFinal(data)
        return salt + iv + ciphertext
    }

    fun decrypt(encryptedData: ByteArray, password: CharArray): ByteArray {
        if (encryptedData.size < SALT_SIZE + IV_SIZE) {
            throw IllegalArgumentException("无效数据")
        }

        val salt = encryptedData.sliceArray(0 until SALT_SIZE)
        val iv = encryptedData.sliceArray(SALT_SIZE until SALT_SIZE + IV_SIZE)
        val ciphertext = encryptedData.sliceArray(SALT_SIZE + IV_SIZE until encryptedData.size)

        val secretKey = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
            }
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("Tag mismatch") || msg.contains("bad decrypt") || msg.contains("Padding")) {
                throw InvalidPasswordException()
            }
            throw e
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val key = factory.generateSecret(spec).encoded
        val secretKeySpec = SecretKeySpec(key, AES_ALGORITHM)
        key.fill(0)
        spec.clearPassword()
        return secretKeySpec
    }

    companion object {
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512"
        private const val AES_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ITERATIONS = 100000
        private const val KEY_LENGTH = 256
        private const val SALT_SIZE = 16
        private const val IV_SIZE = 12
        private const val TAG_LENGTH = 128
    }
}