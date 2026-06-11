// 文件说明：全屏「适应屏幕」图片的双指缩放、拖拽平移（无双击放大）。

package com.example.Lulu.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.abs

private const val ZoomThreshold = 1.02f

@Composable
fun ZoomableFitAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxScale: Float = 5f,
    /**
     * 为 true 时表示放在 `HorizontalPager` 内：
     * - 不注册 [detectTapGestures]（避免单指被 tap 检测长期占用）
     * - 使用自定义变换检测，避免标准 [detectTransformGestures] 在单指平移过 slop 后 consume 导致无法横向翻页
     */
    embedInHorizontalPager: Boolean = false,
    onClickWhenNotZoomed: (() -> Unit)? = null,
    onZoomedChange: ((zoomed: Boolean) -> Unit)? = null,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun panBounds(containerW: Float, containerH: Float): Pair<Float, Float> {
        val maxX = (containerW * (scale - 1f) * 0.5f).coerceAtLeast(0f)
        val maxY = (containerH * (scale - 1f) * 0.5f).coerceAtLeast(0f)
        return maxX to maxY
    }

    fun clampOffset(containerW: Float, containerH: Float) {
        val (maxX, maxY) = panBounds(containerW, containerH)
        offset = Offset(
            offset.x.coerceIn(-maxX, maxX),
            offset.y.coerceIn(-maxY, maxY),
        )
    }

    fun notifyZoomed() {
        onZoomedChange?.invoke(scale > ZoomThreshold)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cw = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val ch = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        LaunchedEffect(cw, ch) {
            clampOffset(cw, ch)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .then(
                    if (embedInHorizontalPager) {
                        Modifier
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (scale <= ZoomThreshold) {
                                        onClickWhenNotZoomed?.invoke()
                                    }
                                },
                            )
                        }
                    },
                )
                .pointerInput(embedInHorizontalPager) {
                    fun applyTransformStep(panChange: Offset, zoomChange: Float) {
                        scale = (scale * zoomChange).coerceIn(1f, maxScale)
                        if (scale > 1f) {
                            offset += panChange
                            clampOffset(cw, ch)
                        } else {
                            offset = Offset.Zero
                        }
                        notifyZoomed()
                    }

                    if (embedInHorizontalPager) {
                        // 与 HorizontalPager 并存：标准 detectTransformGestures 在「单指平移」超过 touchSlop 后就会
                        // consume，横向滑会被抢走。嵌入 Pager 时仅在缩放/旋转越过 slop 后才进入消费；捏合仍可放大。
                        awaitEachGesture {
                            var rotation = 0f
                            var zoom = 1f
                            var pan = Offset.Zero
                            var pastTouchSlop = false
                            val touchSlop = viewConfiguration.touchSlop
                            var lockedToPanZoom = false

                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.fastAny { it.isConsumed }
                                if (!canceled) {
                                    val zoomChange = event.calculateZoom()
                                    val rotationChange = event.calculateRotation()
                                    val panChange = event.calculatePan()

                                    if (!pastTouchSlop) {
                                        zoom *= zoomChange
                                        rotation += rotationChange
                                        pan += panChange

                                        val centroidSize =
                                            event.calculateCentroidSize(useCurrent = false)
                                        val zoomMotion = abs(1f - zoom) * centroidSize
                                        val rotationMotion =
                                            abs(rotation * PI.toFloat() * centroidSize / 180f)

                                        if (zoomMotion > touchSlop || rotationMotion > touchSlop) {
                                            pastTouchSlop = true
                                            lockedToPanZoom = rotationMotion < touchSlop
                                        }
                                    }

                                    if (pastTouchSlop) {
                                        val effectiveRotation =
                                            if (lockedToPanZoom) 0f else rotationChange
                                        if (effectiveRotation != 0f ||
                                            zoomChange != 1f ||
                                            panChange != Offset.Zero
                                        ) {
                                            applyTransformStep(panChange, zoomChange)
                                        }
                                        event.changes.fastForEach {
                                            if (it.positionChanged()) {
                                                it.consume()
                                            }
                                        }
                                    }
                                }
                            } while (!canceled && event.changes.fastAny { it.pressed })
                        }
                    } else {
                        detectTransformGestures(panZoomLock = true) { _, panChange, zoomChange, _ ->
                            applyTransformStep(panChange, zoomChange)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    },
            )
        }
    }
}
