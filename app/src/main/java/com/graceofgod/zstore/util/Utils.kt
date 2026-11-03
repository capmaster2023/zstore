package com.graceofgod.zstore.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.drawable.toBitmap
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class ApkInfo(
    val packageName: String,
    val versionCode: Long,
    val versionName: String?,
    val label: String,
    val minSdk: Int?,
    val iconPng: ByteArray?,
)

object Pkg {
    private fun info(pm: PackageManager, pkg: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        else pm.getPackageInfo(pkg, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun versionCode(p: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong()

    /** versionCode installé, ou null si l'app n'est pas installée */
    fun installedVersion(context: Context, pkg: String): Long? = info(context.packageManager, pkg)?.let { versionCode(it) }

    fun installedMap(context: Context, pkgs: Collection<String>): Map<String, Long> =
        pkgs.mapNotNull { p -> installedVersion(context, p)?.let { p to it } }.toMap()

    fun open(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun uninstall(context: Context, pkg: String) {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Lit le package, la version, le nom et l'icône d'un fichier APK. */
    fun parseApk(context: Context, file: File): ApkInfo? {
        val pm = context.packageManager
        val p: PackageInfo = (if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageArchiveInfo(file.path, 0)
        }) ?: return null
        val ai = p.applicationInfo ?: return null
        ai.sourceDir = file.path
        ai.publicSourceDir = file.path
        val label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(p.packageName)
        val icon = runCatching {
            val bmp = ai.loadIcon(pm).toBitmap(256, 256, Bitmap.Config.ARGB_8888)
            ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray() }
        }.getOrNull()
        return ApkInfo(
            packageName = p.packageName,
            versionCode = versionCode(p),
            versionName = p.versionName,
            label = label,
            minSdk = ai.minSdkVersion,
            iconPng = icon,
        )
    }
}

fun formatPrice(cents: Int, currency: String = "cad"): String {
    if (cents == 0) return "Gratuit"
    val f = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH)
    runCatching { f.currency = Currency.getInstance(currency.uppercase()) }
    return f.format(cents / 100.0)
}

/** "4,99" ou "4.99" → 499 ; vide → 0 ; invalide → null */
fun parsePriceToCents(text: String): Int? {
    val t = text.trim().replace(',', '.').replace("$", "").trim()
    if (t.isEmpty()) return 0
    val v = t.toDoubleOrNull() ?: return null
    if (v < 0) return null
    return Math.round(v * 100).toInt()
}

fun centsToInput(cents: Int): String =
    if (cents == 0) "" else String.format(Locale.CANADA_FRENCH, "%.2f", cents / 100.0)

fun formatSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "—"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1) String.format(Locale.CANADA_FRENCH, "%.1f Mo", mb)
    else String.format(Locale.CANADA_FRENCH, "%.0f Ko", bytes / 1024.0)
}

fun openUrl(context: Context, url: String) {
    runCatching {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(url))
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Corps de requête qui rapporte la progression de l'envoi (0f..1f). */
class ProgressRequestBody(
    private val file: File,
    contentType: String,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    private val type: MediaType = contentType.toMediaType()
    override fun contentType(): MediaType = type
    override fun contentLength(): Long = file.length()
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1)
        var sent = 0L
        var lastReported = -1
        file.source().use { source ->
            while (true) {
                val read = source.read(sink.buffer, 64 * 1024)
                if (read == -1L) break
                sink.flush()
                sent += read
                val pct = (sent * 100 / total).toInt()
                if (pct != lastReported) {
                    lastReported = pct
                    onProgress(sent.toFloat() / total)
                }
            }
        }
    }
}
