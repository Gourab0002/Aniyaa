package com.nyaa.aniyaa.data.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AppHttpClient {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    private const val CACHE_SIZE_BYTES = 10L * 1024L * 1024L
    private const val DEFAULT_MAX_AGE_SECONDS = 60

    private val cacheDirectory = AtomicReference<File?>(null)

    fun configure(cacheDir: File) {
        cacheDirectory.compareAndSet(null, File(cacheDir, "http"))
    }

    val instance: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 16
            maxRequestsPerHost = 8
        }
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addNetworkInterceptor(shortLivedGetCacheInterceptor())
        cacheDirectory.get()?.let { dir ->
            dir.mkdirs()
            builder.cache(Cache(dir, CACHE_SIZE_BYTES))
        }
        builder.build()
    }

    fun clearCache() {
        runCatching { instance.cache?.evictAll() }
    }

    val imageClient: OkHttpClient by lazy {
        instance.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("Referer", "https://nyaa.si/")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun newRequest(url: String): Request =
        Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .build()

    private fun shortLivedGetCacheInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.method != "GET" || !response.isSuccessful) {
            return@Interceptor response
        }
        if (!SiteConfig.isCatalogHost(request.url.host)) {
            return@Interceptor response
        }
        response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", "public, max-age=$DEFAULT_MAX_AGE_SECONDS")
            .build()
    }
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    })
    continuation.invokeOnCancellation {
        try {
            cancel()
        } catch (_: Exception) {
        }
    }
}
