package com.nyaa.aniyaa.data.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class NetworkErrorsTest {

    @Test
    fun unknownHostSuggestsMirror() {
        val message = UnknownHostException("sukebei.nyaa.si").toUserMessage()
        assertTrue(message.contains("mirror", ignoreCase = true))
    }

    @Test
    fun http403SuggestsMirror() {
        val message = HttpException(403, "HTTP 403: Forbidden").toUserMessage()
        assertTrue(message.contains("blocked", ignoreCase = true))
    }
}
