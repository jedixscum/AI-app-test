package dev.pocketdev.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pocketdev.app.core.ProviderKind
import dev.pocketdev.app.core.SetupStep
import dev.pocketdev.app.data.AppSettings
import dev.pocketdev.app.data.LogStore
import dev.pocketdev.app.data.SettingsStore
import dev.pocketdev.app.setup.SetupCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val logs = LogStore(app)
    private val settingsStore = SettingsStore(app)
    private val repo = EnvironmentRepository(app, logs, settingsStore)
    private val mutex = Mutex()

    val state: StateFlow<EnvState> = repo.state
    val logEntries = logs.entries
    val settings: StateFlow<AppSettings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _editorFolder = MutableStateFlow<String?>(null)
    val editorFolder: StateFlow<String?> = _editorFolder.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        refreshAll()
    }

    /** Serialises long operations so two Termux jobs never fight over the same state. */
    private fun op(label: String, block: suspend () -> Unit) = viewModelScope.launch {
        mutex.withLock {
            repo.setBusy(label)
            try {
                block()
            } catch (e: Exception) {
                logs.error("app", "$label failed: ${e.javaClass.simpleName}: ${e.message}")
                _events.tryEmit("$label failed: ${e.message}")
            } finally {
                repo.setBusy(null)
            }
        }
    }

    fun refreshAll() = op("Checking environment") {
        repo.refreshTermux()
        repo.checkSteps()
        repo.refreshCodeServer()
        repo.refreshProjects()
        repo.checkProvider()
    }

    fun refreshTermux() = op("Checking Termux") {
        repo.refreshTermux()
        repo.checkSteps()
    }

    fun runStep(step: SetupStep) = op("Installing ${step.title}") { repo.runStep(step) }
    fun runAllMissing() = op("Running setup") { repo.runAllMissing() }

    fun startEditor() = op("Starting code-server") {
        val r = repo.startCodeServer()
        if (!r.succeeded) _events.tryEmit("code-server did not start. See Logs for details.")
    }

    fun stopEditor() = op("Stopping code-server") { repo.stopCodeServer() }
    fun refreshEditor() = op("Checking code-server") { repo.refreshCodeServer() }

    fun createProject(name: String) = op("Creating project") {
        val err = repo.createProject(name)
        _events.tryEmit(err ?: "Created $name")
    }

    fun openInEditor(project: String?) {
        _editorFolder.value = project
    }

    fun saveProvider(kind: ProviderKind, url: String) = op("Saving provider") {
        settingsStore.saveProvider(kind, url)
        repo.checkProvider()
    }

    fun checkProvider() = op("Checking AI provider") { repo.checkProvider() }
    fun runDiagnostics() = op("Running diagnostics") { repo.runDiagnostics() }

    suspend fun readPassword(): String? = repo.readCodeServerPassword()

    fun startKeepAliveTest() = op("Starting keep-alive test") { _events.tryEmit(repo.startKeepAliveTest()) }
    fun checkKeepAliveTest() = op("Checking keep-alive test") { _events.tryEmit(repo.checkKeepAliveTest()) }

    fun clearLogs() = logs.clear()
    fun exportLogs(): String = logs.exportText()

    fun stepsInOrder() = SetupCatalog.steps
}
