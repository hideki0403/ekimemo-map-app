package dev.yukineko.ekimemo_map.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantFlowTest {
    private val flow = listOf(
        AssistantFlowItem.Tap(Offset(540f, 1200.5f)),
        AssistantFlowItem.TapRect(Rect(100f, 200f, 300f, 400f)),
        AssistantFlowItem.Wait(500),
        AssistantFlowItem.WaitRandom(1000),
    )

    @Test
    fun decodesFlutterFormat() {
        val text = """[
            {"type":"tap","content":"540.0,1200.5"},
            {"type":"tapRect","content":"100.0,200.0,300.0,400.0"},
            {"type":"wait","content":500},
            {"type":"waitRandom","content":1000}
        ]"""
        assertEquals(flow, Json.decodeFromString<List<AssistantFlowItem>>(text))
    }

    @Test
    fun roundTrip() {
        assertEquals(flow, Json.decodeFromString<List<AssistantFlowItem>>(Json.encodeToString(flow)))
    }

    @Test
    fun packageNamePatternSupportsWildcard() {
        val pattern = packageNamePattern("com.example.*")
        assertTrue(pattern.matches("com.example.app"))
        assertFalse(pattern.matches("com.exampleapp"))
        assertFalse(pattern.matches("org.com.example.app"))
    }

    @Test
    fun packageNamePatternKeepsUnderscore() {
        assertTrue(packageNamePattern("dev.yukineko.ekimemo_map").matches("dev.yukineko.ekimemo_map"))
    }

    @Test
    fun packageNamePatternIsCaseSensitive() {
        val pattern = packageNamePattern("com.Example.app")
        assertTrue(pattern.matches("com.Example.app"))
        assertFalse(pattern.matches("com.example.app"))
    }
}
