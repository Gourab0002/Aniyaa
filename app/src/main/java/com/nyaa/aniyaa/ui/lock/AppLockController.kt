package com.nyaa.aniyaa.ui.lock

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockController(
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var lastBackgroundAt = 0L

    fun lock() {
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    fun prepare(enabled: Boolean, hasPin: Boolean) {
        _locked.value = enabled && hasPin
        lastBackgroundAt = 0L
    }

    fun onBackground() {
        lastBackgroundAt = elapsedRealtime()
    }

    fun onForeground(enabled: Boolean, hasPin: Boolean, graceMs: Long) {
        if (!enabled || !hasPin) {
            _locked.value = false
            return
        }
        if (lastBackgroundAt == 0L) return
        val elapsed = elapsedRealtime() - lastBackgroundAt
        if (elapsed >= graceMs) {
            _locked.value = true
        }
    }
}
