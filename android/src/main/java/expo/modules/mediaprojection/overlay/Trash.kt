package expo.modules.mediaprojection.overlay

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectDragGestures

fun calcTimerIsHoverTrash(
    overlayState: OverlayState,
    timerSizePx: Float,
    trashRect: Rect
): Boolean {
    val timerCenterX = overlayState.timerOffset.x + (timerSizePx / 2)
    val timerCenterY = overlayState.timerOffset.y + (timerSizePx / 2)
    return !(timerCenterX < trashRect.left ||
            timerCenterX > trashRect.right ||
            timerCenterY < trashRect.top ||
            timerCenterY > trashRect.bottom)
}

@Composable
fun Trash(
    overlayState: OverlayState,
    buttonRadiusDp: Int,
    trashSizeDp: Int,
) {
    val context = LocalContext.current
    val overlayButtonSizePx = remember {
        val density = context.resources.displayMetrics.density
        buttonRadiusDp * density
    }
    var trashRect by remember { mutableStateOf(Rect.Zero) }
    val isTimerDragHoveringTrash = remember {
        derivedStateOf {
            calcTimerIsHoverTrash(overlayState, overlayButtonSizePx, trashRect)
        }
    }
    val iconTint by remember {
        derivedStateOf {
            if (isTimerDragHoveringTrash.value) {
                Color.Red
            } else { Color.Black }
        }
    }

    Box(
        Modifier
            .size(trashSizeDp.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = .5f))
            .onGloballyPositioned {
                trashRect = it.boundsInRoot()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Delete, "trash", modifier = Modifier
                .size(34.dp), tint = iconTint
        )
    }

    LaunchedEffect(isTimerDragHoveringTrash) {
        snapshotFlow {
            isTimerDragHoveringTrash.value
        }.collect {
            overlayState.isTimerHoverTrash = it
        }
    }
}

@Composable
fun TrashContentScreen(
    showOverlayButton: Boolean,
    serviceState: ServiceState,
    buttonRadiusDp: Int,
    trashSizeDp: Int,
) {

    Box(Modifier.onGloballyPositioned {
        serviceState.screenWidthPx = it.size.width
        serviceState.screenHeightPx = it.size.height
    }) {
        // future.txt correct z-order
        if (showOverlayButton) {
            ShowTrash(
                serviceState = serviceState,
                buttonRadiusDp = buttonRadiusDp,
                trashSizeDp = trashSizeDp
            )
        }
    }
}

@Composable
fun ShowTrash(serviceState: ServiceState, buttonRadiusDp: Int, trashSizeDp: Int) {
    val overlayState = serviceState.overlayButtonState

    if (overlayState.showTrash) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Trash(overlayState, buttonRadiusDp = buttonRadiusDp, trashSizeDp = trashSizeDp)
        }
    }
}

@Composable
fun ClickTarget(
    serviceState: ServiceState,
    controller: OverlayDraggableViewController,
    overlayState: OverlayState,
    viewHolder: OverlayViewHolder,
    onDropOnTrash: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        overlayState.showTrash = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dragAmountIntOffset =
                            IntOffset(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())

                        val timerOffset = overlayState.timerOffset + dragAmountIntOffset

                        var x = max(timerOffset.x, 0)

                        x = min(x, serviceState.screenWidthPx - controller.buttonRadiusPx)

                        var y = max(timerOffset.y, 0)

                        y = min(y, serviceState.screenHeightPx - controller.buttonRadiusPx)

                        // this is the good code
                        overlayState.timerOffset = IntOffset(x, y)

                        viewHolder.updateViewPos(
                            x = overlayState.timerOffset.x,
                            y = overlayState.timerOffset.y,
                        )
                    },
                    onDragEnd = {
                        overlayState.showTrash = false

                        if (overlayState.isTimerHoverTrash) {
                            onDropOnTrash()
                            return@detectDragGestures
                        }
                                },
                )
            },
    ) {
        content()
    }
}

