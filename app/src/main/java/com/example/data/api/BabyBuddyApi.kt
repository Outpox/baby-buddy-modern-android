package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class BabyBuddyChildDto(
    val id: Int,
    @param:Json(name = "first_name") val firstName: String,
    @param:Json(name = "last_name") val lastName: String,
    @param:Json(name = "birth_date") val birthDate: String?,
    val slug: String?
)

data class BabyBuddyFeedingDto(
    val id: Long? = null,
    val child: Int,
    val start: String,
    val end: String? = null,
    val duration: String? = null, // read-only, "HH:MM:SS" (or "D HH:MM:SS")
    val type: String, // "breast milk", "formula", "fortified breast milk", "solid food"
    val method: String, // "bottle", "left breast", "right breast", "both breasts", "parent fed", "self fed"
    val amount: Double? = null,
    val notes: String? = null
)

data class BabyBuddySleepDto(
    val id: Long? = null,
    val child: Int,
    val start: String,
    val end: String? = null,
    val duration: String? = null,
    val nap: Boolean? = null,
    val notes: String? = null
)

data class BabyBuddyChangeDto(
    val id: Long? = null,
    val child: Int,
    val time: String,
    val wet: Boolean = false,
    val solid: Boolean = false,
    val color: String? = null,
    val amount: Double? = null,
    val notes: String? = null
)

data class BabyBuddyTummyTimeDto(
    val id: Long? = null,
    val child: Int,
    val start: String,
    val end: String? = null,
    val duration: String? = null,
    val milestone: String? = null,
    val notes: String? = null
)

data class BabyBuddyTemperatureDto(
    val id: Long? = null,
    val child: Int,
    val temperature: Double? = null,
    val time: String? = null,
    val notes: String? = null
)

/** A row from api/weight/, api/height/ or api/head-circumference/; only that endpoint's value field is set. */
data class BabyBuddyGrowthDto(
    val id: Long? = null,
    val child: Int,
    val weight: Double? = null,
    val height: Double? = null,
    @Json(name = "head_circumference") val headCircumference: Double? = null,
    val date: String? = null,
    val notes: String? = null
)

data class BabyBuddyNoteDto(
    val id: Long? = null,
    val child: Int,
    val note: String? = null,
    val time: String? = null,
    val tags: List<String>? = null
)

/** A Baby Buddy timer; the app keeps a running feed's or sleep's live state in its name (see SharedTimers). */
data class BabyBuddyTimerDto(
    val id: Long? = null,
    val child: Int? = null,
    val name: String? = null,
    val start: String? = null
)

data class BabyBuddyTagDto(
    val slug: String? = null,
    val name: String = "",
    val color: String? = null,
    @param:Json(name = "last_used") val lastUsed: String? = null
)

data class BabyBuddyPumpingDto(
    val id: Long? = null,
    val child: Int,
    val amount: Double? = null,
    val start: String? = null,
    val end: String? = null,
    val duration: String? = null,
    val notes: String? = null
)

data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

