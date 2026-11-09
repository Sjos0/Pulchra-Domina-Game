package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** Pixelify Sans (Google Fonts, OFL) — UI pixel legível em português. */
val PixelFontFamily = FontFamily(
    Font(R.font.pixelify_sans_regular, weight = FontWeight.Normal)
)

/** Press Start 2P (Google Fonts, OFL) — títulos curtos estilo arcade. */
val ArcadeFontFamily = FontFamily(
    Font(R.font.press_start_2p, weight = FontWeight.Normal)
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
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            ),
    )
