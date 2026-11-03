package com.graceofgod.zstore.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.graceofgod.zstore.data.Net
import com.graceofgod.zstore.data.StoreApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

sealed interface InstallState {
    data class Downloading(val progress: Float) : InstallState
    data object Installing : InstallState
    data object WaitingUser : InstallState
    data object Done : InstallState
    data class Failed(val message: String) : InstallState
}

/**
 * Télécharge les APK depuis le serveur Z-Store et les installe avec PackageInstaller.
 * Les mises à jour des apps installées par Z-Store se font sans confirmation sur Android 12+.
 */
class InstallManager(
    private val context: Context,
    private val http: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val jobs = mutableMapOf<String, Job>()

    private val _states = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    val states: StateFlow<Map<String, InstallState>> = _states.asStateFlow()

    /** Incrémenté après chaque installation/désinstallation pour rafraîchir l'état « installé ». */
    private val _installedTick = MutableStateFlow(0)
    val installedTick: StateFlow<Int> = _installedTick.asStateFlow()

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun set(pkg: String, state: InstallState?) {
        _states.update { if (state == null) it - pkg else it + (pkg to state) }
        if (state == InstallState.Done) _installedTick.update { it + 1 }
    }

    fun refreshInstalled() = _installedTick.update { it + 1 }

    fun isBusy(pkg: String): Boolean = when (_states.value[pkg]) {
        is InstallState.Downloading, InstallState.Installing, InstallState.WaitingUser -> true
        else -> false
    }

    fun install(app: StoreApp) {
        if (isBusy(app.packageName)) return
        jobs[app.packageName]?.cancel()
        jobs[app.packageName] = scope.launch {
            try {
                set(app.packageName, InstallState.Downloading(0f))
                val file = download(app) { p -> set(app.packageName, InstallState.Downloading(p)) }
                set(app.packageName, InstallState.Installing)
                commitSession(file, app.packageName)
            } catch (e: kotlinx.coroutines.CancellationException) {
                set(app.packageName, null)
            } catch (e: Exception) {
                set(app.packageName, InstallState.Failed(e.message ?: "Échec de l'installation"))
            }
        }
    }

    fun cancel(pkg: String) {
        jobs.remove(pkg)?.cancel()
        set(pkg, null)
    }

    private suspend fun download(app: StoreApp, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 3600_000L }?.forEach { it.delete() }
        val out = File(dir, "${app.packageName}.apk")
        val req = Request.Builder().url(Net.baseUrl + "apps/${app.id}/download").build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                val err = res.body?.string()?.let { Regex("\"error\"\\s*:\\s*\"([^\"]*)\"").find(it)?.groupValues?.get(1) }
                throw Exception(err ?: "Téléchargement impossible (${res.code})")
            }
            val body = res.body ?: throw Exception("Réponse vide du serveur")
            val total = body.contentLength().takeIf { it > 0 } ?: app.sizeBytes ?: -1L
            val expectedSha = res.header("X-Apk-Sha256")
            val digest = MessageDigest.getInstance("SHA-256")
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var last = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        ensureActive()
                        output.write(buf, 0, read)
                        digest.update(buf, 0, read)
                        done += read
                        if (total > 0 && done - last > 128 * 1024) {
                            last = done
                            withContext(Dispatchers.Main) { onProgress(done.toFloat() / total) }
                        }
                    }
                }
            }
            if (expectedSha != null) {
                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                if (!sha.equals(expectedSha, ignoreCase = true)) {
                    out.delete()
                    throw Exception("Fichier corrompu pendant le téléchargement, réessaie")
                }
            }
        }
        out
    }

    private suspend fun commitSession(file: File, pkg: String) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(pkg)
            setSize(file.length())
            if (Build.VERSION.SDK_INT >= 31) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, InstallResultReceiver::class.java)
                .setPackage(context.packageName)
                .putExtra(InstallResultReceiver.EXTRA_PKG, pkg)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }
}
