package dev.yukineko.ekimemo_map.ui.line

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.ui.LineProgress
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.SimpleStationCard
import dev.yukineko.ekimemo_map.ui.ToolItem
import dev.yukineko.ekimemo_map.ui.ToolsSheet
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import dev.yukineko.ekimemo_map.ui.parseLineColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineDetailScreen(
    lineId: Int,
    onNavigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: LineDetailViewModel = viewModel(factory = LineDetailViewModel.factory(lineId)),
) {
    val line by viewModel.line.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val accessedStationIds by viewModel.accessedStationIds.collectAsState()
    var showTools by remember { mutableStateOf(false) }

    if (showTools) {
        val uriHandler = LocalUriHandler.current
        val dismiss = { showTools = false }
        ToolsSheet(dismiss) {
            ToolItem(R.string.open_map, Icons.Rounded.Map) { dismiss(); onNavigate(MapRoute(lineId = lineId)) }
            ToolItem(R.string.line_open_official, Icons.Rounded.Train) {
                dismiss()
                line?.let { uriHandler.openUri("https://ekimemo.com/database/line/${it.id}") }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_line_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTools = true }) {
                Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.title_tools))
            }
        },
    ) { padding ->
        val current = line
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val progress = LineProgress(current.stationList.count { it in accessedStationIds }, current.stationList.size)
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
        ) {
            item { LineHeader(current) }
            item { SectionTitle(stringResource(R.string.line_info), Modifier.padding(horizontal = 12.dp)) }
            item {
                Column(Modifier.padding(horizontal = 28.dp)) {
                    Text(stringResource(R.string.line_station_count, progress.total))
                    Text(stringResource(R.string.line_accessed_count, progress.accessed, progress.percent))
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (progress.total == 0) 0f else progress.accessed.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item { SectionTitle(stringResource(R.string.line_station_info), Modifier.padding(horizontal = 12.dp)) }
            items(stations, key = { it.id }) { station ->
                SimpleStationCard(
                    station = station,
                    accessed = station.id in accessedStationIds,
                    showAttr = true,
                    onClick = { onNavigate(StationDetailRoute(station.id)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LineHeader(line: Line) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.surfaceContainer).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(line.name, style = MaterialTheme.typography.headlineSmall)
        Text(line.nameKana)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(parseLineColor(line.color) ?: colors.surfaceBright, CircleShape),
        )
        line.nameFormal?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
    }
}
