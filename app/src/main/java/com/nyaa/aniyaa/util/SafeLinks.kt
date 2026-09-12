package com.nyaa.aniyaa.util

fun isSafeHttpUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return false
    val schemeEnd = trimmed.indexOf(':')
    if (schemeEnd <= 0) return false
    val scheme = trimmed.substring(0, schemeEnd).lowercase()
    return scheme == "https" || scheme == "http"
}
