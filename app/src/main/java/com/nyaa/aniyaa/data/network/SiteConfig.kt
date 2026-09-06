package com.nyaa.aniyaa.data.network

import com.nyaa.aniyaa.data.model.CatalogSite
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object SiteConfig {
    private val currentRef = AtomicReference(CatalogSite.NYAA)
    private val bases = ConcurrentHashMap<CatalogSite, String>().apply {
        CatalogSite.entries.forEach { put(it, it.defaultBase) }
    }

    var currentSite: CatalogSite
        get() = currentRef.get()
        set(value) {
            currentRef.set(value)
        }

    val baseUrl: String
        get() = baseUrl(currentSite)

    fun baseUrl(site: CatalogSite): String = bases[site] ?: site.defaultBase

    fun setBaseUrl(site: CatalogSite, url: String) {
        bases[site] = normalize(url, site)
    }

    fun normalize(raw: String, site: CatalogSite = currentSite): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return site.defaultBase
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    fun resolveUrl(pathOrUrl: String, site: CatalogSite = currentSite): String {
        val value = pathOrUrl.trim()
        if (value.isEmpty()) return ""
        val base = baseUrl(site)
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("//") -> "https:$value"
            value.startsWith("/") -> base + value
            else -> "$base/$value"
        }
    }
}
