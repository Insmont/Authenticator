package org.insmont.authenticator.core.util

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object OtpUtils {
    fun generateTotp(
        secret: ByteArray,
        decrypt: ((ByteArray) -> ByteArray)? = null,
        currentTime: Long = System.currentTimeMillis(),
        period: Int = 30,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): String {
        if (secret.isEmpty()) return "0".padStart(length = digits, padChar = '0')
        var decryptedSecret: ByteArray? = null
        try {
            decryptedSecret = if (decrypt != null) decrypt(secret) else secret
            val counter = currentTime / 1000 / period
            val data = ByteBuffer.allocate(8).putLong(counter).array()

            val macAlgorithm = when (algorithm.uppercase()) {
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> "HmacSHA1"
            }

            val mac = Mac.getInstance(macAlgorithm)
            mac.init(SecretKeySpec(decryptedSecret, macAlgorithm))
            val hash = mac.doFinal(data)

            val offset = hash[hash.size - 1].toInt() and 0xf
            val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)

            val otp = binary % 10.0.pow(digits.toDouble()).toInt()
            val result = otp.toString().padStart(length = digits, padChar = '0')
            
            hash.fill(0)
            data.fill(0)
            
            return result
        } catch (_: Exception) {
            return "0".padStart(length = digits, padChar = '0')
        } finally {
            if (decrypt != null) decryptedSecret?.fill(0)
        }
    }

    fun calculateProgress(currentTime: Long, period: Int): Float {
        val millisInPeriod = period * 1000L
        val elapsedMillis = currentTime % millisInPeriod
        return 1f - (elapsedMillis.toFloat() / millisInPeriod.toFloat())
    }

    fun calculateRemainingSeconds(currentTime: Long, period: Int): Int {
        val elapsed = (currentTime / 1000) % period
        return (period - elapsed).toInt()
    }

    fun decodeBase32(base32: ByteArray): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var bits = 0
        var value = 0L
        
        var charCount = 0
        for (b in base32) {
            val char = b.toInt().toChar().uppercaseChar()
            if (char == ' ' || char == '-' || char == '=') continue
            if (alphabet.indexOf(char) != -1) charCount++
        }
        
        val output = ByteArray((charCount * 5 + 7) / 8)
        var pos = 0

        for (byte in base32) {
            val char = byte.toInt().toChar().uppercaseChar()
            if (char == ' ' || char == '-' || char == '=') continue
            val index = alphabet.indexOf(char)
            if (index == -1) continue
            
            value = (value shl 5) or index.toLong()
            bits += 5
            
            while (bits >= 8) {
                if (pos < output.size) {
                    output[pos++] = ((value shr (bits - 8)) and 0xFFL).toByte()
                }
                bits -= 8
            }
        }

        if (bits > 0 && pos < output.size) {
            output[pos] = ((value shl (8 - bits)) and 0xFFL).toByte()
        }

        return output
    }
}