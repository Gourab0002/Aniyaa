package com.nyaa.aniyaa.data.update

import com.nyaa.aniyaa.BuildConfig
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.network.await
import org.json.JSONObject

data class AppUpdate(
    val versionName: String,
    val htmlUrl: String,
    val notes: String
)

object UpdateChecker {
    private const val LATEST_URL =
        "https://api.github.com/repos/Gourab0002/Aniyaa/releases/latest"

    suspend fun check(): Result<AppUpdate?> {
        return try {
            val request = AppHttpClient.newRequest(LATEST_URL).newBuilder()
                .header("Accept", "application/vnd.github+json")
                .build()
            AppHttpClient.instance.newCall(request).await().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Could not check for updates (HTTP ${response.code})"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val tag = json.optString("tag_name").removePrefix("v")
                if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) {
                    return Result.success(null)
                }
                Result.success(
                    AppUpdate(
                        versionName = tag,
                        htmlUrl = json.optString("html_url")
                            .ifBlank { "https://github.com/Gourab0002/Aniyaa/releases/latest" },
                        notes = json.optString("body")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.removePrefix("v").split('.', '-', '_')
            .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val localParts = local.removePrefix("v").split('.', '-', '_')
            .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val max = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until max) {
            val r = remoteParts.getOrNull(i) ?: 0
            val l = localParts.getOrNull(i) ?: 0
            if (r != l) return r > l
        }
        return false
    }
}
