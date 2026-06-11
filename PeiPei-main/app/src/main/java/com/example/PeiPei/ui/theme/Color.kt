// 文件说明：Material/Compose 主题色板与语义色定义。

package com.example.Lulu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 品牌主色：玫红 / 奶油
val BrandPink = Color(0xFFE0115F)
val BrandPinkStrong = Color(0xFFB00D4C)
val BrandPinkContainer = Color(0xFFFFE4ED)
val BrandPinkSoft = Color(0xFFFFF0F3)

val BrandSecondary = Color(0xFFC53C42)
val BrandSecondaryDeep = Color(0xFFA62F34)
val BrandSecondaryContainer = Color(0xFFFFE7E8)

val BrandCream = Color(0xFFF2F0EA)
val BrandCreamSoft = Color(0xFFFFFBF7)
val BrandCreamDeep = Color(0xFFE4DED3)

// 中性色
val TextPrimary = Color(0xFF2F2730)
val TextSecondary = Color(0xFF7B707A)
val DividerLight = Color(0xFFE7DFE3)
val SurfaceWhite = Color(0xFFFFFFFF)

// 暗色模式
val DarkBackground = Color(0xFF181318)
val DarkSurface = Color(0xFF241E24)
val DarkSurfaceVariant = Color(0xFF3A323A)
val DarkOutline = Color(0xFF5B515B)
val DarkTextPrimary = Color(0xFFF9EEF3)
val DarkTextSecondary = Color(0xFFD8C2CF)

// 品牌渐变
val ThemeButtonGradientStart = BrandPink
val ThemeButtonGradientEnd = BrandPinkStrong
val ThemeButtonGradientDisabledStart = Color(0xFFE8B8CA)
val ThemeButtonGradientDisabledEnd = Color(0xFFECC4D2)

// 常用语义色
val SuccessGreen = Color(0xFF4FA26F)
val ErrorRed = Color(0xFFD95C73)
val LinkBlue = Color(0xFF5E8FA0)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

val LuluLightScheme = lightColorScheme(
    primary = BrandPink,
    onPrimary = White,
    primaryContainer = BrandPinkContainer,
    onPrimaryContainer = TextPrimary,
    secondary = BrandSecondaryDeep,
    onSecondary = White,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = BrandCreamDeep,
    onTertiary = TextPrimary,
    background = BrandCream,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BrandPinkSoft,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = White,
    outline = DividerLight,
    outlineVariant = BrandCreamDeep
)

val LuluDarkScheme = darkColorScheme(
    primary = BrandPink,
    onPrimary = Black,
    primaryContainer = BrandPinkStrong,
    onPrimaryContainer = White,
    secondary = BrandSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = BrandSecondaryDeep,
    onSecondaryContainer = White,
    tertiary = BrandCreamDeep,
    onTertiary = TextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = ErrorRed,
    onError = White,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant
)
