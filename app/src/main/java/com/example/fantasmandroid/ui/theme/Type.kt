@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.example.fantasmandroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.R

/** Tall, narrow display face used for the FANTASM wordmark and screen titles. */
val FantasmDisplay = FontFamily(
    Font(
        resId = R.font.oswald,
        weight = FontWeight.Light,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    )
)

/** Handwritten marker face used for the ITC phrase banner. */
val FantasmHand = FontFamily(
    Font(
        resId = R.font.caveat,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

val FantasmTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FantasmDisplay,
        fontWeight = FontWeight.Light,
        fontSize = 22.sp,
        letterSpacing = 2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp
    )
)
