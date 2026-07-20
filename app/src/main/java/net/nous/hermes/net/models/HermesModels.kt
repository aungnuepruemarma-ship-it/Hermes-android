package net.nous.hermes.net.models

import com.google.gson.annotations.SerializedName

/* ------------------------------------------------------------------ *
 * DTOs mirror the JSON returned by the live `hermes dashboard` backend
 * (hermes-agent 0.16.0, hermes_cli/web_server.py). Verified against
 * GET /api/status and the endpoint list. Unknown fields are ignored
 * so the app keeps working if the backend adds fields.
 * ------------------------------------------------------------------ */

data class Status(
    @SerializedName("version") val version: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("hermes_home") val hermesHome: String? = null,
    @SerializedName("config_path") val configPath: String? = null,
    @SerializedName("env_path") val envPath: String? = null,
    @SerializedName("config_version") val configVersion: Int? = null,
    @SerializedName("latest_config_version") val latestConfigVersion: Int? = null,
    @SerializedName("gateway_running") val gatewayRunning: Boolean = false,
    @SerializedName("gateway_pid") val gatewayPid: Int? = null,
    @SerializedName("gateway_state") val gatewayState: String? = null,
    @SerializedName("gateway_platforms") val gatewayPlatforms: Map<String, Any>? = null,
    @SerializedName("active_sessions") val activeSessions: Int = 0,
    @SerializedName("auth_required") val authRequired: Boolean = false,
    @SerializedName("auth_providers") val authProviders: List<String>? = null,
)

/** GET /api/system/stats — CPU / RAM / token usage (📊).
 *  Verified shape (hermes-agent 0.16.0, Android host):
 *  { os, os_release, os_version, platform, arch, hostname,
 *    python_version, hermes_version, cpu_count,
 *    memory:{total,available,used,percent},
 *    disk:{total,used,free,percent}, psutil } */
data class SystemStats(
    @SerializedName("os") val os: String? = null,
    @SerializedName("platform") val platform: String? = null,
    @SerializedName("arch") val arch: String? = null,
    @SerializedName("hostname") val hostname: String? = null,
    @SerializedName("hermes_version") val hermesVersion: String? = null,
    @SerializedName("cpu_count") val cpuCount: Int? = null,
    @SerializedName("memory") val memory: MemoryInfo? = null,
    @SerializedName("disk") val disk: DiskInfo? = null,
    @SerializedName("psutil") val psutil: Boolean? = null,
)

data class MemoryInfo(
    @SerializedName("total_bytes") val totalBytes: Long? = null,
    @SerializedName("used_bytes") val usedBytes: Long? = null,
    @SerializedName("percent") val percent: Double? = null,
)

data class DiskInfo(
    @SerializedName("total_bytes") val totalBytes: Long? = null,
    @SerializedName("used_bytes") val usedBytes: Long? = null,
    @SerializedName("percent") val percent: Double? = null,
)

data class TokenUsage(
    @SerializedName("prompt_tokens") val promptTokens: Long? = null,
    @SerializedName("completion_tokens") val completionTokens: Long? = null,
    @SerializedName("total_tokens") val totalTokens: Long? = null,
    @SerializedName("cost_usd") val costUsd: Double? = null,
)

/** GET /api/sessions — list of sessions (💬).
 *  Verified shape: { sessions:[ { id, source, user_id, model, model_config,
 *    created_at?, updated_at?, title?, message_count? } ] } */
data class SessionList(
    @SerializedName("sessions") val sessions: List<SessionSummary> = emptyList(),
)

data class SessionSummary(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("message_count") val messageCount: Int? = null,
    @SerializedName("profile") val profile: String? = null,
)

/** GET /api/sessions/{id}/messages — full message history. */
data class SessionMessage(
    @SerializedName("role") val role: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<Any>? = null,
)

/** GET /api/tools/toolsets — tool list (🔌). */
data class Toolset(
    @SerializedName("name") val name: String = "",
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("description") val description: String? = null,
)

/** GET /api/skills — skills list. */
data class Skill(
    @SerializedName("name") val name: String = "",
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("description") val description: String? = null,
)

/** GET /api/cron/jobs — task queue + history (📝).
 *  Verified shape: { id, name, prompt, skills:[...], schedule?, enabled,
 *                    paused?, last_run?, next_run?, last_status? } */
data class CronJob(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("skills") val skills: List<String>? = null,
    @SerializedName("schedule") val schedule: String? = null,
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("paused") val paused: Boolean = false,
    @SerializedName("last_run") val lastRun: String? = null,
    @SerializedName("next_run") val nextRun: String? = null,
    @SerializedName("last_status") val lastStatus: String? = null,
)

/** GET /api/memory — memory store (🧠).
 *  Verified shape: { active:"agentmemory",
 *    providers:[ { name, description, configured } ] } */
data class MemoryView(
    @SerializedName("active") val active: String? = null,
    @SerializedName("providers") val providers: List<MemoryProvider>? = null,
)

data class MemoryProvider(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("configured") val configured: Boolean = false,
)

/** GET /api/config/raw — raw config YAML text (📂).
 *  Verified shape: { yaml:"model:\n  default: ..." } */
data class RawConfig(
    @SerializedName("yaml") val yaml: String? = null,
)

/** POST body for skill toggle. Verified: /api/skills/toggle requires
 *  { "name": "...", "enabled": bool } (not just enabled). */
data class SkillToggleRequest(
    @SerializedName("name") val name: String,
    @SerializedName("enabled") val enabled: Boolean,
)

/** POST body for tool/skill toggle. */
data class ToggleRequest(
    @SerializedName("enabled") val enabled: Boolean,
)

/** POST body for config raw PUT. Verified: field is "yaml_text"
 *  (not "yaml" / "content"). */
data class RawConfigPut(
    @SerializedName("yaml_text") val yamlText: String,
)

/** GET /api/logs — tail of Hermes's own activity log (read-only live feed).
 *  Verified shape: { "file":"agent", "lines":[ "2026-...", ... ] }.
 *  Used as the "live output" surface because the WS /api/pty channel is not
 *  reachable from an external client on 0.16.0 (404/403). */
data class LogView(
    @SerializedName("file") val file: String? = null,
    @SerializedName("lines") val lines: List<String>? = null,
)
