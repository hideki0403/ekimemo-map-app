package dev.yukineko.ekimemo_map.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.MainActivity
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.StationTree
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationService : Service() {
    private val logger = Logger("LocationService")
    private val scope = MainScope()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_place)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_search_text))
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setOngoing(true)
            .build()

        runCatching {
            createNotificationChannel()
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
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

        val tracker = NearestStationTracker(app.userDatabase.accessLogDao(), app.userDatabase.passingLogDao())
        val trees = app.stationDataRepository.data.filterNotNull()
            .map { it to StationTree(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(scope, SharingStarted.Eagerly, null)
        scope.launch {
            _location.filterNotNull().collect { location ->
                val (data, tree) = trees.value ?: return@collect
                runCatching { tracker.update(data, tree, location, settings.first()) }
                    .onSuccess { result -> result?.let { _nearbyStations.value = it } }
                    .onFailure { logger.error("Failed to update nearest station", it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onDestroy() {
        scope.cancel()
        _isRunning.value = false
        _location.value = null
        _nearbyStations.value = emptyList()
        logger.info("Location service stopped")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notification_channel_search))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates(intervalMillis: Long) = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(this@LocationService)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateDistanceMeters(10f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    companion object {
        private const val CHANNEL_ID = "search"
        private const val NOTIFICATION_ID = 1

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _location = MutableStateFlow<Location?>(null)
        val location: StateFlow<Location?> = _location.asStateFlow()

        private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
        val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations.asStateFlow()

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationService::class.java))
        }
    }
}
