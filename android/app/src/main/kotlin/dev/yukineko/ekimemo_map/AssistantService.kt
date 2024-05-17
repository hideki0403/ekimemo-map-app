package dev.yukineko.ekimemo_map

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent


class AssistantService : AccessibilityService() {
    companion object {
        var instance: AssistantService? = null
            private set
    }

    private val TAG = "AssistantService"
    private var foregroundPackageName: String? = null
    private var targetDebugPackageName: Regex? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val packageName = event.packageName.toString()
            if (packageName.startsWith("com.android")) return
            foregroundPackageName = packageName
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun setDebugPackageName(packageName: String) {
        val regex = packageName
            .filter { it.isLetter() || it.isDigit() || it == '.' || it == '*' }
            .lowercase()
            .replace(".", "\\.")
            .replace("*", ".*")

        targetDebugPackageName = Regex("^$regex$")
    }

    fun performTap(x: Float, y: Float): Boolean {
        Log.d(TAG, "Performing tap at $x, $y, foreground app: $foregroundPackageName, target app: $targetDebugPackageName")
        if (targetDebugPackageName?.matches(foregroundPackageName ?: "") != true) {
            Log.d(TAG, "Not in target app, skipping tap")
            return false
        }
        val path = Path()
        path.moveTo(x, y)
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, 90 + (Math.random() * 20).toLong())
        val gestureDescription = GestureDescription.Builder().addStroke(strokeDescription).build()
        val result = dispatchGesture(gestureDescription, null, null)
        if (!result) {
            Log.e(TAG, "Failed to perform tap")
        }
        return result
    }
}