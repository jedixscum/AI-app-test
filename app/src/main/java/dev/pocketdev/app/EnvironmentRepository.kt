package dev.pocketdev.app

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import dev.pocketdev.app.ai.ProviderCheck
import dev.pocketdev.app.ai.ProviderFactory
import dev.pocketdev.app.core.CheckStatus
import dev.pocketdev.app.core.CommandResult
import dev.pocketdev.app.core.DeviceInfo
import dev.pocketdev.app.core.DiagnosticResult
import dev.pocketdev.app.core.Http
import dev.pocketdev.app.core.ProjectNames
import dev.pocketdev.app.core.Redactor
import dev.pocketdev.app.core.ServiceState
import dev.pocketdev.app.core.SetupStep
import dev.pocketdev.app.core.Shell
import dev.pocketdev.app.core.StepProgress
import dev.pocketdev.app.core.StepState
import dev.pocketdev.app.core.TermuxStatus
import dev.pocketdev.app.data.LogStore
import dev.pocketdev.app.data.SettingsStore
import dev.pocketdev.app.setup.SetupCatalog
import dev.pocketdev.app.termux.TermuxBridge
import dev.pocketdev.app.termux.TermuxDetector
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EnvState(
    val termux: TermuxStatus = TermuxStatus.Unknown,
    val steps: Map<String, StepProgress> = emptyMap(),
    val codeServer: ServiceState = ServiceState.Unknown,
    val codeServerDetail: String = "",
    val provider: ProviderCheck? = null,
    val projects: List<String> = emptyList(),
    val device: DeviceInfo,
    val diagnostics: List<DiagnosticResult> = emptyList(),
    val busy: String? = null,
)

object DeviceProbe {
    private const val MB = 1024L * 1024L
    fun read(context: Context): DeviceInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().path)
        return DeviceInfo(Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS.toList(), mi.totalMem / MB, stat.availableBytes / MB)
    }
}

