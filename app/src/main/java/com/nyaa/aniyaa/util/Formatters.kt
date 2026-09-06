package com.nyaa.aniyaa.util

fun formatCount(value: Int): String {
    val n = value.toLong()
    return when {
        n < 1_000 -> n.toString()
        n < 10_000 -> {
            val tenths = (n * 10 / 1_000)
            val whole = tenths / 10
            val frac = tenths % 10
            if (frac == 0L) "${whole}k" else "$whole.${frac}k"
        }
        n < 1_000_000 -> "${n / 1_000}k"
        else -> {
            val tenths = n * 10 / 1_000_000
            val whole = tenths / 10
            val frac = tenths % 10
            if (frac == 0L) "${whole}M" else "$whole.${frac}M"
        }
    }
}
