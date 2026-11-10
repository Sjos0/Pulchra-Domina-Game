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

val Typography =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = GameFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = GameFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            ),
    )
