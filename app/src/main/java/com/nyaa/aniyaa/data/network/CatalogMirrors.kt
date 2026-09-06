package com.nyaa.aniyaa.data.network

import com.nyaa.aniyaa.data.model.CatalogSite

object CatalogMirrors {
    val nyaa = listOf(
        "https://nyaa.si",
        "https://nyaa.iss.one"
    )

    val sukebei = listOf(
        "https://sukebei.nyaa.si",
        "https://sukebei.nyaa.iss.one"
    )

    fun forSite(site: CatalogSite): List<String> = when (site) {
        CatalogSite.NYAA -> nyaa
        CatalogSite.SUKEBEI -> sukebei
    }

    fun label(url: String, site: CatalogSite): String {
        val normalized = SiteConfig.normalize(url, site)
        return if (normalized.equals(site.defaultBase, ignoreCase = true)) {
            "Official"
        } else {
            normalized.removePrefix("https://").removePrefix("http://")
        }
    }
}
