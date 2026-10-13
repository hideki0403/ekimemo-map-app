package dev.yukineko.ekimemo_map.ui.assistant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.assistant.AssistantFlowItem
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AssistantChooseRectScreen(
    point: Boolean,
    onBack: () -> Unit,
    viewModel: AssistantChooseRectViewModel = viewModel(factory = AssistantChooseRectViewModel.Factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val image by produceState<ImageBitmap?>(null, uri) { value = uri?.let { viewModel.loadImage(it) } }
    var selectedPoint by remember(image) { mutableStateOf<Offset?>(null) }
    var selectedRect by remember(image) { mutableStateOf<Rect?>(null) }
    val item = if (point) selectedPoint?.let(AssistantFlowItem::Tap) else selectedRect?.let(AssistantFlowItem::TapRect)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) uri = picked
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_assistant_choose_rect)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(
                        onClick = {
                            val selected = item ?: return@IconButton
                            scope.launch {
                                if (viewModel.add(selected)) {
                                    onBack()
                                } else {
                                    snackbar.showSnackbar(context.getString(R.string.assistant_flow_save_failed))
                                }
                            }
                        },
                        enabled = item != null,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.ok))
                    }
                },
                colors = appBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) {
                Icon(Icons.Rounded.Image, contentDescription = stringResource(R.string.assistant_choose_rect_select_image))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
            val current = image
            when {
                current == null -> Text(stringResource(R.string.assistant_choose_rect_select_image))
                point -> PointSelector(current, selectedPoint, onPointChange = { selectedPoint = it })
                else -> RectSelector(current, selectedRect, onRectChange = { selectedRect = it })
            }
        }
    }
}

@Composable
private fun PointSelector(image: ImageBitmap, point: Offset?, onPointChange: (Offset) -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    val bounds = image.bounds

    SelectionImage(
        image = image,
        onGesture = { scale ->
            val down = awaitFirstDown()
            onPointChange((down.position / scale).coerceIn(bounds))
            drag(down.id) { change ->
                change.consume()
                onPointChange((change.position / scale).coerceIn(bounds))
            }
        },
        onDraw = { scale ->
            val shown = (point ?: return@SelectionImage) * scale
            drawCircle(color, 16.dp.toPx(), shown, style = Stroke(2.dp.toPx()))
            drawCircle(color, 3.dp.toPx(), shown)
        },
    )
}

@Composable
private fun RectSelector(image: ImageBitmap, rect: Rect?, onRectChange: (Rect) -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    val bounds = image.bounds

    SelectionImage(
        image = image,
        onGesture = { scale ->
            val down = awaitFirstDown()
            val start = down.position / scale
            val corner = rect?.cornerAt(start, TOUCH_TOLERANCE.toPx() / scale)
            val anchor = corner?.let { rect.oppositeCorner(it) } ?: start
            val grabbed = corner ?: start
            val update = { change: PointerInputChange ->
                change.consume()
                onRectChange(rectOf(anchor, (grabbed + (change.position - down.position) / scale).coerceIn(bounds)))
            }
            val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> update(change) } ?: return@SelectionImage
            drag(slop.id, update)
        },
        onDraw = { scale ->
            val shown = (rect ?: return@SelectionImage).let { Rect(it.topLeft * scale, it.bottomRight * scale) }
            clipRect(shown.left, shown.top, shown.right, shown.bottom, ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.5f))
            }
            drawRect(color, shown.topLeft, shown.size, style = Stroke(2.dp.toPx()))
            shown.corners.forEach { drawCircle(color, 8.dp.toPx(), it) }
        },
    )
}

@Composable
private fun SelectionImage(
    image: ImageBitmap,
    onGesture: suspend AwaitPointerEventScope.(scale: Float) -> Unit,
    onDraw: DrawScope.(scale: Float) -> Unit,
) {
    val currentOnGesture by rememberUpdatedState(onGesture)

    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier
            .aspectRatio(image.width.toFloat() / image.height)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .pointerInput(image) { awaitEachGesture { currentOnGesture(size.width / image.width.toFloat()) } }
            .drawWithContent {
                drawContent()
                onDraw(size.width / image.width)
            },
    )
}

private val ImageBitmap.bounds get() = Size(width.toFloat(), height.toFloat())

private val TOUCH_TOLERANCE = 24.dp
