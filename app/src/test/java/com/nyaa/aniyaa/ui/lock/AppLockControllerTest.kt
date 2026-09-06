package com.nyaa.aniyaa.ui.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockControllerTest {

    @Test
    fun gracePeriod_doesNotLockIfReturnedQuickly() {
        var now = 1_000L
        val lock = AppLockController { now }
        lock.prepare(enabled = true, hasPin = true)
        assertTrue(lock.locked.value)
        lock.unlock()
        lock.onBackground()
        now += 5_000L
        lock.onForeground(enabled = true, hasPin = true, graceMs = 15_000L)
        assertFalse(lock.locked.value)
    }

    @Test
    fun gracePeriod_locksAfterDelay() {
        var now = 1_000L
        val lock = AppLockController { now }
        lock.prepare(enabled = true, hasPin = true)
        lock.unlock()
        lock.onBackground()
        now += 20_000L
        lock.onForeground(enabled = true, hasPin = true, graceMs = 15_000L)
        assertTrue(lock.locked.value)
    }

    @Test
    fun immediateGrace_locksOnReturn() {
        var now = 1_000L
        val lock = AppLockController { now }
        lock.prepare(enabled = true, hasPin = true)
        lock.unlock()
        lock.onBackground()
        lock.onForeground(enabled = true, hasPin = true, graceMs = 0L)
        assertTrue(lock.locked.value)
    }
}
