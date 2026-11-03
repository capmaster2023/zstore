package com.graceofgod.zstore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val email: String,
    val name: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

@Serializable
data class AuthResponse(val token: String, val user: User)

@Serializable
data class MeResponse(val user: User)

@Serializable
data class LoginBody(val email: String, val password: String)

@Serializable
data class RegisterBody(val email: String, val password: String, val name: String?)

@Serializable
data class StoreApp(
    val id: Int,
    @SerialName("package_name") val packageName: String,
    val name: String,
    @SerialName("short_desc") val shortDesc: String? = null,
    val description: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("price_cents") val priceCents: Int = 0,
    val currency: String = "cad",
    val published: Boolean = true,
    val owned: Boolean = false,
    @SerialName("version_code") val versionCode: Long? = null,
    @SerialName("version_name") val versionName: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("min_sdk") val minSdk: Int? = null,
    val downloads: Int = 0,
    @SerialName("source_type") val sourceType: String? = null,
    @SerialName("github_repo") val githubRepo: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val isFree: Boolean get() = priceCents == 0
    val hasVersion: Boolean get() = versionCode != null
}

@Serializable
data class AppVersion(
    val id: Int,
    @SerialName("version_code") val versionCode: Long,
    @SerialName("version_name") val versionName: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val sha256: String? = null,
    @SerialName("min_sdk") val minSdk: Int? = null,
    val changelog: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AppsResponse(val apps: List<StoreApp>)

@Serializable
data class AppDetailResponse(val app: StoreApp, val versions: List<AppVersion> = emptyList())

@Serializable
data class CheckoutResponse(val owned: Boolean = false, val url: String? = null)

@Serializable
data class ConfirmResponse(val ok: Boolean = false, @SerialName("app_id") val appId: Int = 0)

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class AppUpdateBody(
    val name: String,
    @SerialName("short_desc") val shortDesc: String,
    val description: String,
    @SerialName("price_cents") val priceCents: Int,
    val published: Boolean,
)

@Serializable
data class Build(
    val id: Int,
    @SerialName("app_id") val appId: Int? = null,
    @SerialName("app_name") val appName: String? = null,
    val repo: String,
    @SerialName("repo_url") val repoUrl: String? = null,
    val status: String,
    @SerialName("commit_sha") val commitSha: String? = null,
    @SerialName("run_url") val runUrl: String? = null,
    @SerialName("error_log") val errorLog: String? = null,
    @SerialName("version_code") val versionCode: Long? = null,
    @SerialName("version_name") val versionName: String? = null,
    val changelog: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val isActive: Boolean get() = status == "queued" || status == "running"
}

@Serializable
data class BuildResponse(val build: Build)

@Serializable
data class BuildsResponse(val builds: List<Build>)

@Serializable
data class Sale(
    val id: Int,
    @SerialName("app_id") val appId: Int,
    @SerialName("app_name") val appName: String,
    val email: String,
    @SerialName("amount_cents") val amountCents: Int,
    val currency: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class SaleTotal(val currency: String, val count: Int, @SerialName("amount_cents") val amountCents: Int)

@Serializable
data class SalesResponse(
    val sales: List<Sale> = emptyList(),
    val totals: List<SaleTotal> = emptyList(),
    val users: Int = 0,
    val downloads: Int = 0,
)

@Serializable
data class ApiError(val error: String)
