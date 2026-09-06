package com.nyaa.aniyaa.ui.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockController {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    fun lock() {
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    fun prepare(enabled: Boolean, hasPin: Boolean) {
        _locked.value = enabled && hasPin
    }
}
