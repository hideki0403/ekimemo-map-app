package dev.yukineko.ekimemo_map.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.lerp
import androidx.datastore.core.DataStore
import dev.yukineko.ekimemo_map.data.settings.AssistantSettings
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class AssistantFlowRunner(private val settings: DataStore<AssistantSettings>) {
    private val logger = Logger("AssistantFlowRunner")

    suspend fun run() {
        val service = AssistantService.instance.value
            ?: return logger.debug("AssistantService is not connected, skipping")
        val settings = settings.data.first()
        if (!settings.enabled) return logger.debug("AssistantFlow is disabled, skipping")

        val target = packageNamePattern(settings.debugPackageName)
        for (item in settings.flow) {
            logger.debug("Next: $item")
            when (item) {
                is AssistantFlowItem.Tap -> service.tapIn(target, item.position)
                is AssistantFlowItem.TapRect -> service.tapIn(target, item.rect.let {
                    Offset(lerp(it.left, it.right, Random.nextFloat()), lerp(it.top, it.bottom, Random.nextFloat()))
                })
                is AssistantFlowItem.Wait -> delay(item.millis)
                is AssistantFlowItem.WaitRandom -> delay((0..item.maxMillis).random())
            }
        }
        logger.debug("Done")
    }

    private suspend fun AssistantService.tapIn(target: Regex, position: Offset) {
        val packageName = activePackageName
        if (!target.matches(packageName.orEmpty())) {
            return logger.debug("Active app $packageName is not the target, skipping tap")
        }
        if (!tap(position)) logger.error("Failed to tap at $position")
    }
}

internal fun packageNamePattern(packageName: String): Regex = packageName
    .filter { it.isLetterOrDigit() || it in "._*" }
    .replace(".", "\\.")
    .replace("*", ".*")
    .toRegex()
