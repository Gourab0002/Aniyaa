package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.api.NyaaCommentParser
import com.nyaa.aniyaa.data.api.NyaaHtmlSearchParser
import com.nyaa.aniyaa.data.api.NyaaRssParser
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.model.TorrentPageData
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.network.HttpException
import com.nyaa.aniyaa.data.network.SiteConfig
import com.nyaa.aniyaa.data.network.await
import com.nyaa.aniyaa.data.network.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

internal const val NYAA_PAGE_SIZE = 75

internal fun torrentIdentity(torrent: Torrent): String = torrent.bookmarkKey()

internal fun mergeSearchPages(
    existing: List<Torrent>,
    incoming: List<Torrent>,
    replace: Boolean
): Pair<List<Torrent>, Boolean> {
    if (replace) {
        val unique = incoming.distinctBy(::torrentIdentity)
        val canLoadMore = incoming.size >= NYAA_PAGE_SIZE && unique.isNotEmpty()
        return unique to canLoadMore
    }
    if (incoming.isEmpty()) return existing to false
    val seen = existing.mapTo(HashSet(existing.size + incoming.size)) { torrentIdentity(it) }
    val added = ArrayList<Torrent>(incoming.size)
    for (torrent in incoming) {
        if (seen.add(torrentIdentity(torrent))) {
            added.add(torrent)
        }
    }
    val merged = if (added.isEmpty()) existing else existing + added
    val canLoadMore = added.isNotEmpty() && incoming.size >= NYAA_PAGE_SIZE
    return merged to canLoadMore
}

internal fun buildSearchUrl(
    params: SearchParams,
    baseUrl: String = SiteConfig.baseUrl(params.site),
    rss: Boolean = true
): String {
    val base = baseUrl.trimEnd('/')
    val rawQuery = params.query.trim()
    val userMatch = USER_QUERY_REGEX.find(rawQuery)
    val username = userMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
    val remainder = if (userMatch != null) rawQuery.removePrefix(userMatch.value).trim() else rawQuery
    val parts = mutableListOf<String>()
    if (rss) parts += "page=rss"
    if (remainder.isNotBlank()) parts += "q=${URLEncoder.encode(remainder, "UTF-8")}"
    if (username.isNotEmpty()) parts += "u=${URLEncoder.encode(username, "UTF-8")}"
    parts += "c=${params.category.value}"
    parts += "f=${params.filter.value}"
    parts += "s=${params.sortField.value}"
    parts += "o=${params.sortOrder.value}"
    if (params.page > 1) parts += "p=${params.page}"
    val query = parts.joinToString("&")
    return if (username.isNotEmpty()) {
        "$base/user/${URLEncoder.encode(username, "UTF-8")}?$query"
    } else {
        "$base/?$query"
    }
}

private val USER_QUERY_REGEX = Regex("^user:([^\\s]+)", RegexOption.IGNORE_CASE)

class NyaaRepository(
    private val client: okhttp3.OkHttpClient = AppHttpClient.instance
) {
    suspend fun search(params: SearchParams, fromCache: Boolean = false, forceNetwork: Boolean = false): Result<List<Torrent>> {
        return withContext(Dispatchers.IO) {
            val rssResult = fetchRss(params, fromCache, forceNetwork)
            if (fromCache) return@withContext rssResult
            if (rssResult.isSuccess) return@withContext rssResult
            val htmlResult = fetchHtml(params, forceNetwork)
            if (htmlResult.isSuccess) htmlResult else rssResult
        }
    }

    suspend fun fetchTorrentPageData(
        torrentId: String,
        site: CatalogSite = SiteConfig.currentSite
    ): Result<TorrentPageData> {
        return withContext(Dispatchers.IO) {
            runCatchingRequest {
                val baseUrl = SiteConfig.baseUrl(site)
                val request = requestBuilder("$baseUrl/view/$torrentId").build()
                client.newCall(request).await().use { response ->
                    if (!response.isSuccessful) {
                        throw HttpException(response.code, "HTTP ${response.code}: ${response.message}")
                    }
                    val html = response.body?.string()
                        ?: throw IllegalStateException("Empty response")
                    NyaaCommentParser.parse(html, baseUrl)
                }
            }
        }
    }

    suspend fun fetchTorrent(
        torrentId: String,
        fallback: Torrent? = null,
        site: CatalogSite = fallback?.site ?: SiteConfig.currentSite
    ): Result<Torrent> {
        return fetchTorrentPageData(torrentId, site).map { page ->
            NyaaCommentParser.toTorrent(page, torrentId, fallback, site)
        }
    }

    private suspend fun fetchRss(
        params: SearchParams,
        fromCache: Boolean,
        forceNetwork: Boolean
    ): Result<List<Torrent>> = runCatchingRequest {
        val baseUrl = SiteConfig.baseUrl(params.site)
        val request = requestBuilder(buildSearchUrl(params, baseUrl, rss = true))
            .cacheControl(cacheControl(fromCache, forceNetwork))
            .build()
        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(response.code, "HTTP ${response.code}: ${response.message}")
            }
            val body = response.body ?: throw IllegalStateException("Empty response")
            NyaaRssParser.parse(body.byteStream()).map { it.copy(site = params.site) }
        }
    }

    private suspend fun fetchHtml(
        params: SearchParams,
        forceNetwork: Boolean
    ): Result<List<Torrent>> = runCatchingRequest {
        val baseUrl = SiteConfig.baseUrl(params.site)
        val request = requestBuilder(buildSearchUrl(params, baseUrl, rss = false))
            .cacheControl(cacheControl(fromCache = false, forceNetwork = forceNetwork))
            .header("Accept", "text/html")
            .build()
        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(response.code, "HTTP ${response.code}: ${response.message}")
            }
            val html = response.body?.string() ?: throw IllegalStateException("Empty response")
            NyaaHtmlSearchParser.parse(html, baseUrl).map { it.copy(site = params.site) }
        }
    }

    private fun requestBuilder(url: String): Request.Builder =
        AppHttpClient.newRequest(url).newBuilder()

    private fun cacheControl(fromCache: Boolean, forceNetwork: Boolean): CacheControl {
        return when {
            fromCache -> CacheControl.FORCE_CACHE
            forceNetwork -> CacheControl.Builder().noCache().maxAge(0, TimeUnit.SECONDS).build()
            else -> CacheControl.Builder().maxAge(60, TimeUnit.SECONDS).build()
        }
    }

    private inline fun <T> runCatchingRequest(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage(), e))
        }
    }
}
