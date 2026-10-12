package dev.yukineko.ekimemo_map.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.user.MoveLog
import dev.yukineko.ekimemo_map.data.user.MoveLogDao
import dev.yukineko.ekimemo_map.data.user.MoveLogSessionDao
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.map.MapStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MoveLogTrack(val sessionId: Long, val logs: List<MoveLog>)

class MoveLogMapViewModel(
    sessionIds: List<Long>,
    private val moveLogSessionDao: MoveLogSessionDao,
    moveLogDao: MoveLogDao,
    settings: Flow<Settings>,
) : ViewModel() {
    private val logger = Logger("MoveLogMapViewModel")

    val mapStyle: StateFlow<MapStyle?> = settings.map { it.mapStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tracks: StateFlow<List<MoveLogTrack>?> = moveLogDao.observeBySessionIds(sessionIds)
        .map { logs -> logs.groupBy { it.sessionId }.map { (id, logs) -> MoveLogTrack(id, logs) } }
        .catch {
            logger.error("Failed to load move logs", it)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun delete(sessionId: Long): Boolean =
        runCatching { moveLogSessionDao.deleteByIds(listOf(sessionId)) }
            .onFailure { logger.error("Failed to delete move log session", it) }
            .isSuccess

    companion object {
        fun factory(sessionIds: List<Long>) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                MoveLogMapViewModel(sessionIds, app.userDatabase.moveLogSessionDao(), app.userDatabase.moveLogDao(), app.settings.data)
            }
        }
    }
}
