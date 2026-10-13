package dev.yukineko.ekimemo_map.ui.update

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.update.UpdateState.Completed
import dev.yukineko.ekimemo_map.ui.update.UpdateState.Confirm
import dev.yukineko.ekimemo_map.ui.update.UpdateState.Downloading
import dev.yukineko.ekimemo_map.ui.update.UpdateState.Failed
import dev.yukineko.ekimemo_map.ui.update.UpdateState.Installing
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateDialog(viewModel: UpdateViewModel) {
    val state = viewModel.state.collectAsState().value ?: return
    val app = state.target == UpdateTarget.APP

    AlertDialog(
        onDismissRequest = { if (state !is Downloading && state !is Installing) viewModel.dismiss() },
        icon = {
            val icon = when (state) {
                is Completed -> Icons.Rounded.DownloadDone
                is Failed -> Icons.Rounded.Error
                else -> if (app) Icons.Rounded.SystemUpdate else Icons.Rounded.Download
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
        },
        title = { Text(stringResource(if (app) R.string.app_update else R.string.station_data_update)) },
        text = {
            when (state) {
                is Confirm -> Text(
                    stringResource(if (app) R.string.app_update_confirm else R.string.station_data_update_confirm, state.asset.version),
                )
                is Downloading -> DownloadProgress(state.bytes, state.asset.size)
                is Installing -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.app_update_installing))
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                }
                is Completed -> Text(stringResource(if (app) R.string.app_update_completed else R.string.station_data_update_completed))
                is Failed -> Text(stringResource(if (app) R.string.app_update_failed else R.string.station_data_update_failed))
            }
        },
        confirmButton = {
            when (state) {
                is Confirm -> DialogButton(R.string.update) { viewModel.start(state.target, state.asset) }
                is Completed, is Failed -> DialogButton(R.string.ok, viewModel::dismiss)
                is Downloading, is Installing -> {}
            }
        },
        dismissButton = {
            when (state) {
                is Confirm -> DialogButton(R.string.cancel, viewModel::dismiss)
                is Downloading -> DialogButton(R.string.cancel, viewModel::cancel)
                is Installing, is Completed, is Failed -> {}
            }
        },
    )
}

@Composable
private fun DialogButton(@StringRes label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, shapes = ButtonDefaults.shapes()) { Text(stringResource(label)) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadProgress(bytes: Long, size: Long) {
    val context = LocalContext.current
    val progress = if (size > 0) (bytes.toFloat() / size).coerceAtMost(1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.update_downloading))
        LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            val style = MaterialTheme.typography.bodySmall
            Text(String.format(Locale.ROOT, "%.0f%%", progress * 100), style = style)
            Text("${Formatter.formatShortFileSize(context, bytes)} / ${Formatter.formatShortFileSize(context, size)}", style = style)
        }
    }
}
