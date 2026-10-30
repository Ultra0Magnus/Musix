package com.louis.musix.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Direction « Sillon » : tout est rond. Cartes à 28 dp, feuilles et dialogues
 * très arrondis ; les pilules utilisent directement CircleShape / RoundedCornerShape(50).
 */
val MusixShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small      = RoundedCornerShape(18.dp),
    medium     = RoundedCornerShape(24.dp),
    large      = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Pilule pleine (rayon 50 %). */
val PillShape = RoundedCornerShape(percent = 50)

/** Carte standard Sillon. */
val CardShape = RoundedCornerShape(28.dp)
