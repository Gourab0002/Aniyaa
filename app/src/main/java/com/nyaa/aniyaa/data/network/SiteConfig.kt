package com.nyaa.aniyaa.data.network

import com.nyaa.aniyaa.data.model.CatalogSite
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object SiteConfig {
    private val currentRef = AtomicReference(CatalogSite.NYAA)
    private val bases = ConcurrentHashMap<CatalogSite, String>().apply {
        CatalogSite.entries.forEach { put(it, it.defaultBase) }
    }
    private val sessionBases = ConcurrentHashMap<CatalogSite, String>()

    var currentSite: CatalogSite
        get() = currentRef.get()
        set(value) {
            currentRef.set(value)
        }

    val baseUrl: String
        get() = baseUrl(currentSite)

    fun baseUrl(site: CatalogSite): String = bases[site] ?: site.defaultBase

    fun resolvedBaseUrl(site: CatalogSite): String = sessionBases[site] ?: baseUrl(site)

    fun setBaseUrl(site: CatalogSite, url: String) {
        bases[site] = normalize(url, site)
        sessionBases.remove(site)
    }

    fun setSessionBaseUrl(site: CatalogSite, url: String) {
        sessionBases[site] = normalize(url, site)
    }

    fun clearSessionBaseUrl(site: CatalogSite) {
        sessionBases.remove(site)
    }

    fun normalize(raw: String, site: CatalogSite = currentSite): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return site.defaultBase
        val withScheme = when {
            trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("http://") -> "https://${trimmed.removePrefix("http://")}"
            else -> "https://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    fun resolveUrl(pathOrUrl: String, site: CatalogSite = currentSite): String {
        val value = pathOrUrl.trim()
        if (value.isEmpty()) return ""
        val base = resolvedBaseUrl(site)
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("//") -> "https:$value"
            value.startsWith("/") -> base + value
            else -> "$base/$value"
        }
    }

    fun isCatalogHost(host: String): Boolean {
        val normalized = host.lowercase().removePrefix("www.")
        if (normalized.isBlank()) return false
        if (normalized.contains("nyaa")) return true
        CatalogSite.entries.forEach { site ->
            val configured = hostOf(baseUrl(site))
            if (configured.isNotBlank() && configured == normalized) return true
            if (site.defaultHost == normalized) return true
        }
        return false
    }

    private fun hostOf(url: String): String {
        val withoutScheme = url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("//")
        return withoutScheme.substringBefore("/").substringBefore("?").substringBefore(":")
            .lowercase().removePrefix("www.")
    }
}
