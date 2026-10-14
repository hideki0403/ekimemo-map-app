package dev.yukineko.ekimemo_map.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.formatDistance
import dev.yukineko.ekimemo_map.data.user.AccessLogDao
import dev.yukineko.ekimemo_map.location.LocationService
import dev.yukineko.ekimemo_map.location.NearbyStation
import dev.yukineko.ekimemo_map.location.cooldownSeconds
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.ui.StationAttrIcon
import dev.yukineko.ekimemo_map.ui.formatSeconds
import dev.yukineko.ekimemo_map.ui.listItemContainerColor
import dev.yukineko.ekimemo_map.ui.rememberNow
import dev.yukineko.ekimemo_map.ui.theme.EkimemoMapTheme
import dev.yukineko.ekimemo_map.ui.theme.isDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

private val OverlayType =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

class StationOverlay(
    private val context: Context,
    private val settings: DataStore<Settings>,
    private val accessLogDao: AccessLogDao,
    private val scope: CoroutineScope,
) {
    private val logger = Logger("StationOverlay")
    private val windowManager = checkNotNull(context.getSystemService<WindowManager>())
    private var view: View? = null
    private var owner: OverlayOwner? = null
    private var collapsed by mutableStateOf(false)

    fun show(current: Settings) {
        if (view != null || !android.provider.Settings.canDrawOverlays(context)) return
        val compact = current.overlayCompact
        val params = WindowManager.LayoutParams(
            if (compact) WindowManager.LayoutParams.WRAP_CONTENT else WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            OverlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (compact) current.overlayX else 0
            y = current.overlayY
        }
        val owner = OverlayOwner()
        val composeView = ComposeView(context).apply {
            setContent {
                val s = settings.data.collectAsState(initial = null).value ?: return@setContent
                val result by LocationService.searchResult.collectAsState()
                val stations = result?.stations.orEmpty().take(if (collapsed) 1 else s.overlayMaxResults)
                StationOverlayContent(s, stations, accessLogDao)
            }
        }
        val view = TouchInterceptLayout(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            addView(composeView)
            setOnClickListener { collapsed = !collapsed }
            setOnTouchListener(DragListener(params, horizontal = compact))
        }
        windowManager.addView(view, params)
        this.view = view
        this.owner = owner
    }

    fun hide() {
        view?.let(windowManager::removeView)
        owner?.destroy()
        view = null
        owner = null
    }

    private inner class DragListener(
        private val params: WindowManager.LayoutParams,
        private val horizontal: Boolean,
    ) : View.OnTouchListener {
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val frame = Rect()
        private var startX = 0f
        private var startY = 0f
        private var originX = 0
        private var originY = 0
        private var dragging = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    originX = params.x
                    originY = params.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = if (horizontal) event.rawX - startX else 0f
                    val dy = event.rawY - startY
                    if (!dragging && hypot(dx, dy) > touchSlop) dragging = true
                    if (dragging) {
                        v.getWindowVisibleDisplayFrame(frame)
                        params.x = (originX + dx.roundToInt()).coerceIn(0, (frame.width() - v.width).coerceAtLeast(0))
                        params.y = (originY + dy.roundToInt()).coerceIn(0, (frame.height() - v.height).coerceAtLeast(0))
                        windowManager.updateViewLayout(v, params)
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) savePosition(params.x, params.y, horizontal) else v.performClick()
            }
            return true
        }
    }

    private fun savePosition(x: Int, y: Int, horizontal: Boolean) = scope.launch {
        runCatching { settings.updateData { if (horizontal) it.copy(overlayX = x, overlayY = y) else it.copy(overlayY = y) } }
            .onFailure { logger.error("Failed to save overlay position", it) }
    }
}

private class TouchInterceptLayout(context: Context) : FrameLayout(context) {
    override fun onInterceptTouchEvent(ev: MotionEvent) = true
}

private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StationOverlayContent(settings: Settings, stations: List<NearbyStation>, accessLogDao: AccessLogDao) {
    EkimemoMapTheme(
        darkTheme = settings.themeMode.isDark(),
        useDynamicColor = settings.useDynamicColor,
        seedColor = Color(settings.themeSeedColor),
        font = settings.font,
    ) {
        val containerColor = listItemContainerColor().copy(alpha = settings.overlayOpacity / 100f)
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            when {
                stations.isEmpty() -> OverlayPanel(containerColor) { LoadingIndicator(Modifier.size(32.dp)) }
                settings.overlayCompact -> OverlayPanel(containerColor, Modifier.width(200.dp)) { stations.forEach { CompactStationRow(it) } }
                else -> OverlayPanel(containerColor, Modifier.fillMaxWidth()) { StationTable(stations, settings, accessLogDao) }
            }
        }
    }
}

@Composable
private fun OverlayPanel(containerColor: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = Modifier
            .padding(8.dp)
            .then(modifier)
            .background(containerColor, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun StationTable(stations: List<NearbyStation>, settings: Settings, accessLogDao: AccessLogDao) {
    val ids = stations.map { it.station.id }
    val logs by remember(ids) { accessLogDao.observeByStationIds(ids).map { logs -> logs.associateBy { it.stationId } } }
        .collectAsState(initial = emptyMap())
    val now by rememberNow()
    val cooldowns = stations.map { cooldownSeconds(logs[it.station.id]?.lastAccess, settings, now) }
    val showNumber = stations.size > 1
    val typography = MaterialTheme.typography
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    val rows = stations.size
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showNumber && !settings.showNumberInAttrIcon) {
            TableColumn(rows, horizontalAlignment = Alignment.CenterHorizontally) { Text("${it + 1}", style = typography.labelLarge) }
        }
        TableColumn(rows) {
            StationAttrIcon(stations[it].station.attr, size = 28.dp, number = (it + 1).takeIf { showNumber && settings.showNumberInAttrIcon })
        }
        TableColumn(rows, Modifier.widthIn(max = 160.dp)) {
            Text(stations[it].station.name, style = typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TableColumn(rows, Modifier.weight(1f)) {
            Text(stations[it].line?.name.orEmpty(), style = typography.bodySmall, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (cooldowns.any { it > 0 }) {
            TableColumn(rows, horizontalAlignment = Alignment.End) {
                if (cooldowns[it] > 0) Text(formatSeconds(cooldowns[it]), style = typography.labelMedium, color = subColor)
            }
        }
        TableColumn(rows, horizontalAlignment = Alignment.End) { Text(formatDistance(stations[it].distance), style = typography.labelLarge) }
    }
}

@Composable
private fun TableColumn(
    rows: Int,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    cell: @Composable (Int) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = horizontalAlignment) {
        repeat(rows) { Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) { cell(it) } }
    }
}

@Composable
private fun CompactStationRow(item: NearbyStation) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StationAttrIcon(item.station.attr, size = 28.dp)
        Text(
            item.station.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(formatDistance(item.distance), style = MaterialTheme.typography.labelLarge)
    }
}
