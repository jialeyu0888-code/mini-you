package com.miniyou.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Rose = Color(0xFFE8798F)
val RoseDeep = Color(0xFFC95C74)
val Peach = Color(0xFFF4A88C)
val Lavender = Color(0xFF9C8BD9)
val Mint = Color(0xFF6CC3A7)
val Cream = Color(0xFFFFF8F6)
val Blush = Color(0xFFFCE4E8)
val Ink = Color(0xFF4A3439)
val WarnRed = Color(0xFFD9534F)
val Amber = Color(0xFFE6A23C)

private val LightColors = lightColorScheme(
    primary = Rose,
    onPrimary = Color.White,
    primaryContainer = Blush,
    onPrimaryContainer = Color(0xFF5E1F2E),
    secondary = Peach,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6DA),
    onSecondaryContainer = Color(0xFF5A2A16),
    tertiary = Lavender,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECE7FB),
    onTertiaryContainer = Color(0xFF2E2361),
    background = Color(0xFFFCF0E8),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF8ECEE),
    onSurfaceVariant = Color(0xFF7A6266),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF5F4),
    surfaceContainer = Color(0xFFFCEFF0),
    surfaceContainerHigh = Color(0xFFF9E9EB),
    surfaceContainerHighest = Color(0xFFF5E2E5),
    outline = Color(0xFFD9C2C6),
    outlineVariant = Color(0xFFEFDDE0),
    error = WarnRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2A3B3),
    onPrimary = Color(0xFF4A1524),
    primaryContainer = Color(0xFF6B2C3C),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFF7C1A8),
    tertiary = Color(0xFFC9BCF5),
    background = Color(0xFF1E1719),
    onBackground = Color(0xFFF1DEE1),
    surface = Color(0xFF261D20),
    onSurface = Color(0xFFF1DEE1),
    surfaceVariant = Color(0xFF3A2D31),
    onSurfaceVariant = Color(0xFFD8C1C5),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val AppTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
)

val HeroGradient = Brush.linearGradient(listOf(Color(0xFFF7A1B2), Color(0xFFF4B69C)))
val SoftGradient = Brush.linearGradient(listOf(Color(0xFFFDE7EB), Color(0xFFFFF0E6)))
val AiGradient = Brush.linearGradient(listOf(Color(0xFFB9A6F2), Color(0xFFF2A3B8)))

@Composable
fun MiniYouTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
