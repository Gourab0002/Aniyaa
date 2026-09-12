package com.nyaa.aniyaa.data.network

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun Throwable.isFailoverWorthy(): Boolean {
    val http = httpStatusCode()
    return when {
        this is UnknownHostException || cause is UnknownHostException -> true
        this is SocketTimeoutException || cause is SocketTimeoutException -> true
        this is SSLException || cause is SSLException -> true
        http == 403 || http == 429 || http == 451 -> true
        http != null && http >= 500 -> true
        this is IOException -> http != 404
        else -> cause?.isFailoverWorthy() == true
    }
}

fun Throwable.toUserMessage(): String {
    val httpCode = httpStatusCode()
    return when {
        this is UnknownHostException ->
            "Can't reach the site. It may be blocked — try a mirror in Settings."
        this is SocketTimeoutException ->
            "The site took too long to respond. Check your connection or try a mirror in Settings."
        this is SSLException ->
            "Secure connection failed. The site may be blocked — try a mirror in Settings."
        httpCode == 403 || httpCode == 451 ->
            "The site blocked this request. Try a mirror in Settings."
        httpCode != null && httpCode >= 500 ->
            "The site is down right now. Try again, or set a mirror in Settings."
        httpCode == 404 ->
            "That listing was not found. It may have been removed."
        this is IOException ->
            "Network error. Check your connection, or try a mirror in Settings."
        !message.isNullOrBlank() -> message.orEmpty()
        else -> "Something went wrong. Please try again."
    }
}

private fun Throwable.httpStatusCode(): Int? {
    val fromMessage = Regex("HTTP\\s+(\\d{3})").find(message.orEmpty())
        ?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (fromMessage != null) return fromMessage
    return cause?.httpStatusCode()
}

class HttpException(val code: Int, override val message: String) : IOException(message)
