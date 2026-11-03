package com.graceofgod.zstore.data

import com.graceofgod.zstore.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

interface ZStoreApi {
    @POST("auth/login") suspend fun login(@Body body: LoginBody): AuthResponse
    @POST("auth/register") suspend fun register(@Body body: RegisterBody): AuthResponse
    @POST("auth/logout") suspend fun logout(): OkResponse
    @GET("me") suspend fun me(): MeResponse

    @GET("apps") suspend fun apps(): AppsResponse
    @GET("apps/{id}") suspend fun app(@Path("id") id: Int): AppDetailResponse
    @POST("apps/{id}/checkout") suspend fun checkout(@Path("id") id: Int): CheckoutResponse
    @GET("purchase/confirm") suspend fun confirmPurchase(@Query("session_id") sessionId: String): ConfirmResponse

    @Multipart
    @POST("admin/apps/upload")
    suspend fun uploadApk(
        @Part apk: MultipartBody.Part,
        @Part icon: MultipartBody.Part?,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    ): AppDetailResponse

    @POST("admin/apps/{id}")
    suspend fun updateApp(@Path("id") id: Int, @Body body: AppUpdateBody): AppDetailResponse

    @POST("admin/apps/{id}/delete")
    suspend fun deleteApp(@Path("id") id: Int): OkResponse

    @Multipart
    @POST("admin/builds")
    suspend fun createBuild(
        @Part source: MultipartBody.Part,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    ): BuildResponse

    @GET("admin/builds") suspend fun builds(): BuildsResponse
    @GET("admin/builds/{id}") suspend fun build(@Path("id") id: Int): BuildResponse
    @POST("admin/builds/{id}/retry") suspend fun retryBuild(@Path("id") id: Int): BuildResponse
    @GET("admin/sales") suspend fun sales(): SalesResponse

    @GET("admin/ai/models") suspend fun aiModels(): AiModelsResponse
    @GET("admin/ai/jobs") suspend fun aiJobs(): AiJobsResponse
    @GET("admin/ai/jobs/{id}") suspend fun aiJob(@Path("id") id: Int): AiJobResponse
    @POST("admin/ai/jobs") suspend fun createAiJob(@Body body: AiJobBody): AiJobResponse
}

val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

object Net {
    val baseUrl: String = BuildConfig.API_BASE

    fun client(session: SessionStore): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)   // l'envoi vers GitHub peut prendre du temps
        .writeTimeout(10, TimeUnit.MINUTES)  // gros APK / zips
        .addInterceptor { chain ->
            val token = session.token
            val req = if (token != null) {
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            } else chain.request()
            val res = chain.proceed(req)
            if (res.code == 401 && token != null) session.clear()
            res
        }
        .build()

    fun api(client: OkHttpClient): ZStoreApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ZStoreApi::class.java)
}

/** Exécute un appel API et transforme les erreurs en message lisible. */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val raw = e.response()?.errorBody()?.string()
    val msg = runCatching { json.decodeFromString(ApiError.serializer(), raw ?: "").error }.getOrNull()
    Result.failure(Exception(msg ?: "Erreur serveur (${e.code()})"))
} catch (e: IOException) {
    Result.failure(Exception("Impossible de joindre le serveur Z-Store. Vérifie ta connexion."))
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(Exception(e.message ?: "Erreur inattendue"))
}
