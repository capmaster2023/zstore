package com.graceofgod.zstore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Z {
    val Bg = Color(0xFF0E0F12)
    val Surface = Color(0xFF16181D)
    val SurfaceHigh = Color(0xFF1E2128)
    val Line = Color(0xFF2A2E37)
    val Lime = Color(0xFFC6FF3D)
    val OnLime = Color(0xFF0E0F12)
    val Text = Color(0xFFF2F3F5)
    val Muted = Color(0xFF9AA0AC)
    val Red = Color(0xFFFF5C5C)
    val Amber = Color(0xFFFFC145)
    val Blue = Color(0xFF6CB6FF)
}

private val scheme = darkColorScheme(
    primary = Z.Lime,
    onPrimary = Z.OnLime,
    primaryContainer = Color(0xFF2B3A0A),
    onPrimaryContainer = Z.Lime,
    secondary = Z.Blue,
    background = Z.Bg,
    onBackground = Z.Text,
    surface = Z.Bg,
    onSurface = Z.Text,
    surfaceVariant = Z.Surface,
    onSurfaceVariant = Z.Muted,
    surfaceContainer = Z.Surface,
    surfaceContainerLow = Z.Surface,
    surfaceContainerHigh = Z.SurfaceHigh,
    surfaceContainerHighest = Z.SurfaceHigh,
    outline = Z.Line,
    outlineVariant = Z.Line,
    error = Z.Red,
)

private val typography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

val Mono = FontFamily.Monospace

@Composable
fun ZStoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
        ),
        content = content,
    )
}