interface BabyBuddyApiService {
    // Generic writes used by the offline queue; endpoint is "feedings", "changes" or "tummy-times"
    @POST("api/{endpoint}/")
    suspend fun createRecord(
        @Header("Authorization") authHeader: String,
        @Path("endpoint") endpoint: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, Any?>>

    @PATCH("api/{endpoint}/{id}/")
    suspend fun updateRecord(
        @Header("Authorization") authHeader: String,
        @Path("endpoint") endpoint: String,
        @Path("id") id: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, Any?>>

    @DELETE("api/{endpoint}/{id}/")
    suspend fun deleteRecord(
        @Header("Authorization") authHeader: String,
        @Path("endpoint") endpoint: String,
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/children/")
    suspend fun getChildren(
        @Header("Authorization") authHeader: String
    ): Response<PaginatedResponse<BabyBuddyChildDto>>

    @GET("api/feedings/")
    suspend fun getFeedings(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("start_min") since: String? = null,
        @Query("start_max") until: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddyFeedingDto>>

    @POST("api/feedings/")
    suspend fun postFeeding(
        @Header("Authorization") authHeader: String,
        @Body feeding: BabyBuddyFeedingDto
    ): Response<BabyBuddyFeedingDto>

    @PATCH("api/feedings/{id}/")
    suspend fun patchFeeding(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Long,
        @Body fields: Map<String, @JvmSuppressWildcards Any>
    ): Response<BabyBuddyFeedingDto>

    @DELETE("api/feedings/{id}/")
    suspend fun deleteFeeding(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/sleep/")
    suspend fun getSleep(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("start_min") since: String? = null,
        @Query("start_max") until: String? = null,
        @Query("end_min") endedSince: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddySleepDto>>

    @POST("api/sleep/")
    suspend fun postSleep(
        @Header("Authorization") authHeader: String,
        @Body sleep: BabyBuddySleepDto
    ): Response<BabyBuddySleepDto>

    @GET("api/changes/")
    suspend fun getChanges(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("date_min") since: String? = null,
        @Query("date_max") until: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddyChangeDto>>

    @POST("api/tummy-times/")
    suspend fun postTummyTime(
        @Header("Authorization") authHeader: String,
        @Body tummyTime: BabyBuddyTummyTimeDto
    ): Response<BabyBuddyTummyTimeDto>

    @GET("api/temperature/")
    suspend fun getTemperatures(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("date_min") since: String? = null,
        @Query("date_max") until: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddyTemperatureDto>>

    /** [endpoint]: "weight", "height" or "head-circumference" (newest first). */
    @GET("api/{endpoint}/")
    suspend fun getGrowth(
        @Header("Authorization") authHeader: String,
        @Path("endpoint") endpoint: String,
        @Query("child") child: Int? = null,
        @Query("date") date: String? = null,
        @Query("limit") limit: Int = 200
    ): Response<PaginatedResponse<BabyBuddyGrowthDto>>

    @GET("api/timers/")
    suspend fun getTimers(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("limit") limit: Int = 50
    ): Response<PaginatedResponse<BabyBuddyTimerDto>>

    @GET("api/tags/")
    suspend fun getTags(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddyTagDto>>

    @GET("api/notes/")
    suspend fun getNotes(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("date_min") since: String? = null,
        @Query("date_max") until: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddyNoteDto>>

    @GET("api/pumping/")
    suspend fun getPumping(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("start_min") since: String? = null,
        @Query("start_max") until: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddyPumpingDto>>

    @GET("api/tummy-times/")
    suspend fun getTummyTimes(
        @Header("Authorization") authHeader: String,
        @Query("child") child: Int? = null,
        @Query("start_min") since: String? = null,
        @Query("start_max") until: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PaginatedResponse<BabyBuddyTummyTimeDto>>

    @POST("api/changes/")
    suspend fun postChange(
        @Header("Authorization") authHeader: String,
        @Body change: BabyBuddyChangeDto
    ): Response<BabyBuddyChangeDto>

    @PATCH("api/changes/{id}/")
    suspend fun patchChange(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Long,
        @Body fields: Map<String, @JvmSuppressWildcards Any>
    ): Response<BabyBuddyChangeDto>

    @DELETE("api/changes/{id}/")
    suspend fun deleteChange(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Long
    ): Response<Unit>
}

object BabyBuddyApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * [cfClientId]/[cfClientSecret] are a Cloudflare Access service token, sent on every request
     * so a server behind Cloudflare Access is reachable without the browser login.
     */
    fun createService(
        baseUrl: String,
        allowSelfSigned: Boolean = false,
        cfClientId: String = "",
        cfClientSecret: String = ""
    ): BabyBuddyApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)

        if (cfClientId.isNotBlank() && cfClientSecret.isNotBlank()) {
            builder.addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("CF-Access-Client-Id", cfClientId.trim())
                        .header("CF-Access-Client-Secret", cfClientSecret.trim())
                        .build()
                )
            }
        }

        builder
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )

        if (allowSelfSigned) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (_: Exception) {
                // fallback to default SSL
            }
        }

        val client = builder.build()

        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BabyBuddyApiService::class.java)
    }
}
