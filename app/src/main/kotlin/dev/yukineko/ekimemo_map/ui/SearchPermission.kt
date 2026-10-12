package dev.yukineko.ekimemo_map.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dev.yukineko.ekimemo_map.R

enum class PermissionIssue(@StringRes val title: Int, @StringRes val message: Int, val blocksSearch: Boolean) {
    NotificationDenied(R.string.permission_required, R.string.notification_permission_denied, false),
    LocationServiceDisabled(R.string.location_service_disabled, R.string.location_service_disabled_message, true),
    LocationDenied(R.string.permission_required, R.string.location_permission_denied, true),
}

private val searchPermissions = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.permissionIssues() = buildList {
    if (!NotificationManagerCompat.from(this@permissionIssues).areNotificationsEnabled()) {
        add(PermissionIssue.NotificationDenied)
    }
    if (!LocationManagerCompat.isLocationEnabled(getSystemService(LocationManager::class.java))) {
        add(PermissionIssue.LocationServiceDisabled)
    }
    if (!isGranted(Manifest.permission.ACCESS_FINE_LOCATION) && !isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
        add(PermissionIssue.LocationDenied)
    }
}

@Composable
fun rememberSearchPermissionRequester(onResult: (List<PermissionIssue>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        currentOnResult(context.permissionIssues())
    }
    return {
        val missing = searchPermissions.filterNot { context.isGranted(it) }
        if (missing.isEmpty()) currentOnResult(context.permissionIssues()) else launcher.launch(missing.toTypedArray())
    }
}

@Composable
fun PermissionIssueDialog(issue: PermissionIssue, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ok)) } },
        title = { Text(stringResource(issue.title)) },
        text = { Text(stringResource(issue.message)) },
    )
}
