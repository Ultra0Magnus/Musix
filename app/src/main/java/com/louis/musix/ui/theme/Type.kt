package com.louis.musix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.louis.musix.R

// Anton — grotesque condensé ultra-gras, poids unique. Titres d'écran héros + titre de piste.
val Anton = FontFamily(Font(R.font.anton_regular, FontWeight.Normal))

// Barlow Condensed — industriel condensé. Tout le reste : sections, morceaux, corps, métadonnées.
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/**
 * Échelle typographique « Affiche ».
 * - Anton : display* + headlineLarge (titres géants, titre de piste).
 * - Barlow Condensed : headline/title/body/label (sections, morceaux, corps, méta).
 * NB : la mise en majuscules (méta, labels, onglets) se fait au point d'appel — Compose
 * n'expose pas de textTransform sur TextStyle.
 */
val MusixTypography = Typography(
    // ── Héros Anton ──────────────────────────────────────────────────────────
    displayLarge  = TextStyle(fontFamily = Anton, fontWeight = FontWeight.Normal, fontSize = 56.sp, lineHeight = 52.sp, letterSpacing = 0.4.sp),
    displayMedium = TextStyle(fontFamily = Anton, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 42.sp, letterSpacing = 0.4.sp),
    displaySmall  = TextStyle(fontFamily = Anton, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 34.sp, letterSpacing = 0.3.sp),
    headlineLarge = TextStyle(fontFamily = Anton, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 38.sp, letterSpacing = 0.3.sp),

    // ── Barlow Condensed ─────────────────────────────────────────────────────
    headlineMedium = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp),
    headlineSmall  = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge     = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
    titleMedium    = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    titleSmall     = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    bodyLarge      = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
    labelMedium    = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
    labelSmall     = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.sp),
)
