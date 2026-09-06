package com.nyaa.aniyaa.data.prefs

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    const val PBKDF2_SHA256 = "PBKDF2WithHmacSHA256"
    const val PBKDF2_SHA1 = "PBKDF2WithHmacSHA1"
    const val LEGACY_SHA256 = "SHA-256"
    const val ITERATIONS = 120_000
    const val KEY_LENGTH_BITS = 256

    fun preferredAlgorithm(): String {
        return try {
            SecretKeyFactory.getInstance(PBKDF2_SHA256)
            PBKDF2_SHA256
        } catch (_: Exception) {
            try {
                SecretKeyFactory.getInstance(PBKDF2_SHA1)
                PBKDF2_SHA1
            } catch (_: Exception) {
                LEGACY_SHA256
            }
        }
    }

    fun hash(pin: String, salt: ByteArray, algorithm: String = preferredAlgorithm()): String {
        return when (algorithm) {
            LEGACY_SHA256 -> {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(salt)
                digest.update(pin.toByteArray(Charsets.UTF_8))
                digest.digest().toHex()
            }
            else -> {
                val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
                val factory = SecretKeyFactory.getInstance(algorithm)
                try {
                    factory.generateSecret(spec).encoded.toHex()
                } finally {
                    spec.clearPassword()
                }
            }
        }
    }

    fun lockoutMillis(failures: Int): Long {
        if (failures < 5) return 0L
        val steps = (failures - 4).coerceAtMost(5)
        return 30_000L * (1L shl (steps - 1))
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun String.fromHex(): ByteArray {
    if (length % 2 != 0) return ByteArray(0)
    return ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
