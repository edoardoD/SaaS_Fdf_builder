package manutenzioni.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Stitch MCP Generated Palette
val PrimaryBlue = Color(0xFF3366FF)
val PrimaryBlueHover = Color(0xFF004BE3)
val SlateBackground = Color(0xFFF8FAFC)
val SlateBorder = Color(0xFFE2E8F0)
val TextPrimary = Color(0xFF191C1E)
val TextSecondary = Color(0xFF64748B)
val TextTertiary = Color(0xFF1E293B)
val LightSurface = Color(0xFFFFFFFF)

// Material Colors mapped to the Stitch theme
val ManutenzioniColors = Colors(
    primary = PrimaryBlue,
    primaryVariant = PrimaryBlueHover,
    secondary = TextSecondary,
    secondaryVariant = TextTertiary,
    background = SlateBackground,
    surface = LightSurface,
    error = Color(0xFFBA1A1A),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Color.White,
    isLight = true
)

// Stitch Typography (Inter)
val InterFontFamily = FontFamily.Default // In a real app we'd load Inter.ttf here

val ManutenzioniTypography = Typography(
    h5 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.01).sp
    ),
    h6 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    body1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.05.sp
    )
)

// Stitch Shapes (ROUND_EIGHT -> 8px)
val ManutenzioniShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun ManutenzioniTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = ManutenzioniColors,
        typography = ManutenzioniTypography,
        shapes = ManutenzioniShapes,
        content = content
    )
}
