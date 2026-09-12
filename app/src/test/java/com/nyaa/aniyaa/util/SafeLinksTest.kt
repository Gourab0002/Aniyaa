package com.nyaa.aniyaa.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLinksTest {
    @Test
    fun allowsHttpAndHttps() {
        assertTrue(isSafeHttpUrl("https://nyaa.si/view/1"))
        assertTrue(isSafeHttpUrl("http://example.com/a"))
    }

    @Test
    fun rejectsOtherSchemes() {
        assertFalse(isSafeHttpUrl("intent://evil"))
        assertFalse(isSafeHttpUrl("file:///sdcard/x"))
        assertFalse(isSafeHttpUrl("javascript:alert(1)"))
        assertFalse(isSafeHttpUrl(""))
    }
}
