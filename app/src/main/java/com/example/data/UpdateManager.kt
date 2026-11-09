package com.example.data

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

/**
 * Gerencia verificação e download de atualizações via GitHub Releases.
 * Repo: Sjos0/Pulchra-Domina-Game
 */
class UpdateManager(private val context: Context) {

    data class ReleaseInfo(
        val tagName: String,
        val versionName: String,
        val downloadUrl: String?,
        val body: String?,
        val publishedAt: String?
    )

    data class CheckResult(
        val status: Status,
        val release: ReleaseInfo? = null,
        val message: String
    )

    enum class Status {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        NO_RELEASES,
        ERROR
    }

    companion object {
        private const val REPO_OWNER = "Sjos0"
        private const val REPO_NAME = "Pulchra-Domina-Game"
        private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        private const val CURRENT_VERSION = "1.0.7"
    }

    fun getCurrentVersion(): String = CURRENT_VERSION

    /**
     * Consulta o último release no GitHub.
     */
    suspend fun checkForUpdate(): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "PulchraDomina-Updater")
                connectTimeout = 12000
                readTimeout = 12000
            }

            val code = conn.responseCode
            if (code == 404) {
                return@withContext CheckResult(
                    Status.NO_RELEASES,
                    message = "Nenhum release publicado ainda no GitHub. Você está na versão $CURRENT_VERSION."
                )
            }
            if (code != 200) {
                return@withContext CheckResult(
                    Status.ERROR,
                    message = "Falha ao consultar GitHub (HTTP $code)."
                )
            }

            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val name = json.optString("name", tag)
            val notes = json.optString("body", null)
            val published = json.optString("published_at", null)

            // Procura asset .apk
            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            val remoteVersion = tag.removePrefix("v").trim()
            val isNewer = isVersionNewer(remoteVersion, CURRENT_VERSION)

            if (!isNewer) {
                CheckResult(
                    Status.UP_TO_DATE,
                    ReleaseInfo(tag, name, apkUrl, notes, published),
                    "Você já está na versão mais recente ($CURRENT_VERSION)."
                )
            } else {
                CheckResult(
                    Status.UPDATE_AVAILABLE,
                    ReleaseInfo(tag, name, apkUrl, notes, published),
                    "Nova versão disponível: $tag"
                )
            }
        } catch (e: Exception) {
            CheckResult(Status.ERROR, message = "Erro de rede: ${e.message ?: "desconhecido"}")
        }
    }

    /**
     * Baixa o APK para o cache do app.
     * Retorna o File baixado ou null em caso de falha.
     */
    suspend fun downloadApk(downloadUrl: String, onProgress: (Int) -> Unit = {}): File? =
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "PulchraDomina-Updater")
                    connectTimeout = 20000
                    readTimeout = 60000
                }
                if (conn.responseCode != 200) return@withContext null

                val total = conn.contentLengthLong.coerceAtLeast(1L)
                val outFile = File(context.cacheDir, "pulchra_update.apk")
                if (outFile.exists()) outFile.delete()

                conn.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
                outFile
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Dispara a instalação do APK baixado.
     * Retorna true se o Intent foi disparado.
     */
    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() < 1000L) return false
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            // Prefer PackageInstaller-friendly VIEW intent; keep file until user finishes
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            // Grant explicit read permission to package installer packages
            context.grantUriPermission(
                "com.android.packageinstaller",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            try {
                context.grantUriPermission(
                    "com.google.android.packageinstaller",
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /** Apaga o APK do cache após a instalação (ou em falha). */
    fun cleanupApk() {
        try {
            File(context.cacheDir, "pulchra_update.apk").delete()
        } catch (_: Exception) { }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        fun parse(v: String) = v.split(".", "-").mapNotNull { it.toIntOrNull() }
        val r = parse(remote)
        val c = parse(current)
        val max = maxOf(r.size, c.size)
        for (i in 0 until max) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }
}
