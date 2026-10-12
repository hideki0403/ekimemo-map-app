package dev.yukineko.ekimemo_map.ui.route

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.StationCard
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSearchScreen(onNavigate: (Any) -> Unit, onBack: () -> Unit, viewModel: RouteSearchViewModel = viewModel(factory = RouteSearchViewModel.Factory)) {
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var searching by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            searching = true
            if (!viewModel.search(uri)) snackbar.showSnackbar(context.getString(R.string.route_search_failed))
            searching = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_route_search)) },
                navigationIcon = { BackButton(onBack) },
                colors = appBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item { Text(stringResource(R.string.route_search_description), Modifier.padding(horizontal = 16.dp)) }
            item {
                Button(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    shapes = ButtonDefaults.shapes(),
                    enabled = !searching,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.route_search_select_file))
                }
            }
            result?.let { result ->
                item {
                    SectionTitle(
                        stringResource(R.string.route_search_result, result.stations.size, result.elapsedMillis),
                    )
                }
                item {
                    Text(
                        stringResource(R.string.route_search_file_info, result.fileName, result.pointCount),
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(result.stations) { index, item ->
                    StationCard(
                        station = item.station,
                        line = item.line,
                        index = index,
                        count = result.stations.size,
                        onClick = { onNavigate(StationDetailRoute(item.station.id)) },
                    )
                }
            }
        }
    }
}
