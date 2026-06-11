// 文件说明：加载中、空状态等通用指示 UI 组件。

package com.example.Lulu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FeiLingLoadingDots(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "loadingDots")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing)
        ),
        label = "loadingRotation"
    )
    val corePulse = transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingCorePulse"
    )
    val textHaloPulse = transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingHalo"
    )
    val loaderSize = dotSize * 4.2f
    val orbitRadius = loaderSize * 0.34f
    val orbitRadiusPx = with(LocalDensity.current) { orbitRadius.toPx() }

    Box(
        modifier = modifier.size(loaderSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(corePulse.value)
                .alpha(textHaloPulse.value)
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(loaderSize * 0.42f)
                .scale(corePulse.value)
                .background(color.copy(alpha = 0.16f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize * 1.05f)
                .background(color, CircleShape)
        )
        repeat(3) { index ->
            val orbitPulse = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 960
                        0f at 0
                        1f at 240 using FastOutSlowInEasing
                        0f at 480 using FastOutSlowInEasing
                        0f at 960
                    },
                    initialStartOffset = StartOffset(index * 120)
                ),
                label = "loadingOrbit$index"
            )
            val angle = Math.toRadians((rotation.value + index * 120f).toDouble())

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        translationX = (cos(angle) * orbitRadiusPx).toFloat()
                        translationY = (sin(angle) * orbitRadiusPx).toFloat()
                    }
                    .scale(0.82f + orbitPulse.value * 0.45f)
                    .alpha(0.42f + orbitPulse.value * 0.58f)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun FeiLingLoadingLabel(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
    dotColor: Color = textColor,
    dotSize: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "loadingLabel")
    val textAlpha = transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingTextAlpha"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeiLingLoadingDots(
            color = dotColor,
            dotSize = dotSize
        )
        Text(
            text = text,
            modifier = Modifier.alpha(textAlpha.value),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FeiLingLoadingCard(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .defaultMinSize(minWidth = 164.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeiLingLoadingDots(
                color = contentColor,
                dotSize = 10.dp
            )
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "请稍候",
                color = contentColor.copy(alpha = 0.64f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun FeiLingTopSyncIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 1.8.dp,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    )
                }
            }
        }
    }
}

@Composable
fun FeiLingPullRefreshHintIndicator(
    progress: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val easedProgress = FastOutSlowInEasing.transform(clamped)
    val targetAlpha = if (visible) (0.15f + easedProgress * 0.85f).coerceIn(0f, 1f) else 0f
    val targetOffset = if (visible) lerp((-34).dp, 0.dp, easedProgress) else (-34).dp
    val targetScale = if (visible) (0.86f + (1f - 0.86f) * easedProgress) else 0.86f
    val animatedAlpha = androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 100),
        label = "pullHintAlpha"
    ).value
    val animatedOffsetY = androidx.compose.animation.core.animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 100),
        label = "pullHintOffset"
    ).value
    val animatedScale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 100),
        label = "pullHintScale"
    ).value

    Box(
        modifier = modifier
            .offset(y = animatedOffsetY)
            .alpha(animatedAlpha)
            .scale(animatedScale),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "下拉刷新",
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (0.35f + easedProgress * 0.65f).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
