package net.nous.hermes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.nous.hermes.service.EncryptedPrefs
import net.nous.hermes.service.HermesLauncherService
import net.nous.hermes.ui.*

class MainActivity : ComponentActivity() {
    // Fallback token used when the app did NOT launch the dashboard itself
    // (e.g. the dashboard is started manually in Termux). The Termux side must
    // launch the dashboard with the SAME value:
    //   HERMES_DASHBOARD_SESSION_TOKEN=this-value hermes dashboard --host 127.0.0.1 --port 9119 --no-open
    // When the app's own foreground service launches the dashboard, it overrides
    // this with a randomly generated token stored in EncryptedSharedPreferences.
    private val FALLBACK_TOKEN = "hermes-android-local-frontend"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the foreground service that launches `hermes dashboard` inside
        // Termux with a deterministic session token. If Termux cannot be spawned
        // (app sandbox), the dashboard must be started manually in Termux with
        // HERMES_DASHBOARD_SESSION_TOKEN set to FALLBACK_TOKEN.
        startForegroundService(
            Intent(this, HermesLauncherService::class.java).apply {
                action = HermesLauncherService.ACTION_START
            }
        )

        setContent {
            MaterialTheme {
                val vm: HermesViewModel = viewModel()
                var token by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    // Prefer a token the service generated+stored; otherwise fall
                    // back to the known local token so we can still attach to a
                    // manually-started dashboard.
                    val stored = EncryptedPrefs.getToken(this@MainActivity)
                    token = stored ?: FALLBACK_TOKEN
                    vm.attach(token!!)
                }

                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") { HomeScreen(vm) { nav.navigate(it) } }
                    composable("tools") { ToolsScreen(vm) { nav.popBackStack() } }
                    composable("skills") { SkillsScreen(vm) { nav.popBackStack() } }
                    composable("sessions") {
                        SessionsScreen(vm, onOpen = { nav.navigate("session/$it") }) { nav.popBackStack() }
                    }
                    composable("session/{id}") {
                        SessionDetailScreen(vm, it.arguments!!.getString("id")!!) { nav.popBackStack() }
                    }
                    composable("cron") { CronScreen(vm) { nav.popBackStack() } }
                    composable("memory") { MemoryScreen(vm) { nav.popBackStack() } }
                    composable("config") { ConfigScreen(vm) { nav.popBackStack() } }
                    composable("terminal") { LogsScreen(vm) { nav.popBackStack() } }
                }
            }
        }
    }
}
