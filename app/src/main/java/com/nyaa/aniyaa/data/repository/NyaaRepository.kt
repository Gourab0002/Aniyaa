package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.api.NyaaRssParser
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.Torrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NyaaRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun search(params: SearchParams): Result<List<Torrent>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(params)
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Aniyaa/1.0 (Android)")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body ?: return@withContext Result.failure(Exception("Empty response"))
                    val torrents = NyaaRssParser.parse(body.byteStream())
                    Result.success(torrents)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildUrl(params: SearchParams): String {
        val sb = StringBuilder("https://nyaa.si/?page=rss")
        if (params.query.isNotBlank()) {
            sb.append("&q=${java.net.URLEncoder.encode(params.query.trim(), "UTF-8")}")
        }
        sb.append("&c=${params.category.value}")
        sb.append("&f=${params.filter.value}")
        sb.append("&s=${params.sortField.value}")
        sb.append("&o=${params.sortOrder.value}")
        return sb.toString()
    }
}
