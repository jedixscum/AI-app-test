package dev.pocketdev.app.core

/** Pure Kotlin (no Android imports) so it can be unit-tested on the JVM. */

data class CommandResult(
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val errorCode: Int? = null,
    val errorMessage: String? = null,
    val timedOut: Boolean = false,
) {
    /** True only when Termux reported exit code 0 and no Termux-level error. */
    val succeeded: Boolean get() = exitCode == 0 && errorCode == null && !timedOut

    val combinedOutput: String
        get() = listOf(stdout, stderr).filter { it.isNotBlank() }.joinToString("\n").trim()

    companion object {
        fun failure(message: String) = CommandResult(null, "", "", errorMessage = message)
        fun timeout() = CommandResult(
            null, "", "", timedOut = true,
            errorMessage = "Termux did not answer before the timeout",
        )
    }
}

enum class StepState { Unknown, Checking, Missing, Running, Done, Failed }
enum class ServiceState { Unknown, Checking, Running, Stopped, Error }
enum class CheckStatus { Pass, Fail, Skipped }
enum class InferenceLocation { LOCAL, REMOTE }

enum class ProviderKind(val label: String, val defaultUrl: String) {
    OLLAMA("Ollama", "http://127.0.0.1:11434"),
    OPENAI_COMPATIBLE("OpenAI-compatible API", "https://"),
}

sealed interface TermuxStatus {
    data object Unknown : TermuxStatus
    data object Checking : TermuxStatus
    data object NotInstalled : TermuxStatus
    data object PlayStoreBuild : TermuxStatus
    data object PermissionMissing : TermuxStatus
    data class NotResponding(val detail: String) : TermuxStatus
    data class Ready(val version: String?) : TermuxStatus
}

data class SetupStep(
    val id: String,
    val title: String,
    val description: String,
    val installAsset: String,
    val checkAsset: String,
    val sizeHint: String,
)

data class StepProgress(val state: StepState = StepState.Unknown, val output: String = "")

data class DiagnosticResult(val name: String, val status: CheckStatus, val detail: String)

data class DeviceInfo(
    val androidSdk: Int,
    val abis: List<String>,
    val totalRamMb: Long,
    val freeStorageMb: Long,
)

object Shell {
    /** Single-quote a value for safe use inside a POSIX shell command. */
    fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}

object ProjectNames {
    private val valid = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
    fun isValid(name: String): Boolean = valid.matches(name)
}

object Redactor {
    private val rules: List<Pair<Regex, String>> = listOf(
        Regex("(?i)(password\\s*[:=]\\s*)\\S+") to "$1[redacted]",
        Regex("gh[pousr]_[A-Za-z0-9]{20,}") to "[redacted-token]",
        Regex("sk-[A-Za-z0-9_-]{16,}") to "[redacted-key]",
        Regex("(?i)(authorization:\\s*bearer\\s+)\\S+") to "$1[redacted]",
    )

    fun redact(text: String): String = rules.fold(text) { acc, (re, rep) -> re.replace(acc, rep) }
}

object Endpoints {
    /** An endpoint is LOCAL only if it points at this device's loopback interface. */
    fun inferenceLocation(baseUrl: String): InferenceLocation {
        val host = runCatching { java.net.URI(baseUrl.trim()).host }.getOrNull()?.lowercase()
        return when (host) {
            "localhost", "127.0.0.1", "::1", "[::1]" -> InferenceLocation.LOCAL
            else -> InferenceLocation.REMOTE
        }
    }
}
