package dev.yukineko.ekimemo_map.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.geometry.Offset
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.random.Random

class AssistantService : AccessibilityService() {
    private val logger = Logger("AssistantService")

    val activePackageName: String?
        get() = rootInActiveWindow?.packageName?.toString()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance.value = this
        logger.debug("Service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _instance.value = null
        return super.onUnbind(intent)
    }

    suspend fun tap(position: Offset): Boolean = suspendCancellableCoroutine { continuation ->
        val path = Path().apply { moveTo(position.x, position.y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, Random.nextLong(90, 110))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) = continuation.resume(true)
            override fun onCancelled(gestureDescription: GestureDescription) = continuation.resume(false)
        }
        if (!dispatchGesture(gesture, callback, null)) continuation.resume(false)
    }

    companion object {
        private val _instance = MutableStateFlow<AssistantService?>(null)
        val instance: StateFlow<AssistantService?> = _instance.asStateFlow()
    }
}
