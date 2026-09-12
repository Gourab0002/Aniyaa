package com.nyaa.aniyaa.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

object UpdateInstaller {
    private const val RELATIVE_DIR = "updates"
    private const val FILE_NAME = "Aniyaa-update.apk"

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    suspend fun download(context: Context, apkUrl: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, RELATIVE_DIR).apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        if (file.exists()) file.delete()
        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", AppHttpClient.USER_AGENT)
            .header("Accept", "application/octet-stream")
            .build()
        AppHttpClient.instance.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed (HTTP ${response.code})")
            }
            val body = response.body ?: throw IllegalStateException("Empty update file")
            file.outputStream().use { output ->
                body.byteStream().copyTo(output)
            }
        }
        if (file.length() < 1024L) {
            file.delete()
            throw IllegalStateException("Downloaded update was empty")
        }
        if (!signingMatchesInstalled(context, file)) {
            file.delete()
            throw IllegalStateException("Update signature does not match the installed app")
        }
        file
    }

    fun signingMatchesInstalled(context: Context, file: File): Boolean {
        val archiveSigs = packageSignatures(context, archivePath = file.absolutePath) ?: return false
        val installedSigs = packageSignatures(context, packageName = context.packageName) ?: return false
        return archiveSigs.any { it in installedSigs }
    }

    private fun packageSignatures(
        context: Context,
        archivePath: String? = null,
        packageName: String? = null
    ): Set<String>? {
        val pm = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                val info = if (archivePath != null) {
                    pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNING_CERTIFICATES)
                } else {
                    pm.getPackageInfo(packageName!!, PackageManager.GET_SIGNING_CERTIFICATES)
                }
                val signers = info?.signingInfo?.apkContentsSigners ?: info?.signingInfo?.signingCertificateHistory
                signers?.map { it.toCharsString() }?.toSet()
            } else {
                @Suppress("DEPRECATION")
                val info = if (archivePath != null) {
                    pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNATURES)
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
                }
                @Suppress("DEPRECATION")
                info?.signatures?.map { it.toCharsString() }?.toSet()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun install(context: Context, file: File): String? {
        if (!file.exists()) return "Update file missing"
        if (!signingMatchesInstalled(context, file)) {
            file.delete()
            return "Update signature does not match the installed app"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallPackages(context)) {
            requestInstallPermission(context)
            return "Allow Aniyaa to install updates, then tap Install again"
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            null
        } catch (e: Exception) {
            e.message ?: "Could not open the installer"
        }
    }
}
