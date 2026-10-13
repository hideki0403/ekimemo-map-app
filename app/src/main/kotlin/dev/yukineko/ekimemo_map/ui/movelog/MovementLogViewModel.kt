package dev.yukineko.ekimemo_map.ui.movelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.user.MoveLogSessionDao
import dev.yukineko.ekimemo_map.data.user.MoveLogSessionSummary
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MovementLogViewModel(private val moveLogSessionDao: MoveLogSessionDao) : ViewModel() {
    private val logger = Logger("MovementLogViewModel")

    val days: StateFlow<Map<LocalDate, List<MoveLogSessionSummary>>?> = moveLogSessionDao.observeSummaries()
        .map { sessions -> sessions.groupBy { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() } }
        .catch {
            logger.error("Failed to load move log sessions", it)
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun delete(sessionId: Long): Boolean =
        runCatching { moveLogSessionDao.deleteByIds(listOf(sessionId)) }
            .onFailure { logger.error("Failed to delete move log session", it) }
            .isSuccess

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                MovementLogViewModel(app.userDatabase.moveLogSessionDao())
            }
        }
    }
}
