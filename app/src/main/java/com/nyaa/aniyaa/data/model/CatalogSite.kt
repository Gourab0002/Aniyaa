package com.nyaa.aniyaa.data.model

enum class CatalogSite(
    val id: String,
    val displayName: String,
    val defaultHost: String,
    val defaultBase: String,
    val nsfw: Boolean
) {
    NYAA(
        id = "nyaa",
        displayName = "Nyaa",
        defaultHost = "nyaa.si",
        defaultBase = "https://nyaa.si",
        nsfw = false
    ),
    SUKEBEI(
        id = "sukebei",
        displayName = "Sukebei",
        defaultHost = "sukebei.nyaa.si",
        defaultBase = "https://sukebei.nyaa.si",
        nsfw = true
    );

    val categories: List<Category>
        get() = when (this) {
            NYAA -> NYAA_CATEGORIES
            SUKEBEI -> SUKEBEI_CATEGORIES
        }

    val primaryCategories: List<Category>
        get() = categories.filter { it.isPrimary }

    val viewOnLabel: String
        get() = when (this) {
            NYAA -> "View on Nyaa"
            SUKEBEI -> "View on Sukebei"
        }

    companion object {
        fun fromId(id: String): CatalogSite =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: NYAA

        fun fromHost(host: String): CatalogSite? {
            val normalized = host.lowercase().removePrefix("www.")
            if (normalized.isBlank()) return null
            if (normalized.contains("sukebei")) return SUKEBEI
            if (normalized == NYAA.defaultHost || normalized.endsWith(".nyaa.si")) return NYAA
            if (normalized.contains("nyaa")) return NYAA
            return null
        }

        fun fromUrl(url: String): CatalogSite? {
            val value = url.trim()
            if (value.isEmpty()) return null
            fromHost(hostOf(value))?.let { return it }
            return when {
                value.contains("sukebei", ignoreCase = true) -> SUKEBEI
                value.contains("nyaa", ignoreCase = true) -> NYAA
                else -> null
            }
        }

        private fun hostOf(url: String): String {
            val withoutScheme = url
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("//")
            return withoutScheme.substringBefore("/").substringBefore("?")
        }
    }
}

val NYAA_CATEGORIES = listOf(
    Category("0_0", "All Categories"),
    Category("1_0", "Anime"),
    Category("1_1", "Anime - AMV"),
    Category("1_2", "Anime - English"),
    Category("1_3", "Anime - Non-English"),
    Category("1_4", "Anime - Raw"),
    Category("2_0", "Audio"),
    Category("3_0", "Literature"),
    Category("4_0", "Live Action"),
    Category("4_1", "Live Action - English"),
    Category("4_4", "Live Action - Raw"),
    Category("5_0", "Pictures"),
    Category("6_0", "Software")
)

val SUKEBEI_CATEGORIES = listOf(
    Category("0_0", "All Categories"),
    Category("1_0", "Art"),
    Category("1_1", "Art - Anime"),
    Category("1_2", "Art - Doujinshi"),
    Category("1_3", "Art - Games"),
    Category("1_4", "Art - Manga"),
    Category("1_5", "Art - Pictures"),
    Category("2_0", "Real Life"),
    Category("2_1", "Real Life - Photobooks / Pictures"),
    Category("2_2", "Real Life - Videos")
)

@Deprecated("Use CatalogSite.categories", ReplaceWith("CatalogSite.NYAA.categories"))
val CATEGORIES: List<Category> = NYAA_CATEGORIES
