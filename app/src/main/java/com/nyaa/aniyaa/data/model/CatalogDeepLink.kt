package com.nyaa.aniyaa.data.model

data class CatalogDeepLink(
    val site: CatalogSite,
    val viewId: String? = null,
    val savedSearchId: Long? = null,
    val searchParams: SearchParams? = null,
    val requiresNsfw: Boolean = false
) {
    val isView: Boolean get() = !viewId.isNullOrBlank()
    val isSavedSearch: Boolean get() = savedSearchId != null
}

data class SimpleUri(
    val scheme: String,
    val host: String,
    val pathSegments: List<String>,
    val query: Map<String, String>
) {
    val lastPathSegment: String? get() = pathSegments.lastOrNull()
}

object CatalogDeepLinks {

    fun parse(url: String, fallbackSite: CatalogSite = CatalogSite.NYAA): CatalogDeepLink? {
        val uri = parseSimpleUri(url, fallbackSite) ?: return null
        return parse(uri, fallbackSite)
    }

    fun parse(uri: SimpleUri, fallbackSite: CatalogSite = CatalogSite.NYAA): CatalogDeepLink {
        val host = uri.host
        val segments = uri.pathSegments
        val linkedSite = CatalogSite.fromHost(host) ?: CatalogSite.fromUrl(uri.toUrlHint())
        val viewId = when {
            host == "view" -> uri.lastPathSegment
            segments.firstOrNull() == "view" -> segments.getOrNull(1)
            else -> null
        }?.substringBefore("#")?.takeIf { it.isNotBlank() }
        val savedId = if (host == "saved") uri.lastPathSegment?.toLongOrNull() else null
        val site = linkedSite ?: fallbackSite
        if (viewId != null) {
            return CatalogDeepLink(site = site, viewId = viewId, requiresNsfw = site.nsfw)
        }
        if (savedId != null) {
            return CatalogDeepLink(site = site, savedSearchId = savedId, requiresNsfw = site.nsfw)
        }
        val user = if (segments.firstOrNull() == "user") segments.getOrNull(1).orEmpty() else ""
        val query = uri.query["q"].orEmpty()
        val category = uri.query["c"]
        val filter = uri.query["f"]?.toIntOrNull()
        val sort = uri.query["s"]
        val order = uri.query["o"]
        if (user.isNotBlank() || query.isNotBlank() || !category.isNullOrBlank()) {
            val combined = if (user.isNotBlank()) {
                "user:$user ${query}".trim()
            } else {
                query
            }
            return CatalogDeepLink(
                site = site,
                searchParams = SearchParams(
                    query = combined,
                    site = site,
                    category = categoryByValue(category ?: "0_0", site),
                    filter = filterByValue(filter ?: 0),
                    sortField = sortFieldByValue(sort ?: "id"),
                    sortOrder = sortOrderByValue(order ?: "desc")
                ),
                requiresNsfw = site.nsfw
            )
        }
        return CatalogDeepLink(
            site = site,
            searchParams = SearchParams(site = site),
            requiresNsfw = site.nsfw
        )
    }

    fun parseSimpleUri(raw: String, fallbackSite: CatalogSite = CatalogSite.NYAA): SimpleUri? {
        var value = raw.trim()
        if (value.isEmpty()) return null
        val hash = value.indexOf('#')
        if (hash >= 0) value = value.substring(0, hash)
        if (value.startsWith("/")) {
            val pathAndQuery = value
            val q = pathAndQuery.indexOf('?')
            val pathPart = if (q >= 0) pathAndQuery.substring(0, q) else pathAndQuery
            val queryPart = if (q >= 0) pathAndQuery.substring(q + 1) else ""
            return SimpleUri(
                scheme = "https",
                host = fallbackSite.defaultHost,
                pathSegments = pathPart.split('/').filter { it.isNotEmpty() },
                query = parseQuery(queryPart)
            )
        }
        var scheme = ""
        val rest: String
        val schemeIdx = value.indexOf("://")
        when {
            schemeIdx >= 0 -> {
                scheme = value.substring(0, schemeIdx)
                rest = value.substring(schemeIdx + 3)
            }
            value.startsWith("//") -> {
                scheme = "https"
                rest = value.drop(2)
            }
            else -> return null
        }
        val slash = rest.indexOf('/')
        val qmark = rest.indexOf('?')
        val host: String
        val pathPart: String
        val queryPart: String
        when {
            slash < 0 && qmark < 0 -> {
                host = rest
                pathPart = ""
                queryPart = ""
            }
            qmark >= 0 && (slash < 0 || qmark < slash) -> {
                host = rest.substring(0, qmark)
                pathPart = ""
                queryPart = rest.substring(qmark + 1)
            }
            else -> {
                host = rest.substring(0, slash)
                val after = rest.substring(slash)
                val q = after.indexOf('?')
                if (q >= 0) {
                    pathPart = after.substring(0, q)
                    queryPart = after.substring(q + 1)
                } else {
                    pathPart = after
                    queryPart = ""
                }
            }
        }
        return SimpleUri(
            scheme = scheme,
            host = host.substringBefore(':').lowercase().removePrefix("www."),
            pathSegments = pathPart.split('/').filter { it.isNotEmpty() },
            query = parseQuery(queryPart)
        )
    }

    private fun parseQuery(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val out = linkedMapOf<String, String>()
        raw.split('&').forEach { part ->
            if (part.isEmpty()) return@forEach
            val eq = part.indexOf('=')
            if (eq < 0) {
                out[urlDecode(part)] = ""
            } else {
                out[urlDecode(part.substring(0, eq))] = urlDecode(part.substring(eq + 1))
            }
        }
        return out
    }

    private fun urlDecode(value: String): String = try {
        java.net.URLDecoder.decode(value.replace("+", "%20"), "UTF-8")
    } catch (_: Exception) {
        value
    }

    private fun SimpleUri.toUrlHint(): String =
        buildString {
            if (scheme.isNotBlank()) append(scheme).append("://")
            append(host)
            if (pathSegments.isNotEmpty()) {
                append('/')
                append(pathSegments.joinToString("/"))
            }
        }
}

fun stubTorrent(id: String, site: CatalogSite): Torrent = Torrent(
    id = id,
    title = "",
    link = "${site.defaultBase}/download/$id.torrent",
    guid = "${site.defaultBase}/view/$id",
    pubDate = "",
    seeders = 0,
    leechers = 0,
    downloads = 0,
    infoHash = "",
    category = "",
    size = "",
    comments = 0,
    trusted = false,
    remake = false,
    magnetLink = "",
    site = site
)
