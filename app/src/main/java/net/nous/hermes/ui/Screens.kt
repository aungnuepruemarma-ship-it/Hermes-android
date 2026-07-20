package net.nous.hermes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.nous.hermes.net.models.*

// ---------------------------------------------------------------------------
// Small shared bits
// ---------------------------------------------------------------------------

@Composable
private fun ErrorText(err: String?) {
    err?.let {
        Text("⚠ $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun ScreenScaffold(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            onBack?.let {
                TextButton(onClick = it) { Text("← Back") }
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

// ---------------------------------------------------------------------------
// Home / dashboard (📊 💬 ▶️)
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(vm: HermesViewModel, onNavigate: (String) -> Unit) {
    val status by vm.status.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()

    ScreenScaffold("Hermes") {
        ErrorText(err)
        Spacer(Modifier.height(8.dp))
        Text("gateway: ${status?.gatewayState ?: "—"}   ·   sessions: ${status?.activeSessions ?: 0}")
        stats?.let { s ->
            Text("CPU cores: ${s.cpuCount ?: "?"}")
            s.memory?.let { m ->
                Text("RAM: ${m.percent?.toInt()}%  (${(m.used ?: 0) / 1_048_576} / ${(m.total ?: 0) / 1_048_576} MB)")
            }
            s.disk?.let { d ->
                Text("Disk: ${d.percent?.toInt()}% used")
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.gatewayStart() }) { Text("Start") }
            Button(onClick = { vm.gatewayRestart() }) { Text("Restart") }
            Button(onClick = { vm.gatewayStop() }) { Text("Stop") }
        }
        Spacer(Modifier.height(16.dp))
        val tiles = listOf(
            "Tools" to "tools", "Skills" to "skills", "Sessions" to "sessions",
            "Cron" to "cron", "Memory" to "memory", "Config" to "config",
            "Live Output" to "terminal",
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tiles) { (label, route) ->
                OutlinedButton(onClick = { onNavigate(route) }, Modifier.fillMaxWidth()) {
                    Text(label)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tools (🔌)
// ---------------------------------------------------------------------------

@Composable
fun ToolsScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val toolsets by vm.toolsets.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    ScreenScaffold("Toolsets", onBack) {
        ErrorText(err)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(toolsets) { ts ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ts.enabled, onCheckedChange = { vm.setToolsetEnabled(ts.name, it) })
                    Column {
                        Text(ts.name)
                        ts.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Skills (🔌)
// ---------------------------------------------------------------------------

@Composable
fun SkillsScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val skills by vm.skills.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    ScreenScaffold("Skills", onBack) {
        ErrorText(err)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(skills) { sk ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sk.enabled, onCheckedChange = { vm.toggleSkill(sk.name, it) })
                    Column {
                        Text(sk.name)
                        sk.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sessions (💬)
// ---------------------------------------------------------------------------

@Composable
fun SessionsScreen(vm: HermesViewModel, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    ScreenScaffold("Sessions", onBack) {
        ErrorText(err)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(sessions?.sessions ?: emptyList()) { s ->
                OutlinedButton(onClick = { onOpen(s.id) }, Modifier.fillMaxWidth()) {
                    Text("${s.title ?: s.id}  ·  ${s.model ?: "?"}")
                }
            }
        }
    }
}

@Composable
fun SessionDetailScreen(vm: HermesViewModel, sessionId: String, onBack: () -> Unit) {
    val messages by vm.sessionMessages.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { vm.loadSessionMessages(sessionId) }
    ScreenScaffold("Session", onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages) { m ->
                SelectionContainer {
                    Column(Modifier.fillMaxWidth().padding(4.dp)) {
                        Text("${m.role ?: "?"}:", style = MaterialTheme.typography.labelSmall)
                        Text(m.content ?: "")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Cron (📝)
// ---------------------------------------------------------------------------

@Composable
fun CronScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val jobs by vm.cron.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    ScreenScaffold("Task Queue", onBack) {
        ErrorText(err)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobs) { job ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(job.name ?: job.id)
                        Text("schedule: ${job.schedule ?: "?"}  ·  ${if (job.paused) "paused" else "active"}", style = MaterialTheme.typography.bodySmall)
                        job.lastStatus?.let { Text("last: $it", style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.triggerCron(job.id) }) { Text("Run") }
                            Button(onClick = { vm.pauseCron(job.id) }) { Text("Pause") }
                            Button(onClick = { vm.resumeCron(job.id) }) { Text("Resume") }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Memory (🧠)
// ---------------------------------------------------------------------------

@Composable
fun MemoryScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val memory by vm.memory.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    ScreenScaffold("Memory", onBack) {
        ErrorText(err)
        Text("active provider: ${memory?.active ?: "?"}")
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(memory?.providers ?: emptyList()) { p ->
                Text("• ${p.name}  ${if (p.configured) "✓" else "✗"}")
                p.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.resetMemory() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text("Reset memory")
        }
    }
}

// ---------------------------------------------------------------------------
// Config editor (📂)
// ---------------------------------------------------------------------------

@Composable
fun ConfigScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val config by vm.config.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    var draft by remember(config) { mutableStateOf(config) }

    ScreenScaffold("Config (config.yaml)", onBack) {
        ErrorText(err)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxSize().weight(1f),
            label = { Text("YAML") },
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.saveConfig(draft) }, enabled = !busy) { Text("Save") }
    }
}

// ---------------------------------------------------------------------------
// Live output (📜) — REST /api/logs tail
//
// NOTE: the dashboard's WebSocket channels (/api/pty, /api/ws, /api/pub)
// are NOT reachable from an external native client on hermes-agent 0.16.0
// — /api/pty returns 404 and /api/ws|/api/pub return 403 even with a valid
// token (they are gated for the browser/desktop shell). So the "live output"
// surface here is the REST /api/logs tail, polled on a timer. It shows the
// agent's own activity log. When the backend exposes a usable PTY WS for
// external clients, swap this for a HermesWebSocket("pty") view.
// ---------------------------------------------------------------------------

@Composable
fun LogsScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val lines by vm.logs.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    // Poll every 2s while the screen is composed.
    LaunchedEffect(Unit) {
        vm.refreshLogs()
        while (true) {
            kotlinx.coroutines.delay(2000)
            vm.refreshLogs()
        }
    }
    // Keep scrolled to bottom as new lines arrive.
    LaunchedEffect(lines.size) { scroll.animateScrollTo(scroll.maxValue) }

    ScreenScaffold("Live Output (logs)", onBack) {
        SelectionContainer(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Text(
                lines.joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
        }
    }
}
