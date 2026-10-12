package dev.yukineko.ekimemo_map.ui.station

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.user.AccessLog
import dev.yukineko.ekimemo_map.ui.LineCard
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.StationAttrIcon
import dev.yukineko.ekimemo_map.ui.navigation.LineDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailScreen(
    stationId: Int,
    onNavigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: StationDetailViewModel = viewModel(factory = StationDetailViewModel.factory(stationId)),
) {
    val station by viewModel.station.collectAsState()
    val lines by viewModel.lines.collectAsState()
    val accessLog by viewModel.accessLog.collectAsState()
    val lineProgress by viewModel.lineProgress.collectAsState()
    val debugMode by viewModel.debugMode.collectAsState()
    var showTools by remember { mutableStateOf(false) }

    if (showTools) {
        ToolsSheet(
            stationId = stationId,
            onDismiss = { showTools = false },
            onNavigate = { showTools = false; onNavigate(it) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_station_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTools = true }) {
                Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.station_tools))
            }
        },
    ) { padding ->
        val current = station
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
        ) {
            item { StationHeader(current, accessLog?.accessed == true) }
            if (debugMode) {
                item { SectionTitle("Station Info (Debug)", Modifier.padding(horizontal = 12.dp)) }
                item {
                    InfoLines(
                        "ID: ${current.id}",
                        "Code: ${current.code}",
                        "Latitude: ${current.lat}",
                        "Longitude: ${current.lng}",
                    )
                }
            }
            item { SectionTitle(stringResource(R.string.station_access_info), Modifier.padding(horizontal = 12.dp)) }
            item { AccessInfo(accessLog) }
            item { SectionTitle(stringResource(R.string.station_line_info), Modifier.padding(horizontal = 12.dp)) }
            items(lines, key = { it.code }) { line ->
                LineCard(
                    line = line,
                    progress = lineProgress[line.code],
                    onClick = { onNavigate(LineDetailRoute(line.code)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StationHeader(station: Station, accessed: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.surfaceContainer).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringArrayResource(R.array.prefectures).getOrElse(station.prefecture - 1) { "" })
        StationAttrIcon(station.attr)
        Text(station.originalName, style = MaterialTheme.typography.headlineSmall)
        Text(station.nameKana)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (accessed) R.string.accessed else R.string.not_accessed),
            modifier = Modifier
                .fillMaxWidth()
                .background(if (accessed) colors.primaryContainer else colors.surfaceBright, CircleShape)
                .padding(vertical = 2.dp),
            color = if (accessed) colors.onPrimaryContainer else colors.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AccessInfo(log: AccessLog?) {
    val notAccessed = stringResource(R.string.not_accessed)
    fun format(time: Long?) = time?.let { dateTimeFormatter.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) } ?: notAccessed
    InfoLines(
        stringResource(R.string.station_access_count, log?.accessCount ?: 0),
        stringResource(R.string.station_last_access, format(log?.lastAccess)),
        stringResource(R.string.station_first_access, format(log?.firstAccess)),
    )
}

@Composable
private fun InfoLines(vararg lines: String) {
    Column(Modifier.padding(horizontal = 28.dp)) {
        lines.forEach { Text(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsSheet(stationId: Int, onDismiss: () -> Unit, onNavigate: (Any) -> Unit) {
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.station_tools),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        ToolItem(R.string.station_open_map, Icons.Rounded.Map) { onNavigate(MapRoute(stationId = stationId)) }
        ToolItem(R.string.station_open_radar, Icons.Rounded.Radar) { onNavigate(MapRoute(radarId = stationId)) }
        ToolItem(R.string.station_open_activity, Icons.Rounded.Train) {
            onDismiss()
            uriHandler.openUri("https://ekimemo.com/database/station/$stationId/activity")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ToolItem(@StringRes text: Int, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(text)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
