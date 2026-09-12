package com.nyaa.aniyaa.data.backup

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val SALT_BYTES = 16

    fun isEncrypted(json: String): Boolean {
        return try {
            JSONObject(json).optBoolean("encrypted", false)
        } catch (_: Exception) {
            false
        }
    }

    fun encrypt(plaintext: String, password: String): String {
        require(password.isNotBlank()) { "Password required" }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return JSONObject().apply {
            put("version", 4)
            put("encrypted", true)
            put("kdf", KDF)
            put("iter", ITERATIONS)
            put("salt", salt.toB64())
            put("iv", iv.toB64())
            put("data", ciphertext.toB64())
        }.toString(2)
    }

    fun decrypt(json: String, password: String): String {
        val root = JSONObject(json)
        if (!root.optBoolean("encrypted", false)) return json
        require(password.isNotBlank()) { "This backup is password-protected" }
        val salt = root.getString("salt").fromB64()
        val iv = root.getString("iv").fromB64()
        val data = root.getString("data").fromB64()
        val iterations = root.optInt("iter", ITERATIONS).coerceAtLeast(10_000)
        val key = derive(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return try {
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (_: Exception) {
            throw IllegalArgumentException("Wrong backup password")
        }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            val raw = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            SecretKeySpec(raw, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toB64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
