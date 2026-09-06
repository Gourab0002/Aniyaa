package com.nyaa.aniyaa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class PubDateFormatterTest {

    @Test
    fun format_rfc822KeepsYear() {
        val formatted = PubDateFormatter.format("Wed, 01 Jan 2025 00:00:00 -0000")
        assertTrue(formatted.contains("2025"))
        assertTrue(formatted.contains("Jan"))
        val parsed = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).parse(formatted)
        assertNotNull(parsed)
    }

    @Test
    fun format_blankStaysBlank() {
        assertEquals("", PubDateFormatter.format("   "))
    }

    @Test
    fun format_unknownReturnsOriginal() {
        assertEquals("not-a-date", PubDateFormatter.format("not-a-date"))
    }

    @Test
    fun formatRelative_recentIsShort() {
        val now = 1_735_689_600_000L
        val twoHoursAgo = "Wed, 31 Dec 2024 22:00:00 +0000"
        val formatted = PubDateFormatter.formatRelative(twoHoursAgo, now)
        assertTrue(formatted.endsWith("ago") || formatted.contains("2024") || formatted.contains("Dec"))
    }
}
