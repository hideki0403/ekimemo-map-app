package dev.yukineko.ekimemo_map.ui.assistant

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.assistant.AssistantFlowItem
import dev.yukineko.ekimemo_map.data.settings.AssistantSettings
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssistantChooseRectViewModel(
    private val settings: DataStore<AssistantSettings>,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    private val logger = Logger("AssistantChooseRectViewModel")

    suspend fun loadImage(uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching { contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.asImageBitmap() }
            .onFailure { logger.error("Failed to load image", it) }
            .getOrNull()
    }

    suspend fun add(item: AssistantFlowItem): Boolean =
        runCatching { settings.updateData { it.copy(flow = it.flow + item) } }
            .onFailure { logger.error("Failed to save assistant flow", it) }
            .isSuccess

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                AssistantChooseRectViewModel(app.assistantSettings, app.contentResolver)
            }
        }
    }
}
