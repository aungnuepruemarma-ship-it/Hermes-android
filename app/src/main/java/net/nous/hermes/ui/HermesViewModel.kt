package net.nous.hermes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.nous.hermes.net.HermesClient
import net.nous.hermes.net.models.*

/**
 * Owns the HermesApi instance and exposes UI state for every screen.
 * The API is built once we have the session token from the launcher service.
 */
class HermesViewModel : ViewModel() {

    private val baseUrl = "http://127.0.0.1:${net.nous.hermes.service.HermesLauncherService.PORT}"
    private val hostHeader = "127.0.0.1:${net.nous.hermes.service.HermesLauncherService.PORT}"

    // ---- state ----
    private val _status = MutableStateFlow<Status?>(null)
    val status: StateFlow<Status?> = _status

    private val _stats = MutableStateFlow<SystemStats?>(null)
    val stats: StateFlow<SystemStats?> = _stats

    private val _toolsets = MutableStateFlow<List<Toolset>>(emptyList())
    val toolsets: StateFlow<List<Toolset>> = _toolsets

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills

    private val _sessions = MutableStateFlow<SessionList?>(null)
    val sessions: StateFlow<SessionList?> = _sessions

    private val _sessionMessages = MutableStateFlow<List<SessionMessage>>(emptyList())
    val sessionMessages: StateFlow<List<SessionMessage>> = _sessionMessages

    private val _cron = MutableStateFlow<List<CronJob>>(emptyList())
    val cron: StateFlow<List<CronJob>> = _cron

    private val _memory = MutableStateFlow<MemoryView?>(null)
    val memory: StateFlow<MemoryView?> = _memory

    private val _config = MutableStateFlow<String>("")
    val config: StateFlow<String> = _config

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private var api: net.nous.hermes.net.HermesApi? = null

    fun attach(token: String) {
        api = HermesClient.create(baseUrl, token, hostHeader)
        refreshAll()
    }

    fun refreshAll() {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { a.getStatus() }.onSuccess { _status.value = it }
                .onFailure { _error.value = it.message }
            runCatching { a.getSystemStats() }.onSuccess { _stats.value = it }
                .onFailure { _error.value = it.message }
            runCatching { a.getToolsets() }.onSuccess { _toolsets.value = it }
                .onFailure { _error.value = it.message }
            runCatching { a.getSkills() }.onSuccess { _skills.value = it }
                .onFailure { _error.value = it.message }
            runCatching { a.listSessions() }.onSuccess { _sessions.value = it }
                .onFailure { _error.value = it.message }
            runCatching { a.getCronJobs() }.onSuccess { _cron.value = it }
                .onFailure { _error.value = it.message }
            runCatching { a.getMemory() }.onSuccess { _memory.value = it }
                .onFailure { _error.value = it.message }
            runCatching { a.getRawConfig() }.onSuccess { _config.value = it.yaml ?: "" }
                .onFailure { _error.value = it.message }
        }
    }

    // ---- mutations ----

    fun setToolsetEnabled(name: String, enabled: Boolean) {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { a.setToolsetEnabled(name, ToggleRequest(enabled)) }
                .onSuccess { refreshTools() }.onFailure { _error.value = it.message }
        }
    }

    fun toggleSkill(name: String, enabled: Boolean) {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { a.toggleSkill(SkillToggleRequest(name, enabled)) }
                .onSuccess { refreshSkills() }.onFailure { _error.value = it.message }
        }
    }

    fun triggerCron(id: String) = cronAction { it.triggerCronJob(id) }
    fun pauseCron(id: String) = cronAction { it.pauseCronJob(id) }
    fun resumeCron(id: String) = cronAction { it.resumeCronJob(id) }

    private fun cronAction(block: suspend (net.nous.hermes.net.HermesApi) -> Unit) {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { block(a) }.onSuccess { refreshCron() }
                .onFailure { _error.value = it.message }
        }
    }

    fun resetMemory() {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { a.resetMemory() }.onSuccess { refreshMemory() }
                .onFailure { _error.value = it.message }
        }
    }

    fun refreshLogs() {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { a.getLogs() }.onSuccess { _logs.value = it.lines ?: emptyList() }
                .onFailure { _error.value = it.message }
        }
    }

    fun saveConfig(yaml: String) {
        val a = api ?: return
        viewModelScope.launch {
            _busy.value = true
            runCatching { a.putRawConfig(RawConfigPut(yaml)) }
                .onSuccess { _config.value = yaml }
                .onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun gatewayStart() = gatewayAction { it.gatewayStart() }
    fun gatewayStop() = gatewayAction { it.gatewayStop() }
    fun gatewayRestart() = gatewayAction { it.gatewayRestart() }

    private fun gatewayAction(block: suspend (net.nous.hermes.net.HermesApi) -> Unit) {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { block(a) }.onSuccess { _status.value = a.getStatus() }
                .onFailure { _error.value = it.message }
        }
    }

    fun loadSessionMessages(id: String) {
        val a = api ?: return
        viewModelScope.launch {
            runCatching { a.getSessionMessages(id) }.onSuccess { _sessionMessages.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    private fun refreshTools() = relaunch { getToolsets() }(_toolsets)
    private fun refreshSkills() = relaunch { getSkills() }(_skills)
    private fun refreshCron() = relaunch { getCronJobs() }(_cron)
    private fun refreshMemory() = relaunch { getMemory() }(_memory)

    private fun <T> relaunch(block: suspend net.nous.hermes.net.HermesApi.() -> T): (MutableStateFlow<T>) -> Unit =
        { flow ->
            val a = api ?: return@relaunch
            viewModelScope.launch {
                runCatching { a.block() }.onSuccess { flow.value = it }
                    .onFailure { _error.value = it.message }
            }
        }
}
