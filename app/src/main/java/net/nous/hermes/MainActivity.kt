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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the foreground service that launches `hermes dashboard`
        // inside Termux with a deterministic session token.
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
                    // Token is stored in EncryptedSharedPreferences by the service.
                    val t = EncryptedPrefs.getToken(this@MainActivity)
                    token = t
                    t?.let { vm.attach(it) }
                }

                val nav = rememberNavController()
                token?.let { tk ->
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
                } ?: run {
                    androidx.compose.material3.Text("Connecting to Hermes runtime…")
                }
            }
        }
    }
}
