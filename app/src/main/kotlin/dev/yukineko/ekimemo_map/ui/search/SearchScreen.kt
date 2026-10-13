package dev.yukineko.ekimemo_map.ui.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.LineCard
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.SimpleStationCard
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.navigation.LineDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onNavigate: (Any) -> Unit, onBack: () -> Unit, viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)) {
    val result by viewModel.result.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                colors = appBarColors(),
                title = {
                    SearchBarDefaults.InputField(
                        state = viewModel.query,
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = SearchBarDefaults.colors().containerColor.let {
                            SearchBarDefaults.inputFieldColors(focusedContainerColor = it, unfocusedContainerColor = it)
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (viewModel.query.text.isNotEmpty()) {
                                IconButton(onClick = { viewModel.query.clearText() }, shapes = IconButtonDefaults.shapes()) {
                                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        },
                    )
                },
            )
        },
    ) { padding ->
        val itemModifier = Modifier.padding(horizontal = 12.dp, vertical = ListItemDefaults.SegmentedGap / 2)
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 64.dp),
        ) {
            result?.let { result ->
                item { Text(summary(result), Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp)) }
                if (result.stations.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_stations), Modifier.padding(horizontal = 12.dp)) }
                    itemsIndexed(result.stations, key = { _, station -> "station-${station.id}" }) { index, station ->
                        SimpleStationCard(station, index, result.stations.size, onClick = { onNavigate(StationDetailRoute(station.id)) }, modifier = itemModifier, showAttr = true)
                    }
                }
                if (result.lines.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_lines), Modifier.padding(horizontal = 12.dp)) }
                    itemsIndexed(result.lines, key = { _, line -> "line-${line.code}" }) { index, line ->
                        LineCard(line, index, result.lines.size, onClick = { onNavigate(LineDetailRoute(line.code)) }, modifier = itemModifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun summary(result: SearchResult): String {
    val hits = listOfNotNull(
        result.stations.size.takeIf { it > 0 }?.let { stringResource(R.string.search_station_count, it) },
        result.lines.size.takeIf { it > 0 }?.let { stringResource(R.string.search_line_count, it) },
    )
    return if (hits.isEmpty()) {
        stringResource(R.string.search_no_result, result.elapsedMillis)
    } else {
        stringResource(R.string.search_result, hits.joinToString("、"), result.elapsedMillis)
    }
}
