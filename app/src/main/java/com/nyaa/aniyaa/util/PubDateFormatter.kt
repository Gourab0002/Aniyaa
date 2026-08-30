package com.nyaa.aniyaa.util

import java.text.SimpleDateFormat
import java.util.LinkedHashMap
import java.util.Locale

object PubDateFormatter {
    private const val CACHE_LIMIT = 128

    private val inputPatterns = arrayOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z"
    )

    private val cache = object : LinkedHashMap<String, String>(CACHE_LIMIT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > CACHE_LIMIT
    }

    fun format(raw: String): String {
        if (raw.isBlank()) return ""
        synchronized(cache) {
            cache[raw]?.let { return it }
        }
        val parsed = parse(raw) ?: return raw
        val formatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed)
        synchronized(cache) {
            cache[raw] = formatted
        }
        return formatted
    }

    internal fun parse(raw: String): java.util.Date? {
        for (pattern in inputPatterns) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US).parse(raw)
                if (parsed != null) return parsed
            } catch (_: Exception) {
            }
        }
        return null
    }
}
