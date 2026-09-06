package com.nyaa.aniyaa.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatCount_thresholds() {
        assertEquals("12", formatCount(12))
        assertEquals("1.2k", formatCount(1200))
        assertEquals("12k", formatCount(12_400))
        assertEquals("1.2M", formatCount(1_200_000))
    }
}