class EnvironmentRepository(
    private val context: Context,
    private val logs: LogStore,
    private val settings: SettingsStore,
) {
    private val bridge = TermuxBridge(context)
    private val _state = MutableStateFlow(EnvState(device = DeviceProbe.read(context)))
    val state: StateFlow<EnvState> = _state.asStateFlow()

    private fun asset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    private fun setStep(id: String, state: StepState, output: String? = null) = _state.update { s ->
        val old = s.steps[id] ?: StepProgress()
        s.copy(steps = s.steps + (id to StepProgress(state, output ?: old.output)))
    }

    fun setBusy(label: String?) = _state.update { it.copy(busy = label, device = DeviceProbe.read(context)) }

    private fun termuxReady() = _state.value.termux is TermuxStatus.Ready

    // ---- Termux ----------------------------------------------------------------------------

    suspend fun refreshTermux() {
        _state.update { it.copy(termux = TermuxStatus.Checking) }
        val pre = TermuxDetector.precheck(context)
        if (pre != null) {
            _state.update { it.copy(termux = pre) }
            logs.warn("termux", "Precheck: $pre")
            return
        }
        val r = bridge.run("echo pocketdev-ok", 20_000)
        val status = if (r.succeeded && r.stdout.contains("pocketdev-ok")) {
            TermuxStatus.Ready(TermuxDetector.versionName(context))
        } else {
            TermuxStatus.NotResponding(
                r.errorMessage ?: r.combinedOutput.ifBlank { "No answer. Is allow-external-apps enabled in Termux?" },
            )
        }
        _state.update { it.copy(termux = status) }
        if (status is TermuxStatus.Ready) logs.info("termux", "Round-trip OK (Termux ${status.version})")
        else logs.error("termux", "Round-trip failed: $status")
    }

    // ---- Setup steps -----------------------------------------------------------------------

    suspend fun checkSteps() {
        if (!termuxReady()) return
        for (step in SetupCatalog.steps) {
            setStep(step.id, StepState.Checking)
            val r = bridge.run(asset(step.checkAsset), 100_000)
            setStep(step.id, classify(r), Redactor.redact(r.combinedOutput.ifBlank { r.errorMessage.orEmpty() }).takeLast(1500))
        }
    }

    private fun classify(r: CommandResult): StepState = when {
        r.succeeded -> StepState.Done
        r.timedOut || r.errorMessage != null || r.errorCode != null -> StepState.Failed
        else -> StepState.Missing
    }

    suspend fun runStep(step: SetupStep) {
        if (!termuxReady()) {
            logs.warn("setup", "Cannot run ${step.id}: Termux is not ready")
            return
        }
        setStep(step.id, StepState.Running, "Starting…")
        logs.info("setup", "Running step ${step.id}")
        val logPath = "~/.pocketdev/logs/${step.id}.log"
        val wrapped = "mkdir -p ~/.pocketdev/logs\n( set -e; set -o pipefail\n${asset(step.installAsset)}\n) > $logPath 2>&1"
        val result = coroutineScope {
            val poller = launch {
                while (isActive) {
                    delay(3_000)
                    val t = bridge.run("tail -n 12 $logPath 2>/dev/null", 10_000)
                    if (t.succeeded && t.stdout.isNotBlank()) setStep(step.id, StepState.Running, Redactor.redact(t.stdout))
                }
            }
            try {
                bridge.run(wrapped, 30 * 60_000L)
            } finally {
                poller.cancel()
            }
        }
        val tail = bridge.run("tail -n 25 $logPath 2>/dev/null", 10_000).stdout
        if (!result.succeeded) {
            logs.error("setup", "Step ${step.id} failed (exit=${result.exitCode}, ${result.errorMessage ?: "no Termux error"})")
            setStep(step.id, StepState.Failed, Redactor.redact(tail.ifBlank { result.errorMessage.orEmpty() }))
            return
        }
        // Only report success once the independent check agrees.
        val verify = bridge.run(asset(step.checkAsset), 100_000)
        if (verify.succeeded) {
            logs.info("setup", "Step ${step.id} verified")
            setStep(step.id, StepState.Done, Redactor.redact(verify.combinedOutput))
        } else {
            logs.error("setup", "Step ${step.id} ran but verification failed")
            setStep(step.id, StepState.Failed, Redactor.redact("Install finished but the check failed:\n${verify.combinedOutput}\n$tail"))
        }
    }

    suspend fun runAllMissing() {
        for (step in SetupCatalog.steps) {
            val current = _state.value.steps[step.id]?.state
            if (current == StepState.Done) continue
            runStep(step)
            if (_state.value.steps[step.id]?.state != StepState.Done) return
        }
    }

    // ---- code-server -----------------------------------------------------------------------

    suspend fun refreshCodeServer() {
        _state.update { it.copy(codeServer = ServiceState.Checking) }
        try {
            val r = Http.get("http://127.0.0.1:8080/healthz", timeoutMs = 2500)
            _state.update {
                if (r.code in 200..299) it.copy(codeServer = ServiceState.Running, codeServerDetail = "Healthy on 127.0.0.1:8080")
                else it.copy(codeServer = ServiceState.Error, codeServerDetail = "Health endpoint answered HTTP ${r.code}")
            }
        } catch (e: Exception) {
            _state.update { it.copy(codeServer = ServiceState.Stopped, codeServerDetail = "Nothing is listening on 127.0.0.1:8080") }
        }
    }

    suspend fun startCodeServer(): CommandResult {
        val r = bridge.run(asset("setup/start_codeserver.sh"), 60_000)
        if (r.succeeded) logs.info("editor", "code-server: ${r.stdout.trim()}")
        else logs.error("editor", "code-server start failed: ${r.combinedOutput.ifBlank { r.errorMessage.orEmpty() }}")
        refreshCodeServer()
        return r
    }

    suspend fun stopCodeServer() {
        val r = bridge.run(asset("setup/stop_codeserver.sh"), 30_000)
        logs.info("editor", "code-server stop: exit=${r.exitCode}")
        refreshCodeServer()
    }

    suspend fun readCodeServerPassword(): String? {
        val r = bridge.run(asset("setup/read_password.sh"), 15_000)
        return r.stdout.trim().takeIf { r.succeeded && it.isNotEmpty() }
    }

    // ---- Projects --------------------------------------------------------------------------

    suspend fun refreshProjects() {
        if (!termuxReady()) return
        val r = bridge.run("mkdir -p ~/projects && ls -1t ~/projects | head -n 30", 20_000)
        if (r.succeeded) _state.update { it.copy(projects = r.stdout.lines().filter { l -> l.isNotBlank() }) }
    }

    /** Returns an error message, or null on success. */
    suspend fun createProject(name: String): String? {
        if (!ProjectNames.isValid(name)) return "Use letters, numbers, dots, dashes or underscores (max 64)."
        if (!termuxReady()) return "Termux is not ready."
        val q = Shell.quote(name)
        val r = bridge.run("mkdir -p ~/projects && cd ~/projects && mkdir -- $q && cd -- $q && (git init -q || true) && test -d .", 30_000)
        return if (r.succeeded) {
            logs.info("projects", "Created project $name")
            refreshProjects()
            null
        } else {
            logs.error("projects", "Create project failed: ${r.combinedOutput}")
            r.combinedOutput.ifBlank { r.errorMessage ?: "Could not create the project (does it already exist?)" }
        }
    }

    // ---- Keep-alive test -------------------------------------------------------------------

    suspend fun startKeepAliveTest(): String {
        if (!termuxReady()) return "Termux is not ready."
        val r = bridge.run(asset("setup/keepalive_start.sh"), 20_000)
        logs.info("keepalive", "Test start: exit=${r.exitCode}")
        return if (r.succeeded) r.stdout.trim() else r.combinedOutput.ifBlank { r.errorMessage ?: "Could not start the test" }
    }

    /** Exit 0 = alive, 1 = killed, 2 = never started. The result text comes straight from the check script. */
    suspend fun checkKeepAliveTest(): String {
        if (!termuxReady()) return "Termux is not ready."
        val r = bridge.run(asset("setup/keepalive_check.sh"), 20_000)
        logs.info("keepalive", "Test check: exit=${r.exitCode} ${r.stdout.trim()}")
        return r.stdout.trim().ifBlank { r.errorMessage ?: "No answer from Termux" }
    }

    // ---- AI provider -----------------------------------------------------------------------

    suspend fun checkProvider() {
        val s = settings.settings.first()
        val check = ProviderFactory.create(s.providerKind, s.providerBaseUrl).check()
        _state.update { it.copy(provider = check) }
        logs.info("ai", "Provider check (${s.providerKind}): reachable=${check.reachable}")
    }

    // ---- Diagnostics -----------------------------------------------------------------------

    suspend fun runDiagnostics() {
        val out = mutableListOf<DiagnosticResult>()
        fun add(name: String, ok: Boolean, detail: String) = out.add(DiagnosticResult(name, if (ok) CheckStatus.Pass else CheckStatus.Fail, detail))
        fun skip(name: String, why: String) = out.add(DiagnosticResult(name, CheckStatus.Skipped, why))

        refreshTermux()
        val t = _state.value.termux
        add("Termux round-trip", t is TermuxStatus.Ready, if (t is TermuxStatus.Ready) "Command executed and result returned" else t.toString())

        if (t is TermuxStatus.Ready) {
            val w = bridge.run("echo ok > ~/.pocketdev-write-test && cat ~/.pocketdev-write-test && rm ~/.pocketdev-write-test", 20_000)
            add("Termux file write", w.succeeded && w.stdout.trim() == "ok", w.combinedOutput.ifBlank { w.errorMessage.orEmpty() })
            for (step in SetupCatalog.steps) {
                val r = bridge.run(asset(step.checkAsset), 100_000)
                add(step.title, r.succeeded, r.combinedOutput.ifBlank { r.errorMessage.orEmpty() })
            }
        } else {
            skip("Linux and editor tools", "Needs a working Termux connection")
        }
        refreshCodeServer()
        val cs = _state.value
        add("code-server health", cs.codeServer == ServiceState.Running, cs.codeServerDetail)

        checkProvider()
        val p = _state.value.provider
        if (p != null) out.add(DiagnosticResult("AI provider", if (p.reachable) CheckStatus.Pass else CheckStatus.Fail, p.detail))

        _state.update { it.copy(diagnostics = out) }
        logs.info("diagnostics", out.joinToString("; ") { "${it.name}=${it.status}" })
    }
}
