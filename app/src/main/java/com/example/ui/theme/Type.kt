package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

val OrbitronFontFamily = CairoFontFamily
val RajdhaniFontFamily = CairoFontFamily

val CyberTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 1.5.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    displayMedium = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 1.2.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    displaySmall = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 1.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    headlineMedium = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 0.8.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    titleLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 0.8.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    titleMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 0.5.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    bodyLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
<<<<<<< HEAD
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
=======
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.4.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    bodyMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
<<<<<<< HEAD
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
=======
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    labelLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 0.8.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    ),
    labelSmall = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
<<<<<<< HEAD
        letterSpacing = 0.sp
=======
        letterSpacing = 0.5.sp
>>>>>>> 9460234707315938c64498f8c919e664a97d06b9
    )
)
