package com.nyaa.aniyaa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseTitleTest {

    @Test
    fun parse_subsPleaseStyle() {
        val parsed = parseReleaseTitle("[SubsPlease] Bocchi the Rock! - 12 (1080p) [ABCDEF].mkv")
        assertEquals("SubsPlease", parsed.group)
        assertEquals("Bocchi the Rock!", parsed.show)
    }

    @Test
    fun parse_withoutGroup() {
        val parsed = parseReleaseTitle("Some Movie (2024)")
        assertNull(parsed.group)
        assertEquals("Some Movie", parsed.show)
    }
}
