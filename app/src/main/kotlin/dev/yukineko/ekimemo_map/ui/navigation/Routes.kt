package dev.yukineko.ekimemo_map.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object DevRoute

@Serializable
data object SettingsRoute

@Serializable
data object AssistantFlowRoute

@Serializable
data object AssistantChooseRectRoute

@Serializable
data object LogRoute

@Serializable
data object ToolsRoute

@Serializable
data object SearchRoute

@Serializable
data object HistoryRoute

@Serializable
data object RouteSearchRoute

@Serializable
data object IntervalTimerRoute

@Serializable
data object MovementLogRoute

@Serializable
data object LicenseRoute

@Serializable
data class MapRoute(
    val stationId: Int? = null,
    val lineId: Int? = null,
    val radarId: Int? = null,
    val sessionIds: List<Long> = emptyList(),
)

@Serializable
data class StationDetailRoute(val stationId: Int)

@Serializable
data class LineDetailRoute(val lineId: Int)
