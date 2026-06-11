// 文件说明：小型开关样式的 Compose 控件。

package com.example.Lulu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MiniSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color = Color.Gray,
    thumbColor: Color = Color.White
) {
    val switchWidth = 36.dp
    val switchHeight = 18.dp
    val thumbSize = 14.dp
    val padding = 2.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) switchWidth - thumbSize - padding else padding,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (enabled) {
            if (checked) checkedTrackColor else uncheckedTrackColor
        } else {
            (if (checked) checkedTrackColor else uncheckedTrackColor).copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )

    Box(
        modifier = modifier
            .size(width = switchWidth, height = switchHeight)
            .clip(RoundedCornerShape(switchHeight / 2))
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
