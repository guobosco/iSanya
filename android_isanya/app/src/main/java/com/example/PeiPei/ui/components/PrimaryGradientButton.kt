// 文件说明：主按钮样式（渐变、圆角等）的可复用组件。

package com.example.Lulu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import com.example.Lulu.ui.theme.ThemeButtonGradientDisabledEnd
import com.example.Lulu.ui.theme.ThemeButtonGradientDisabledStart
import com.example.Lulu.ui.theme.ThemeButtonGradientEnd
import com.example.Lulu.ui.theme.ThemeButtonGradientStart

@Composable
fun PrimaryGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = if (enabled) {
            listOf(ThemeButtonGradientStart, ThemeButtonGradientEnd)
        } else {
            listOf(ThemeButtonGradientDisabledStart, ThemeButtonGradientDisabledEnd)
        }
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.8f)
        ),
        modifier = modifier
            .clip(shape)
            .background(brush = gradient, shape = shape)
    ) {
        content()
    }
}
