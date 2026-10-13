package dev.yukineko.ekimemo_map.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

@Serializable
sealed interface AssistantFlowItem {
    @Serializable
    @SerialName("tap")
    data class Tap(
        @SerialName("content") @Serializable(OffsetSerializer::class) val position: Offset,
    ) : AssistantFlowItem

    @Serializable
    @SerialName("tapRect")
    data class TapRect(
        @SerialName("content") @Serializable(RectSerializer::class) val rect: Rect,
    ) : AssistantFlowItem

    @Serializable
    @SerialName("wait")
    data class Wait(@SerialName("content") val millis: Long) : AssistantFlowItem

    @Serializable
    @SerialName("waitRandom")
    data class WaitRandom(@SerialName("content") val maxMillis: Long) : AssistantFlowItem
}

fun encodeAssistantFlow(flow: List<AssistantFlowItem>): String =
    Base64.encode(Json.encodeToString(flow).encodeToByteArray())

fun decodeAssistantFlow(code: String): List<AssistantFlowItem>? = runCatching {
    Json.decodeFromString<List<AssistantFlowItem>>(Base64.decode(code.trim()).decodeToString())
}.getOrNull()

private abstract class CommaSeparatedSerializer<T>(
    name: String,
    private val toValues: (T) -> List<Float>,
    private val fromValues: (List<Float>) -> T,
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) =
        encoder.encodeString(toValues(value).joinToString(","))

    override fun deserialize(decoder: Decoder): T =
        fromValues(decoder.decodeString().split(",").map(String::toFloat))
}

private object OffsetSerializer : CommaSeparatedSerializer<Offset>(
    "AssistantFlowOffset",
    { listOf(it.x, it.y) },
    { Offset(it[0], it[1]) },
)

private object RectSerializer : CommaSeparatedSerializer<Rect>(
    "AssistantFlowRect",
    { listOf(it.left, it.top, it.right, it.bottom) },
    { Rect(it[0], it[1], it[2], it[3]) },
)
