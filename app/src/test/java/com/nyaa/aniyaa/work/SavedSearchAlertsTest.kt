package com.nyaa.aniyaa.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSearchAlertsTest {

    @Test
    fun newIds_skipsFirstSnapshot() {
        assertTrue(SavedSearchAlerts.newIds(listOf("1", "2"), "").isEmpty())
    }

    @Test
    fun newIds_returnsOnlyUnseen() {
        assertEquals(listOf("3"), SavedSearchAlerts.newIds(listOf("3", "2", "1"), "1,2"))
    }

    @Test
    fun storeIds_limitsAndDedupes() {
        assertEquals("1,2,3", SavedSearchAlerts.storeIds(listOf("1", "1", "2", "3"), limit = 10))
        assertEquals("1,2", SavedSearchAlerts.storeIds(listOf("1", "2", "3"), limit = 2))
    }

    @Test
    fun inboxLines_addsOverflow() {
        val lines = SavedSearchAlerts.inboxLines(listOf("A", "B", "C", "D", "E", "F"), 6)
        assertEquals(6, lines.size)
        assertEquals("and 1 more", lines.last())
        assertEquals("1 new listing", SavedSearchAlerts.contentText(1))
        assertEquals("2 new listings", SavedSearchAlerts.contentText(2))
    }
}
