package dev.yukineko.ekimemo_map.ui.assistant

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.assistant.AssistantFlowItem
import dev.yukineko.ekimemo_map.data.settings.AssistantSettings
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlowEntry(val key: Int, val item: AssistantFlowItem)

class AssistantFlowViewModel(private val settings: DataStore<AssistantSettings>) : ViewModel() {
    private val logger = Logger("AssistantFlowViewModel")
    private var nextKey = 0

    private val _entries = MutableStateFlow<List<FlowEntry>?>(null)
    val entries: StateFlow<List<FlowEntry>?> = _entries.asStateFlow()

    init {
        viewModelScope.launch {
            settings.data.collect { data ->
                if (data.flow != _entries.value?.map { it.item }) _entries.value = data.flow.map(::newEntry)
            }
        }
    }

    fun move(from: Int, to: Int) = _entries.update { it?.toMutableList()?.apply { add(to, removeAt(from)) } }

    suspend fun commit(): Boolean = save { it }

    suspend fun add(item: AssistantFlowItem): Boolean = save { it + newEntry(item) }

    suspend fun remove(entry: FlowEntry): Boolean = save { it - entry }

    suspend fun replace(flow: List<AssistantFlowItem>): Boolean = save { flow.map(::newEntry) }

    private fun newEntry(item: AssistantFlowItem) = FlowEntry(nextKey++, item)

    private suspend fun save(transform: (List<FlowEntry>) -> List<FlowEntry>): Boolean {
        val entries = transform(_entries.value.orEmpty())
        _entries.value = entries
        return runCatching { settings.updateData { it.copy(flow = entries.map(FlowEntry::item)) } }
            .onFailure { logger.error("Failed to save assistant flow", it) }
            .isSuccess
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                AssistantFlowViewModel(app.assistantSettings)
            }
        }
    }
}
