package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** Jost (SIL OFL 1.1) — sans geométrica inspirada em Futura, a mesma família usada em Backbone. */
val GameFontFamily = FontFamily(
    Font(R.font.jost_light, weight = FontWeight.Light),
    Font(R.font.jost_book, weight = FontWeight.Normal),
    Font(R.font.jost_medium, weight = FontWeight.Medium),
)

private val BaseTypography = Typography()

/**
 * Todo texto do app usa GameFontFamily, inclusive os estilos padrão do Material
 * (antes, textos sem fontFamily explícita caíam na Roboto e destoavam do resto).
 */
val Typography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = GameFontFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = GameFontFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = GameFontFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = GameFontFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = GameFontFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = GameFontFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = GameFontFamily),
    titleMedium = TextStyle(
        fontFamily = GameFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = GameFontFamily),
    bodyLarge = TextStyle(
        fontFamily = GameFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = GameFontFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = GameFontFamily),
    labelLarge = TextStyle(
        fontFamily = GameFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = GameFontFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = GameFontFamily),
)
