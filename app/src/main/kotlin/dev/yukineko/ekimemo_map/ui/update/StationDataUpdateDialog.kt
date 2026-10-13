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
import dev.yukineko.ekimemo_map.ui.update.StationDataUpdateState.Completed
import dev.yukineko.ekimemo_map.ui.update.StationDataUpdateState.Confirm
import dev.yukineko.ekimemo_map.ui.update.StationDataUpdateState.Downloading
import dev.yukineko.ekimemo_map.ui.update.StationDataUpdateState.Failed
import java.util.Locale

@Composable
fun StationDataUpdateDialog(viewModel: StationDataUpdateViewModel) {
    val state = viewModel.state.collectAsState().value ?: return

    AlertDialog(
        onDismissRequest = { if (state !is Downloading) viewModel.dismiss() },
        icon = {
            val icon = when (state) {
                Completed -> Icons.Rounded.DownloadDone
                Failed -> Icons.Rounded.Error
                else -> Icons.Rounded.Download
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
        },
        title = { Text(stringResource(R.string.station_data_update)) },
        text = {
            when (state) {
                is Confirm -> Text(stringResource(R.string.station_data_update_confirm, state.asset.version))
                is Downloading -> DownloadProgress(state.bytes, state.asset.size)
                Completed -> Text(stringResource(R.string.station_data_update_completed))
                Failed -> Text(stringResource(R.string.station_data_update_failed))
            }
        },
        confirmButton = {
            when (state) {
                is Confirm -> DialogButton(R.string.update) { viewModel.start(state.asset) }
                is Downloading -> {}
                Completed, Failed -> DialogButton(R.string.ok, viewModel::dismiss)
            }
        },
        dismissButton = {
            when (state) {
                is Confirm -> DialogButton(R.string.cancel, viewModel::dismiss)
                is Downloading -> DialogButton(R.string.cancel, viewModel::cancel)
                Completed, Failed -> {}
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
        Text(stringResource(R.string.station_data_update_downloading))
        LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            val style = MaterialTheme.typography.bodySmall
            Text(String.format(Locale.ROOT, "%.0f%%", progress * 100), style = style)
            Text("${Formatter.formatShortFileSize(context, bytes)} / ${Formatter.formatShortFileSize(context, size)}", style = style)
        }
    }
}
