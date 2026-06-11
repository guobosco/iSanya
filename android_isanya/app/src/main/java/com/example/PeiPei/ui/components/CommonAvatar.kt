// 文件说明：通用头像组件（加载、占位、点击等）。

package com.example.Lulu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.Lulu.data.remote.RetrofitClient

@Composable
fun CommonAvatar(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            val painter = rememberAsyncImagePainter(
                model = RetrofitClient.normalizeBackendMediaUrlForDisplay(imageUrl)
            )
            val placeholderGray = MaterialTheme.colorScheme.onSurfaceVariant
            val showLetterPlaceholder = painter.state !is AsyncImagePainter.State.Success
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (showLetterPlaceholder) {
                    Text(
                        text = avatarFallbackText(name),
                        color = placeholderGray,
                        style = textStyle,
                        textAlign = TextAlign.Center
                    )
                }
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Text(
                text = avatarFallbackText(name),
                color = contentColor,
                style = textStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun avatarFallbackText(name: String): String {
    val trimmedName = name.trim()
    return trimmedName.firstOrNull()?.toString() ?: "?"
}
