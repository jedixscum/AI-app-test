package dev.pocketdev.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.pocketdev.app.ui.EditorScreen
import dev.pocketdev.app.ui.HomeScreen
import dev.pocketdev.app.ui.KeepAliveScreen
import dev.pocketdev.app.ui.LogsScreen
import dev.pocketdev.app.ui.PocketDevTheme
import dev.pocketdev.app.ui.SettingsScreen
import dev.pocketdev.app.ui.SetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PocketDevTheme { PocketDevApp() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("setup", "Setup", Icons.Default.Build),
    Tab("editor", "Editor", Icons.Default.Edit),
    Tab("logs", "Logs", Icons.Default.List),
    Tab("settings", "Settings", Icons.Default.Settings),
)

@Composable
fun PocketDevApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }

    fun go(r: String) = nav.navigate(r) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = { go(t.route) },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(vm, onOpenSetup = { go("setup") }, onOpenEditor = { go("editor") }) }
            composable("setup") { SetupScreen(vm, onOpenKeepAlive = { nav.navigate("keepalive") }) }
            composable("keepalive") { KeepAliveScreen(vm, onBack = { nav.popBackStack() }) }
            composable("editor") { EditorScreen(vm) }
            composable("logs") { LogsScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}
