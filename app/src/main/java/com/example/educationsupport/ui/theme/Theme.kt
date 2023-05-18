package com.example.educationsupport.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationsupport.R
import kotlin.reflect.typeOf

private val DarkColorPalette = darkColors(
    primary = Purple300,
    primaryVariant = Purple700,
    secondary = Gray400
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple100,
    secondary = Gray400

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun EducationSupportComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val typography = Typography(
        // fonts for the supporters
        subtitle1 = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        ),

        subtitle2 = TextStyle(
            fontWeight = FontWeight.Normal
        ),

        // fonts for the service users
        body1 = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = 0.25.sp
        ),

        body2 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp
        )
    )

    MaterialTheme(
        colors = colors,
        shapes = Shapes,
        content = content,
        typography = typography
    )
}