package dev.yukineko.ekimemo_map.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.StationAttr
import dev.yukineko.ekimemo_map.data.station.StationTree
import dev.yukineko.ekimemo_map.data.station.formatDistance
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.StationNotifier
import dev.yukineko.ekimemo_map.notification.openAppIntent
import dev.yukineko.ekimemo_map.overlay.StationOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val StationAttr.notificationIcon
    get() = when (this) {
        StationAttr.ECO -> R.drawable.ic_notification_attr_eco
        StationAttr.HEAT -> R.drawable.ic_notification_attr_heat
        StationAttr.COOL -> R.drawable.ic_notification_attr_cool
        StationAttr.UNKNOWN -> R.drawable.ic_notification_attr_unknown
    }

class LocationService : Service() {
    private val logger = Logger("LocationService")
    private val scope = MainScope()
    private var overlay: StationOverlay? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        runCatching {
            createNotificationChannel()
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(null, null), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        }.onFailure {
            logger.error("Failed to start foreground service", it)
            stopSelf()
            return
        }

        _isRunning.value = true
        logger.info("Location service started")

        val app = application as EkimemoMapApplication
        val settings = app.settings.data
        scope.launch {
            settings.map { (it.updateFrequency * 1000).toLong() }.distinctUntilChanged().collectLatest { interval ->
                locationUpdates(interval).collect { _location.value = it }
            }
        }

        val overlay = StationOverlay(this, app.settings, app.userDatabase.accessLogDao(), scope)
        this.overlay = overlay
        scope.launch {
            settings.distinctUntilChangedBy { it.enableOverlay to it.overlayCompact }.collect { current ->
                overlay.hide()
                if (!current.enableOverlay) return@collect
                runCatching { overlay.show(current) }
                    .onFailure { logger.error("Failed to show overlay", it) }
            }
        }

        val tracker = NearestStationTracker(app.userDatabase.accessLogDao(), app.userDatabase.passingLogDao())
        val notifier = StationNotifier(this, app.notificationPlayer)
        fun runAssistantFlow() = scope.launch {
            runCatching { app.assistantFlowRunner.run() }
                .onFailure { logger.error("Failed to run assistant flow", it) }
        }
        val trees = app.stationDataRepository.data.filterNotNull()
            .map { it to StationTree(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(scope, SharingStarted.Eagerly, null)
        scope.launch {
            _location.filterNotNull().collect { location ->
                val (data, tree) = trees.value ?: return@collect
                val current = settings.first()
                runCatching {
                    val result = tracker.update(data, tree, location, current) ?: return@runCatching
                    _searchResult.value = SearchResult(result.stations, System.currentTimeMillis(), result.processingMillis)
                    if (result.changed) {
                        val silent = result.inCooldown && !current.enableNotificationDuringCooldown
                        scope.launch {
                            runCatching { notifier.notifyNearest(result.stations.first(), silent, current) }
                                .onFailure { logger.error("Failed to notify nearest station", it) }
                        }
                        runAssistantFlow()
                    }
                }.onFailure { logger.error("Failed to update nearest station", it) }
            }
        }

        val moveLogRecorder = MoveLogRecorder(app.userDatabase.moveLogSessionDao(), app.userDatabase.moveLogDao())
        scope.launch {
            _location.filterNotNull().collect { location ->
                runCatching { moveLogRecorder.record(location, settings.first()) }
                    .onFailure { logger.error("Failed to record move log", it) }
            }
        }

        val reminderAt = MutableStateFlow<Long?>(null)
        scope.launch {
            combine(_searchResult.map { it?.stations?.firstOrNull() }.distinctUntilChanged(), reminderAt, ::Pair)
                .collectLatest { (nearest, at) ->
                    while (true) {
                        updateNotification(nearest, at)
                        if (at == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) break
                        delay((at - System.currentTimeMillis()).mod(1000L) + 1)
                    }
                }
        }

        val accessLogDao = app.userDatabase.accessLogDao()
        scope.launch {
            _searchResult.map { it?.stations?.firstOrNull()?.station }.distinctUntilChangedBy { it?.id }.collectLatest { station ->
                station ?: return@collectLatest
                combine(accessLogDao.observe(station.id), settings, ::Pair).collectLatest { (log, current) ->
                    val now = System.currentTimeMillis()
                    val remaining = cooldownSeconds(log?.lastAccess, current, now)
                    reminderAt.value = if (remaining > 0) now + remaining * 1000L else null
                    if (remaining == 0) return@collectLatest
                    delay(remaining * 1000L)
                    logger.info("Remind: ${station.name} (${station.id})")
                    scope.launch {
                        runCatching { notifier.notifyReminder(station, current) }
                            .onFailure { logger.error("Failed to notify reminder", it) }
                    }
                    runAssistantFlow()
                    runCatching { tracker.recordReminder(station, _location.value) }
                        .onFailure { logger.error("Failed to record reminder", it) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onDestroy() {
        scope.cancel()
        overlay?.hide()
        _isRunning.value = false
        _location.value = null
        _searchResult.value = null
        logger.info("Location service stopped")
        super.onDestroy()
    }

    private fun buildNotification(nearest: NearbyStation?, reminderAt: Long?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_place)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setShowWhen(false)
        if (nearest == null) {
            return builder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_search_text))
                .build()
        }

        builder
            .setSmallIcon(nearest.station.attr.notificationIcon)
            .setContentTitle(nearest.station.name)
            .setSubText(nearest.line?.name)
            .setShortCriticalText(nearest.station.name)
            .setRequestPromotedOngoing(true)
        val distance = formatDistance(nearest.distance)
        val now = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            val style = NotificationCompat.MetricStyle()
                .addMetric(NotificationCompat.Metric(NotificationCompat.Metric.FixedText(distance), getString(R.string.notification_metric_distance)))
            if (reminderAt != null) {
                val timer = NotificationCompat.Metric.TimeDifference.forTimer(
                    SystemClock.elapsedRealtime() + reminderAt - now,
                    NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER,
                )
                style.addMetric(NotificationCompat.Metric(timer, getString(R.string.notification_metric_reminder)))
            }
            return builder
                .setContentText(getString(R.string.notification_search_distance, distance))
                .setStyle(style)
                .build()
        }

        val text = if (reminderAt == null) {
            getString(R.string.notification_search_distance, distance)
        } else {
            val remaining = ((reminderAt - now + 999) / 1000).coerceAtLeast(0)
            getString(R.string.notification_search_distance_reminder, distance, remaining / 60, remaining % 60)
        }
        return builder.setContentText(text).build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(nearest: NearbyStation?, reminderAt: Long?) {
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(nearest, reminderAt))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notification_channel_search))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "search"
        private const val NOTIFICATION_ID = 1

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _location = MutableStateFlow<Location?>(null)
        val location: StateFlow<Location?> = _location.asStateFlow()

        private val _searchResult = MutableStateFlow<SearchResult?>(null)
        val searchResult: StateFlow<SearchResult?> = _searchResult.asStateFlow()

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationService::class.java))
        }
    }
}
