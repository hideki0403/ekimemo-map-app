package dev.yukineko.ekimemo_map.ui.station

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
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.user.AccessLog
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.LineCard
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.StationAttrIcon
import dev.yukineko.ekimemo_map.ui.ToolAction
import dev.yukineko.ekimemo_map.ui.ToolsFabMenu
import dev.yukineko.ekimemo_map.ui.formatDateTime
import dev.yukineko.ekimemo_map.ui.navigation.LineDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_station_detail)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ToolsFabMenu(
                listOf(
                    ToolAction(R.string.open_map, Icons.Rounded.Map) { onNavigate(MapRoute(stationId = stationId)) },
                    ToolAction(R.string.station_open_radar, Icons.Rounded.Radar) { onNavigate(MapRoute(radarId = stationId)) },
                    ToolAction(R.string.station_open_activity, Icons.Rounded.Train) {
                        uriHandler.openUri("https://ekimemo.com/database/station/$stationId/activity")
                    },
                ),
            )
        },
    ) { padding ->
        val current = station
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
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
            itemsIndexed(lines, key = { _, line -> line.code }) { index, line ->
                LineCard(
                    line = line,
                    index = index,
                    count = lines.size,
                    progress = lineProgress[line.code],
                    onClick = { onNavigate(LineDetailRoute(line.code)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = ListItemDefaults.SegmentedGap / 2),
                )
            }
        }
    }
}

@Composable
private fun StationHeader(station: Station, accessed: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .background(colors.surfaceContainer, MaterialTheme.shapes.extraLargeIncreased)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringArrayResource(R.array.prefectures).getOrElse(station.prefecture - 1) { "" })
        StationAttrIcon(station.attr)
        Text(station.originalName, style = MaterialTheme.typography.headlineSmallEmphasized)
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
    fun format(time: Long?) = time?.let(::formatDateTime) ?: notAccessed
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
