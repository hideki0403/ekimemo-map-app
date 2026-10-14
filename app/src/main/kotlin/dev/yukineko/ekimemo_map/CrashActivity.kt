package dev.yukineko.ekimemo_map

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.formatDateTime
import dev.yukineko.ekimemo_map.ui.theme.EkimemoMapTheme
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = reportFile(this).takeIf { it.exists() }?.readText()
        if (report == null) {
            restart()
            return
        }
        enableEdgeToEdge()
        setContent {
            EkimemoMapTheme(darkTheme = isSystemInDarkTheme()) {
                CrashScreen(report, onRestart = ::restart)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) reportFile(this).delete()
    }

    private fun restart() {
        reportFile(this).delete()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val CRASH_LOOP_THRESHOLD_MS = 5_000L

        private fun reportFile(context: Context) = File(context.filesDir, "crash_report.txt")

        fun hasReport(context: Context) = reportFile(context).exists()

        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                val file = reportFile(context)
                val isLooping = file.exists() && System.currentTimeMillis() - file.lastModified() < CRASH_LOOP_THRESHOLD_MS
                val saved = runCatching { file.writeText(buildReport(thread, throwable)) }.isSuccess
                if (isLooping || !saved) {
                    defaultHandler?.uncaughtException(thread, throwable)
                    return@setDefaultUncaughtExceptionHandler
                }
                runCatching {
                    context.startActivity(
                        Intent(context, CrashActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }

        private fun buildReport(thread: Thread, throwable: Throwable) = listOf(
            "time: ${formatDateTime(System.currentTimeMillis())}",
            "version: ${BuildConfig.VERSION_NAME}",
            "device: ${Build.MANUFACTURER} ${Build.MODEL}",
            "android: ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}",
            "thread: ${thread.name}",
            "",
            throwable.stackTraceToString(),
        ).joinToString("\n")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashScreen(report: String, onRestart: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val copied = stringResource(R.string.log_copied)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.crash_title))
                    }
                },
                colors = appBarColors(),
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipData.newPlainText("crash_report", report).toClipEntry())
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) snackbar.showSnackbar(copied)
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.crash_copy))
                }
                Button(onClick = onRestart, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.crash_restart))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.crash_message), modifier = Modifier.padding(top = 8.dp))
            SelectionContainer(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Text(
                    report,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
