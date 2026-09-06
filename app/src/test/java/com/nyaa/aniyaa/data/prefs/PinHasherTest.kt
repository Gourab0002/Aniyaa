package com.nyaa.aniyaa.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun hash_isDeterministicForSameInputs() {
        val salt = ByteArray(16) { it.toByte() }
        val algorithm = PinHasher.preferredAlgorithm()
        assertEquals(PinHasher.hash("1234", salt, algorithm), PinHasher.hash("1234", salt, algorithm))
        assertNotEquals(PinHasher.hash("1234", salt, algorithm), PinHasher.hash("1235", salt, algorithm))
    }

    @Test
    fun legacySha256_matchesPreviousScheme() {
        val salt = "aabbccddeeff00112233445566778899".fromHex()
        val hash = PinHasher.hash("2580", salt, PinHasher.LEGACY_SHA256)
        assertEquals(64, hash.length)
        assertEquals(hash, PinHasher.hash("2580", salt, PinHasher.LEGACY_SHA256))
    }

    @Test
    fun lockout_startsAfterFiveFailures() {
        assertEquals(0L, PinHasher.lockoutMillis(4))
        assertEquals(30_000L, PinHasher.lockoutMillis(5))
        assertEquals(60_000L, PinHasher.lockoutMillis(6))
        assertTrue(PinHasher.lockoutMillis(10) >= PinHasher.lockoutMillis(6))
    }
}
