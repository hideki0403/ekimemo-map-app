package dev.yukineko.ekimemo_map.ui.movelog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.user.MoveLogSessionSummary
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.listItemColors
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import dev.yukineko.ekimemo_map.ui.theme.isDark
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val dayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 (E)", Locale.JAPAN)
private val shortDayFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")
private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月")

private fun formatTime(epochMillis: Long) = timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MovementLogScreen(onNavigate: (Any) -> Unit, onBack: () -> Unit, viewModel: MovementLogViewModel = viewModel(factory = MovementLogViewModel.Factory)) {
    val days by viewModel.days.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var useCalendar by rememberSaveable { mutableStateOf(true) }
    var deleting by remember { mutableStateOf<MoveLogSessionSummary?>(null) }
    val openMap = { sessions: List<MoveLogSessionSummary> -> onNavigate(MapRoute(sessionIds = sessions.map { it.id })) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_movement_log)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { useCalendar = !useCalendar }, shapes = IconButtonDefaults.shapes()) {
                        if (useCalendar) {
                            Icon(Icons.AutoMirrored.Rounded.List, contentDescription = stringResource(R.string.move_log_show_list))
                        } else {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.move_log_show_calendar))
                        }
                    }
                },
                colors = appBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val current = days
        when {
            current == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            current.isEmpty() -> Text(
                stringResource(R.string.move_log_list_empty),
                Modifier.fillMaxWidth().padding(padding).padding(top = 36.dp),
                textAlign = TextAlign.Center,
            )
            useCalendar -> CalendarView(current, padding, onOpen = openMap, onDelete = { deleting = it })
            else -> DayListView(current, padding, onOpen = openMap, onDelete = { deleting = it })
        }
    }

    deleting?.let { session ->
        val deleted = stringResource(R.string.move_log_deleted)
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.move_log_delete_title)) },
            text = { Text(stringResource(R.string.move_log_delete_message, session.count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = null
                        scope.launch { if (viewModel.delete(session.id)) snackbar.showSnackbar(deleted) }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun CalendarView(
    days: Map<LocalDate, List<MoveLogSessionSummary>>,
    padding: PaddingValues,
    onOpen: (List<MoveLogSessionSummary>) -> Unit,
    onDelete: (MoveLogSessionSummary) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val selectedDay = selected ?: days.keys.last()
    val startMonth = YearMonth.from(days.keys.first())
    val endMonth = YearMonth.from(days.keys.last())
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.from(selectedDay),
        firstDayOfWeek = DayOfWeek.SUNDAY,
    )
    LaunchedEffect(startMonth, endMonth) {
        state.startMonth = startMonth
        state.endMonth = endMonth
    }
    val visibleMonth = state.firstVisibleMonth.yearMonth
    val sessions = days[selectedDay]

    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            IconButton(
                onClick = { scope.launch { state.animateScrollToMonth(visibleMonth.minusMonths(1)) } },
                enabled = visibleMonth > startMonth,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.calendar_previous_month))
            }
            Text(
                monthFormatter.format(visibleMonth),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { scope.launch { state.animateScrollToMonth(visibleMonth.plusMonths(1)) } },
                enabled = visibleMonth < endMonth,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.calendar_next_month))
            }
        }
        DaysOfWeekHeader()
        HorizontalCalendar(
            state = state,
            modifier = Modifier.padding(horizontal = 8.dp),
            dayContent = { day ->
                DayCell(day, selected = day.date == selectedDay, enabled = day.date in days) { selected = day.date }
            },
        )
        HorizontalDivider(Modifier.padding(top = 8.dp))
        if (sessions == null) {
            Text(
                stringResource(R.string.move_log_day_empty, shortDayFormatter.format(selectedDay)),
                Modifier.fillMaxWidth().padding(top = 36.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(sessions, key = { _, session -> session.id }) { index, session ->
                    SessionItem(session, index, sessions.size, onClick = { onOpen(listOf(session)) }, onDelete = { onDelete(session) })
                }
            }
            ShowAllButton(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { onOpen(sessions) }
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val dark = MaterialTheme.colorScheme.isDark
    Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        daysOfWeek(DayOfWeek.SUNDAY).forEach { dayOfWeek ->
            val color = when (dayOfWeek) {
                DayOfWeek.SUNDAY -> if (dark) Color(0xFFE57373) else Color(0xFFEF5350)
                DayOfWeek.SATURDAY -> if (dark) Color(0xFF64B5F6) else Color(0xFF42A5F5)
                else -> Color.Unspecified
            }
            Text(
                dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPAN),
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (day.position != DayPosition.MonthDate) return
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        selected -> colors.onPrimary
        day.date == LocalDate.now() -> colors.primary
        else -> colors.onSurface
    }
    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primary else Color.Transparent)
                .clickable(enabled = enabled, onClick = onClick)
                .alpha(if (enabled) 1f else 0.35f),
        ) {
            Text(day.date.dayOfMonth.toString(), color = textColor)
        }
    }
}

@Composable
private fun DayListView(
    days: Map<LocalDate, List<MoveLogSessionSummary>>,
    padding: PaddingValues,
    onOpen: (List<MoveLogSessionSummary>) -> Unit,
    onDelete: (MoveLogSessionSummary) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(days.entries.reversed(), key = { it.key.toEpochDay() }) { (date, sessions) ->
            var expanded by rememberSaveable { mutableStateOf(false) }
            val count = if (expanded) sessions.size + 1 else 1
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                SegmentedListItem(
                    onClick = { expanded = !expanded },
                    onLongClick = { onOpen(sessions) },
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    colors = listItemColors(),
                    trailingContent = { Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dayFormatter.format(date), style = MaterialTheme.typography.titleMedium)
                }
                AnimatedVisibility(expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        sessions.forEachIndexed { index, session ->
                            SessionItem(session, index + 1, count, onClick = { onOpen(listOf(session)) }, onDelete = { onDelete(session) })
                        }
                        ShowAllButton(Modifier.padding(top = 8.dp, bottom = 8.dp)) { onOpen(sessions) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionItem(session: MoveLogSessionSummary, index: Int, count: Int, onClick: () -> Unit, onDelete: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = listItemColors(),
        supportingContent = { Text(stringResource(R.string.move_log_point_count, session.count)) },
        trailingContent = {
            IconButton(onClick = onDelete, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.move_log_session_time, formatTime(session.firstTimestamp), formatTime(session.lastTimestamp)))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShowAllButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, shapes = ButtonDefaults.shapes(), modifier = modifier.fillMaxWidth().height(48.dp)) {
        Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Text(stringResource(R.string.move_log_show_all), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
    }
}
