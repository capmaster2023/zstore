package com.graceofgod.zstore.data

import android.content.Context
import android.net.Uri
import com.graceofgod.zstore.util.ApkInfo
import com.graceofgod.zstore.util.ProgressRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class Repository(
    private val context: Context,
    val api: ZStoreApi,
    val session: SessionStore,
) {
    private val _catalog = MutableStateFlow<List<StoreApp>>(emptyList())
    val catalog: StateFlow<List<StoreApp>> = _catalog.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun toast(msg: String) {
        _messages.tryEmit(msg)
    }

    suspend fun refreshCatalog(): Result<List<StoreApp>> {
        _loading.value = true
        val r = apiCall { api.apps().apps }
        r.onSuccess { _catalog.value = it }
        _loading.value = false
        return r
    }

    fun cached(id: Int): StoreApp? = _catalog.value.firstOrNull { it.id == id }

    // ---- Compte ----

    suspend fun login(email: String, password: String): Result<User> =
        apiCall { api.login(LoginBody(email.trim(), password)) }.map { session.save(it); it.user }
            .also { if (it.isSuccess) refreshCatalog() }

    suspend fun register(email: String, password: String, name: String?): Result<User> =
        apiCall { api.register(RegisterBody(email.trim(), password, name?.takeIf { it.isNotBlank() })) }
            .map { session.save(it); it.user }
            .also { if (it.isSuccess) refreshCatalog() }

    suspend fun refreshMe() {
        if (session.token == null) return
        apiCall { api.me() }.onSuccess { session.updateUser(it.user) }
    }

    suspend fun logout() {
        apiCall { api.logout() }
        session.clear()
        refreshCatalog()
    }

    // ---- Achats ----

    suspend fun checkout(appId: Int): Result<CheckoutResponse> = apiCall { api.checkout(appId) }

    suspend fun confirmPurchase(sessionId: String): Result<ConfirmResponse> =
        apiCall { api.confirmPurchase(sessionId) }.also { if (it.isSuccess) refreshCatalog() }

    // ---- Admin ----

    suspend fun uploadApk(
        apk: File,
        info: ApkInfo,
        appId: Int?,
        name: String,
        shortDesc: String,
        description: String,
        priceCents: Int,
        published: Boolean,
        changelog: String,
        onProgress: (Float) -> Unit,
    ): Result<AppDetailResponse> {
        val apkPart = MultipartBody.Part.createFormData(
            "apk", "${info.packageName}.apk",
            ProgressRequestBody(apk, "application/vnd.android.package-archive", onProgress),
        )
        val iconPart = info.iconPng?.let {
            MultipartBody.Part.createFormData("icon", "icon.png", it.toRequestBody("image/png".toMediaType()))
        }
        val fields = buildMap {
            put("package_name", info.packageName)
            put("version_code", info.versionCode.toString())
            put("version_name", info.versionName ?: "")
            info.minSdk?.let { put("min_sdk", it.toString()) }
            appId?.let { put("app_id", it.toString()) }
            put("name", name)
            put("short_desc", shortDesc)
            put("description", description)
            put("price_cents", priceCents.toString())
            put("published", if (published) "1" else "0")
            put("changelog", changelog)
        }.mapValues { it.value.toPlain() }
        return apiCall { api.uploadApk(apkPart, iconPart, fields) }.also { if (it.isSuccess) refreshCatalog() }
    }

    suspend fun updateApp(id: Int, body: AppUpdateBody): Result<AppDetailResponse> =
        apiCall { api.updateApp(id, body) }.also { if (it.isSuccess) refreshCatalog() }

    suspend fun deleteApp(id: Int): Result<OkResponse> =
        apiCall { api.deleteApp(id) }.also { if (it.isSuccess) refreshCatalog() }

    suspend fun createBuild(
        zip: File,
        repo: String,
        appId: Int?,
        changelog: String,
        private: Boolean,
        onProgress: (Float) -> Unit,
    ): Result<Build> {
        val part = MultipartBody.Part.createFormData("source", "source.zip", ProgressRequestBody(zip, "application/zip", onProgress))
        val fields = buildMap {
            put("repo", repo)
            appId?.let { put("app_id", it.toString()) }
            put("changelog", changelog)
            put("private", if (private) "1" else "0")
        }.mapValues { it.value.toPlain() }
        return apiCall { api.createBuild(part, fields).build }
    }

    suspend fun builds() = apiCall { api.builds().builds }
    suspend fun build(id: Int) = apiCall { api.build(id).build }
    suspend fun retryBuild(id: Int) = apiCall { api.retryBuild(id).build }
    suspend fun sales() = apiCall { api.sales() }
    suspend fun appDetail(id: Int) = apiCall { api.app(id) }

    suspend fun aiModels() = apiCall { api.aiModels() }
    suspend fun aiJobs() = apiCall { api.aiJobs().jobs }
    suspend fun aiJob(id: Int) = apiCall { api.aiJob(id).job }
    suspend fun createAiJob(body: AiJobBody) = apiCall { api.createAiJob(body).job }

    /** Copie un fichier choisi par l'utilisateur dans le cache de l'app. */
    suspend fun copyToCache(uri: Uri, name: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "picked").apply { mkdirs() }
        val out = File(dir, name)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Impossible de lire le fichier" }
            out.outputStream().use { input.copyTo(it) }
        }
        out
    }
}

private fun String.toPlain(): RequestBody = toRequestBody("text/plain; charset=utf-8".toMediaType())
