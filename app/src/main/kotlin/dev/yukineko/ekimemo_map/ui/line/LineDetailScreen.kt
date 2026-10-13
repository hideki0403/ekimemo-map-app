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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.listItemContainerColor
import dev.yukineko.ekimemo_map.ui.LineProgress
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.SimpleStationCard
import dev.yukineko.ekimemo_map.ui.ToolAction
import dev.yukineko.ekimemo_map.ui.ToolsFabMenu
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import dev.yukineko.ekimemo_map.ui.rememberLineColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_line_detail)) },
                navigationIcon = { BackButton(onBack) },
                colors = appBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ToolsFabMenu(
                listOf(
                    ToolAction(R.string.open_map, Icons.Rounded.Map) { onNavigate(MapRoute(lineId = lineId)) },
                    ToolAction(R.string.line_open_official, Icons.Rounded.Train) {
                        line?.let { uriHandler.openUri("https://ekimemo.com/database/line/${it.id}") }
                    },
                ),
            )
        },
    ) { padding ->
        val current = line
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
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
                    LinearWavyProgressIndicator(
                        progress = { if (progress.total == 0) 0f else progress.accessed.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth(),
                        waveSpeed = 0.dp,
                    )
                }
            }
            item { SectionTitle(stringResource(R.string.line_station_info), Modifier.padding(horizontal = 12.dp)) }
            itemsIndexed(stations, key = { _, station -> station.id }) { index, station ->
                SimpleStationCard(
                    station = station,
                    index = index,
                    count = stations.size,
                    accessed = station.id in accessedStationIds,
                    showAttr = true,
                    onClick = { onNavigate(StationDetailRoute(station.id)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = ListItemDefaults.SegmentedGap / 2),
                )
            }
        }
    }
}

@Composable
private fun LineHeader(line: Line) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = listItemContainerColor(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(line.name, style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(line.nameKana)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(rememberLineColors(line.color)?.accent ?: colors.surfaceContainerHighest, CircleShape),
            )
            line.nameFormal?.let {
                Spacer(Modifier.height(8.dp))
                Text(it)
            }
        }
    }
}
