package com.example.simplecustomlauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * ドラッグ中のアイテム情報
 */
data class DragInfo(
    val shortcutId: String,
    val fromRow: Int,
    val fromColumn: Int
)

/**
 * ドロップ先の情報
 */
data class DropTarget(
    val row: Int,
    val column: Int
)

/**
 * ドラッグ可能なアイテムをラップするComposable
 */
@Composable
fun DraggableItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .zIndex(if (isDragging) 1f else 0f)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDragging = true
                                onDragStart()
                            },
                            onDragEnd = {
                                isDragging = false
                                offsetX = 0f
                                offsetY = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                isDragging = false
                                offsetX = 0f
                                offsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                onDrag(Offset(offsetX, offsetY))
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        content()
    }
}
