package dev.pocketdev.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.pocketdev.app.AppViewModel
import dev.pocketdev.app.core.ServiceState
import dev.pocketdev.app.core.StepState
import dev.pocketdev.app.setup.SetupCatalog
import dev.pocketdev.app.termux.TermuxConstants

private const val EDITOR_ORIGIN = "http://127.0.0.1:8080"

@Composable
fun EditorScreen(vm: AppViewModel) {
    val s by vm.state.collectAsState()
    val folder by vm.editorFolder.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var password by remember { mutableStateOf<String?>(null) }
    val toolsReady = SetupCatalog.steps.all { s.steps[it.id]?.state == StepState.Done }

    LaunchedEffect(s.codeServer) {
        if (s.codeServer == ServiceState.Running && password == null) password = vm.readPassword()
    }
    LaunchedEffect(Unit) { vm.refreshEditor() }

    if (s.codeServer != ServiceState.Running) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Editor", style = MaterialTheme.typography.headlineMedium)
            StatusRow("code-server", serviceColor(s.codeServer), s.codeServer.name, s.codeServerDetail, s.codeServer == ServiceState.Checking)
            if (!toolsReady) {
                Text("code-server is not installed and configured yet. Finish the Setup tab first.", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { vm.startEditor() }, enabled = toolsReady && s.busy == null, modifier = Modifier.fillMaxWidth()) { Text("Start editor") }
            OutlinedButton(onClick = { vm.refreshEditor() }, enabled = s.busy == null, modifier = Modifier.fillMaxWidth()) { Text("Check again") }
        }
        return
    }

    val pw = password
    if (pw == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Reading the editor password from Termux…", style = MaterialTheme.typography.bodyMedium)
            Text("If this stays here, the password could not be read. Check the Setup tab.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EDITOR_ORIGIN)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Open in browser") }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(pw)) }) { Text("Copy password") }
        }
        EditorWebView(folder, pw, Modifier.fillMaxSize())
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorWebView(folder: String?, password: String, modifier: Modifier = Modifier) {
    val target = if (folder != null) "/?folder=" + Uri.encode(TermuxConstants.HOME + "/projects/" + folder) else "/"
    val loginUrl = "$EDITOR_ORIGIN/login?to=" + Uri.encode(target)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                        canGoBack = view.canGoBack()
                    }
                }
                postUrl(loginUrl, ("password=" + Uri.encode(password)).toByteArray())
                webView = this
            }
        },
        onRelease = { it.destroy() },
    )
}
