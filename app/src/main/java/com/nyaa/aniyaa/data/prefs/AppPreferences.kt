package com.nyaa.aniyaa.data.prefs

import android.content.Context
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.categoryByValue
import com.nyaa.aniyaa.data.model.darkModeByValue
import com.nyaa.aniyaa.data.model.filterByValue
import com.nyaa.aniyaa.data.model.sortFieldByValue
import com.nyaa.aniyaa.data.model.sortOrderByValue
import com.nyaa.aniyaa.data.network.SiteConfig
import java.security.MessageDigest
import java.security.SecureRandom

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeIndex: Int
        get() = prefs.getInt(KEY_THEME_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_INDEX, value).apply()

    var darkMode: DarkMode
        get() = darkModeByValue(prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.value).orEmpty())
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value.value).apply()

    var currentSite: CatalogSite
        get() {
            val stored = CatalogSite.fromId(prefs.getString(KEY_CURRENT_SITE, CatalogSite.NYAA.id).orEmpty())
            return if (stored.nsfw && !sukebeiEnabled) CatalogSite.NYAA else stored
        }
        set(value) {
            val resolved = if (value.nsfw && !sukebeiEnabled) CatalogSite.NYAA else value
            prefs.edit().putString(KEY_CURRENT_SITE, resolved.id).apply()
            SiteConfig.currentSite = resolved
        }

    var sukebeiAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_SUKEBEI_ACK, false)
        set(value) = prefs.edit().putBoolean(KEY_SUKEBEI_ACK, value).apply()

    var sukebeiEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUKEBEI_ENABLED, sukebeiAcknowledged)
        set(value) {
            prefs.edit().putBoolean(KEY_SUKEBEI_ENABLED, value).apply()
            if (value) {
                sukebeiAcknowledged = true
            } else if (CatalogSite.fromId(prefs.getString(KEY_CURRENT_SITE, "").orEmpty()).nsfw) {
                currentSite = CatalogSite.NYAA
            }
        }

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var preferredTorrentPackage: String
        get() = prefs.getString(KEY_TORRENT_PACKAGE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TORRENT_PACKAGE, value).apply()

    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    var hideScreenshots: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SCREENSHOTS, lockEnabled)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_SCREENSHOTS, value).apply()

    var hideFromRecents: Boolean
        get() = prefs.getBoolean(KEY_HIDE_RECENTS, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_RECENTS, value).apply()

    var lastUpdateCheckAt: Long
        get() = prefs.getLong(KEY_UPDATE_CHECK_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATE_CHECK_AT, value).apply()

    var dismissedUpdateVersion: String
        get() = prefs.getString(KEY_DISMISSED_UPDATE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_DISMISSED_UPDATE, value).apply()

    var roomMigrated: Boolean
        get() = prefs.getBoolean(KEY_ROOM_MIGRATED, false)
        set(value) = prefs.edit().putBoolean(KEY_ROOM_MIGRATED, value).apply()

    val hasPin: Boolean
        get() = !prefs.getString(KEY_PIN_HASH, "").isNullOrBlank()

    fun baseUrl(site: CatalogSite): String {
        val stored = prefs.getString(baseUrlKey(site), "").orEmpty()
        return SiteConfig.normalize(stored, site)
    }

    fun setBaseUrl(site: CatalogSite, url: String) {
        val normalized = SiteConfig.normalize(url, site)
        prefs.edit().putString(baseUrlKey(site), normalized).apply()
        SiteConfig.setBaseUrl(site, normalized)
    }

    fun defaultCategoryValue(site: CatalogSite): String =
        prefs.getString(siteKey(KEY_DEFAULT_CATEGORY, site), site.categories.first().value).orEmpty()

    fun setDefaultCategoryValue(site: CatalogSite, value: String) {
        prefs.edit().putString(siteKey(KEY_DEFAULT_CATEGORY, site), value).apply()
    }

    fun defaultSortFieldValue(site: CatalogSite = currentSite): String =
        prefs.getString(siteKey(KEY_DEFAULT_SORT_FIELD, site), SortField.DATE.value).orEmpty()

    fun setDefaultSortFieldValue(site: CatalogSite, value: String) {
        prefs.edit().putString(siteKey(KEY_DEFAULT_SORT_FIELD, site), value).apply()
    }

    fun defaultSortOrderValue(site: CatalogSite = currentSite): String =
        prefs.getString(siteKey(KEY_DEFAULT_SORT_ORDER, site), SortOrder.DESC.value).orEmpty()

    fun setDefaultSortOrderValue(site: CatalogSite, value: String) {
        prefs.edit().putString(siteKey(KEY_DEFAULT_SORT_ORDER, site), value).apply()
    }

    fun lastCategoryValue(site: CatalogSite): String =
        prefs.getString(siteKey(KEY_LAST_CATEGORY, site), defaultCategoryValue(site)).orEmpty()

    fun lastFilterValue(site: CatalogSite): Int =
        prefs.getInt(siteKey(KEY_LAST_FILTER, site), FilterOption.ALL.value)

    fun lastSortFieldValue(site: CatalogSite): String =
        prefs.getString(siteKey(KEY_LAST_SORT_FIELD, site), defaultSortFieldValue(site)).orEmpty()

    fun lastSortOrderValue(site: CatalogSite): String =
        prefs.getString(siteKey(KEY_LAST_SORT_ORDER, site), defaultSortOrderValue(site)).orEmpty()

    fun defaultCategory(site: CatalogSite = currentSite): Category =
        categoryByValue(defaultCategoryValue(site), site)

    fun defaultSearchParams(site: CatalogSite = currentSite): SearchParams = SearchParams(
        site = site,
        category = categoryByValue(lastCategoryValue(site).ifBlank { defaultCategoryValue(site) }, site),
        filter = filterByValue(lastFilterValue(site)),
        sortField = sortFieldByValue(lastSortFieldValue(site).ifBlank { defaultSortFieldValue(site) }),
        sortOrder = sortOrderByValue(lastSortOrderValue(site).ifBlank { defaultSortOrderValue(site) })
    ).withValidCategory()

    fun persistFilters(params: SearchParams) {
        val site = params.site
        prefs.edit()
            .putString(siteKey(KEY_LAST_CATEGORY, site), params.category.value)
            .putInt(siteKey(KEY_LAST_FILTER, site), params.filter.value)
            .putString(siteKey(KEY_LAST_SORT_FIELD, site), params.sortField.value)
            .putString(siteKey(KEY_LAST_SORT_ORDER, site), params.sortOrder.value)
            .apply()
    }

    fun applyToSiteConfig() {
        CatalogSite.entries.forEach { site ->
            SiteConfig.setBaseUrl(site, baseUrl(site))
        }
        SiteConfig.currentSite = currentSite
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt.toHex())
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltHex = prefs.getString(KEY_PIN_SALT, "").orEmpty()
        val stored = prefs.getString(KEY_PIN_HASH, "").orEmpty()
        if (saltHex.isBlank() || stored.isBlank()) return false
        return hashPin(pin, saltHex.fromHex()) == stored
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_THEME_INDEX = "theme_index"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CURRENT_SITE = "current_site"
        private const val KEY_SUKEBEI_ACK = "sukebei_ack"
        private const val KEY_SUKEBEI_ENABLED = "sukebei_enabled"
        private const val KEY_ONBOARDING = "onboarding_complete"
        private const val KEY_DEFAULT_CATEGORY = "default_category"
        private const val KEY_DEFAULT_SORT_FIELD = "default_sort_field"
        private const val KEY_DEFAULT_SORT_ORDER = "default_sort_order"
        private const val KEY_LAST_CATEGORY = "last_category"
        private const val KEY_LAST_FILTER = "last_filter"
        private const val KEY_LAST_SORT_FIELD = "last_sort_field"
        private const val KEY_LAST_SORT_ORDER = "last_sort_order"
        private const val KEY_TORRENT_PACKAGE = "torrent_package"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_HIDE_SCREENSHOTS = "hide_screenshots"
        private const val KEY_HIDE_RECENTS = "hide_recents"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_UPDATE_CHECK_AT = "update_check_at"
        private const val KEY_DISMISSED_UPDATE = "dismissed_update"
        private const val KEY_ROOM_MIGRATED = "room_migrated"

        private fun baseUrlKey(site: CatalogSite) = "base_url_${site.id}"

        private fun siteKey(base: String, site: CatalogSite) = "${base}_${site.id}"

        fun hashPin(pin: String, salt: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            digest.update(pin.toByteArray(Charsets.UTF_8))
            return digest.digest().toHex()
        }
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun String.fromHex(): ByteArray {
    if (length % 2 != 0) return ByteArray(0)
    return ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
