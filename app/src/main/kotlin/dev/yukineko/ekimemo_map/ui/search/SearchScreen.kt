package dev.yukineko.ekimemo_map.ui.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.LineCard
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.SimpleStationCard
import dev.yukineko.ekimemo_map.ui.navigation.LineDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onNavigate: (Any) -> Unit, onBack: () -> Unit, viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)) {
    val result by viewModel.result.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val itemModifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 64.dp),
        ) {
            item {
                TextField(
                    value = viewModel.query,
                    onValueChange = { viewModel.query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (viewModel.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.query = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }
            result?.let { result ->
                item { Text(summary(result), Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp)) }
                if (result.stations.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_stations), Modifier.padding(horizontal = 12.dp)) }
                    items(result.stations, key = { "station-${it.id}" }) { station ->
                        SimpleStationCard(station, onClick = { onNavigate(StationDetailRoute(station.id)) }, modifier = itemModifier, showAttr = true)
                    }
                }
                if (result.lines.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_lines), Modifier.padding(horizontal = 12.dp)) }
                    items(result.lines, key = { "line-${it.code}" }) { line ->
                        LineCard(line, onClick = { onNavigate(LineDetailRoute(line.code)) }, modifier = itemModifier)
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
