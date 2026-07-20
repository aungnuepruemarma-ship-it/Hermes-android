package net.nous.hermes.net

import com.google.gson.GsonBuilder
import net.nous.hermes.net.models.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * REST client for the existing `hermes dashboard` FastAPI backend.
 *
 * Base URL MUST be http://127.0.0.1:<port> (loopback). The backend refuses
 * non-loopback binds unless launched with --insecure, and engages an OAuth
 * gate in that case — loopback + session token is the intended Android path.
 *
 * Every endpoint below was verified to exist in hermes-agent 0.16.0
 * (hermes_cli/web_server.py). Field names use the backend's JSON keys via
 * @SerializedName in the model classes.
 */
interface HermesApi {

    // ---- Status / system (📊) ----
    @GET("/api/status")
    suspend fun getStatus(): Status

    @GET("/api/system/stats")
    suspend fun getSystemStats(): SystemStats

    // ---- Sessions / chat (💬) ----
    @GET("/api/sessions")
    suspend fun listSessions(): SessionList

    @GET("/api/sessions/{id}/messages")
    suspend fun getSessionMessages(@Path("id") id: String): List<SessionMessage>

    @DELETE("/api/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String)

    // ---- Tools / skills (🔌) ----
    @GET("/api/tools/toolsets")
    suspend fun getToolsets(): List<Toolset>

    @PUT("/api/tools/toolsets/{name}")
    suspend fun setToolsetEnabled(
        @Path("name") name: String,
        @Body body: ToggleRequest,
    )

    @GET("/api/skills")
    suspend fun getSkills(): List<Skill>

    @PUT("/api/skills/toggle")
    suspend fun toggleSkill(@Body body: SkillToggleRequest)

    // ---- Cron / task queue (📝) ----
    @GET("/api/cron/jobs")
    suspend fun getCronJobs(): List<CronJob>

    @POST("/api/cron/jobs/{id}/trigger")
    suspend fun triggerCronJob(@Path("id") id: String)

    @POST("/api/cron/jobs/{id}/pause")
    suspend fun pauseCronJob(@Path("id") id: String)

    @POST("/api/cron/jobs/{id}/resume")
    suspend fun resumeCronJob(@Path("id") id: String)

    // ---- Memory (🧠) ----
    @GET("/api/memory")
    suspend fun getMemory(): MemoryView

    /** POST /api/memory/reset — body is an empty object; returns
     *  { ok:true, deleted:[...] }. */
    @POST("/api/memory/reset")
    suspend fun resetMemory(@Body body: Map<String, String> = emptyMap())

    // ---- Config (📂) ----
    @GET("/api/config/raw")
    suspend fun getRawConfig(): RawConfig

    @PUT("/api/config/raw")
    suspend fun putRawConfig(@Body body: RawConfigPut)

    /** GET /api/logs — live tail of Hermes's own activity log. */
    @GET("/api/logs")
    suspend fun getLogs(): LogView

    // ---- Process control (▶️) ----
    @POST("/api/gateway/start")
    suspend fun gatewayStart()

    @POST("/api/gateway/stop")
    suspend fun gatewayStop()

    @POST("/api/gateway/restart")
    suspend fun gatewayRestart()
}

object HermesClient {
    fun create(
        baseUrl: String,        // e.g. "http://127.0.0.1:9119"
        sessionToken: String,
        hostHeader: String,     // e.g. "127.0.0.1:9119"
    ): HermesApi {
        val ok = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionToken, hostHeader))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HermesApi::class.java)
    }
}
