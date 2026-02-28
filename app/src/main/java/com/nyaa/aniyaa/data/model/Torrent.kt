package com.nyaa.aniyaa.data.model

data class Torrent(
    val id: String,
    val title: String,
    val link: String,
    val guid: String,
    val pubDate: String,
    val seeders: Int,
    val leechers: Int,
    val downloads: Int,
    val infoHash: String,
    val category: String,
    val size: String,
    val comments: Int,
    val trusted: Boolean,
    val remake: Boolean,
    val magnetLink: String
)

enum class SortField(val value: String, val displayName: String) {
    DATE("id", "Date"),
    SEEDERS("seeders", "Seeders"),
    LEECHERS("leechers", "Leechers"),
    SIZE("size", "Size"),
    DOWNLOADS("downloads", "Downloads"),
    COMMENTS("comments", "Comments")
}

enum class SortOrder(val value: String, val displayName: String) {
    DESC("desc", "Descending"),
    ASC("asc", "Ascending")
}

enum class FilterOption(val value: Int, val displayName: String) {
    ALL(0, "No Filter"),
    NO_REMAKES(1, "No Remakes"),
    TRUSTED(2, "Trusted Only")
}

data class Category(val value: String, val displayName: String)

val CATEGORIES = listOf(
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

data class SearchParams(
    val query: String = "",
    val category: Category = CATEGORIES[0],
    val filter: FilterOption = FilterOption.ALL,
    val sortField: SortField = SortField.DATE,
    val sortOrder: SortOrder = SortOrder.DESC,
    val page: Int = 1
)
