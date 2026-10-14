package dev.pocketdev.app.termux

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dev.pocketdev.app.core.CommandResult
import dev.pocketdev.app.core.TermuxStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Constants from the documented Termux RUN_COMMAND intent contract.
 * NOTE: written from the public Termux wiki; verify against the installed Termux version on a device.
 */
object TermuxConstants {
    const val PACKAGE = "com.termux"
    const val PERMISSION = "com.termux.permission.RUN_COMMAND"
    const val SERVICE_CLASS = "com.termux.app.RunCommandService"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    const val PREFIX = "/data/data/com.termux/files/usr"
    const val HOME = "/data/data/com.termux/files/home"
    const val BASH = "$PREFIX/bin/bash"

    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    // Keys of the result bundle that Termux attaches to our PendingIntent.
    const val RESULT_BUNDLE = "result"
    const val RESULT_STDOUT = "stdout"
    const val RESULT_STDERR = "stderr"
    const val RESULT_EXIT_CODE = "exitCode"
    const val RESULT_ERR = "err"
    const val RESULT_ERRMSG = "errmsg"

    const val EXTRA_EXEC_ID = "pocketdev.exec_id"

    const val F_DROID_URL = "https://f-droid.org/packages/com.termux/"
    const val ENABLE_EXTERNAL_APPS_CMD =
        "mkdir -p ~/.termux && grep -q '^allow-external-apps' ~/.termux/termux.properties 2>/dev/null " +
            "|| echo 'allow-external-apps=true' >> ~/.termux/termux.properties"
}

internal object ResultRegistry {
    val pending = ConcurrentHashMap<Int, CompletableDeferred<CommandResult>>()
    val ids = AtomicInteger(1000)
}

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(TermuxConstants.EXTRA_EXEC_ID, -1)
        val b = intent.getBundleExtra(TermuxConstants.RESULT_BUNDLE)
        val result = if (b == null) {
            CommandResult.failure("Termux returned no result bundle")
        } else {
            val err = b.getInt(TermuxConstants.RESULT_ERR, Activity.RESULT_OK)
            CommandResult(
                exitCode = if (b.containsKey(TermuxConstants.RESULT_EXIT_CODE)) b.getInt(TermuxConstants.RESULT_EXIT_CODE) else null,
                stdout = b.getString(TermuxConstants.RESULT_STDOUT).orEmpty(),
                stderr = b.getString(TermuxConstants.RESULT_STDERR).orEmpty(),
                errorCode = if (err == Activity.RESULT_OK) null else err,
                errorMessage = b.getString(TermuxConstants.RESULT_ERRMSG)?.takeIf { it.isNotBlank() },
            )
        }
        ResultRegistry.pending[id]?.complete(result)
    }
}

object TermuxDetector {
    /** Cheap, local checks. Returns null when the next step is a live round-trip to Termux. */
    fun precheck(context: Context): TermuxStatus? {
        val pm = context.packageManager
        try {
            pm.getPackageInfo(TermuxConstants.PACKAGE, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return TermuxStatus.NotInstalled
        }
        if (installedFromPlayStore(context)) return TermuxStatus.PlayStoreBuild
        val granted = ContextCompat.checkSelfPermission(context, TermuxConstants.PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) null else TermuxStatus.PermissionMissing
    }

    fun versionName(context: Context): String? = try {
        context.packageManager.getPackageInfo(TermuxConstants.PACKAGE, 0).versionName
    } catch (_: Exception) {
        null
    }

    private fun installedFromPlayStore(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 30) {
            context.packageManager.getInstallSourceInfo(TermuxConstants.PACKAGE)
                .installingPackageName == "com.android.vending"
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(TermuxConstants.PACKAGE) == "com.android.vending"
        }
    } catch (_: Exception) {
        false
    }
}

class TermuxBridge(private val context: Context) {

    /** Runs [script] with Termux's bash and waits for the real result. Never fabricates output. */
    suspend fun run(script: String, timeoutMs: Long = 60_000, workDir: String = TermuxConstants.HOME): CommandResult {
        val id = ResultRegistry.ids.incrementAndGet()
        val deferred = CompletableDeferred<CommandResult>()
        ResultRegistry.pending[id] = deferred
        try {
            val callback = Intent(context, TermuxResultReceiver::class.java)
                .putExtra(TermuxConstants.EXTRA_EXEC_ID, id)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(context, id, callback, flags)

            val intent = Intent(TermuxConstants.ACTION_RUN_COMMAND).apply {
                setClassName(TermuxConstants.PACKAGE, TermuxConstants.SERVICE_CLASS)
                putExtra(TermuxConstants.EXTRA_COMMAND_PATH, TermuxConstants.BASH)
                putExtra(TermuxConstants.EXTRA_ARGUMENTS, arrayOf("-c", script))
                putExtra(TermuxConstants.EXTRA_WORKDIR, workDir)
                putExtra(TermuxConstants.EXTRA_BACKGROUND, true)
                putExtra(TermuxConstants.EXTRA_PENDING_INTENT, pending)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                return CommandResult.failure("Could not reach Termux: ${e.javaClass.simpleName}: ${e.message}")
            }
            return withTimeoutOrNull(timeoutMs) { deferred.await() } ?: CommandResult.timeout()
        } finally {
            ResultRegistry.pending.remove(id)
        }
    }
}
