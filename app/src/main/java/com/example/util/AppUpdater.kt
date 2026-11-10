package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Describes a newer APK published on Firebase Hosting. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
    val mandatory: Boolean
)

/**
 * Self-update helper for the sideloaded (non-Play-Store) shop app.
 *
 * The build publishes `version.json` next to the APK on Firebase Hosting:
 * `{ versionCode, versionName, apkUrl, notes, mandatory }`. Each device compares
 * its own installed versionCode with that file, downloads the APK and hands it
 * to the system installer. Fully silent installs are not possible for a normal
 * APK, so the user taps "ติดตั้ง" once.
 */
/** Result of handing the downloaded APK to the system. */
enum class InstallResult { INSTALLER_OPENED, NEED_PERMISSION, BROWSER_FALLBACK }

object AppUpdater {
    private const val VERSION_URL = "https://dinoeater-merchant.web.app/version.json"

    fun installedVersionCode(context: Context): Int = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (info.longVersionCode and 0x7FFFFFFFL).toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    } catch (e: Exception) {
        0
    }

    fun installedVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-"
    } catch (e: Exception) {
        "-"
    }

    /** Returns a non-null [UpdateInfo] only when a newer build is published. */
    suspend fun check(context: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val json = httpGetText(VERSION_URL)
            val o = JSONObject(json)
            val info = UpdateInfo(
                versionCode = o.optInt("versionCode", 0),
                versionName = o.optString("versionName", ""),
                apkUrl = o.optString("apkUrl", ""),
                notes = o.optString("notes", ""),
                mandatory = o.optBoolean("mandatory", false)
            )
            val current = installedVersionCode(context)
            if (info.versionCode > current && info.apkUrl.isNotBlank()) {
                Result.success(info)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun httpGetText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads the APK into the app cache and returns the file. */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "update.apk")
        if (out.exists()) out.delete()

        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val total = conn.contentLength.toLong()
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } > 0) {
                        output.write(buf, 0, read)
                        done += read
                        if (total > 0) {
                            onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                    output.flush()
                }
            }
        } finally {
            conn.disconnect()
        }
        if (out.length() < 1_000_000L) error("ไฟล์ APK ไม่สมบูรณ์")
        out
    }

    /**
     * Opens the system installer. Returns NEED_PERMISSION when the user still has
     * to grant the "install unknown apps" permission (Android opens that page).
     */
    fun install(context: Context, apk: File, fallbackUrl: String = ""): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                // Some devices do not expose this screen; the install intent below
                // will still prompt the user.
            }
            return InstallResult.NEED_PERMISSION
        }
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updatefileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallResult.INSTALLER_OPENED
        } catch (e: Exception) {
            // Fall back to the browser download page (e.g. devices without a
            // package-installer activity, such as some emulators).
            if (fallbackUrl.isNotBlank()) {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    return InstallResult.BROWSER_FALLBACK
                } catch (ignored: Exception) {
                }
            }
            InstallResult.BROWSER_FALLBACK
        }
    }
}
