package dev.yukineko.ekimemo_map.ui.license

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.style.LibraryActionBadges
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import dev.yukineko.ekimemo_map.BuildConfig
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(onBack: () -> Unit) {
    val libraries by produceLibraries(R.raw.aboutlibraries)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_license)) },
                navigationIcon = { BackButton(onBack) },
                colors = appBarColors(),
            )
        },
    ) { padding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            detailMode = LibraryDetailMode.Sheet,
            licenseDialogConfirmText = stringResource(R.string.ok),
            header = { item { AppInfo() } },
        )
    }
}

@Composable
private fun AppInfo() {
    val context = LocalContext.current
    val icon = remember { context.assets.open("icon/app.png").use { BitmapFactory.decodeStream(it).asImageBitmap() } }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(icon, contentDescription = null, modifier = Modifier.size(128.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(
            "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
