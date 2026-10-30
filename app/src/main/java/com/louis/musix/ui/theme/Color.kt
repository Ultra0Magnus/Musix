package com.louis.musix.ui.theme

import androidx.compose.ui.graphics.Color

// Palette « Sillon » — sang de bœuf, crème, moutarde. Vinyle chaud, jamais noir pur.
val SillonBackground = Color(0xFF2A1512) // fond sang de bœuf
val SillonSurface    = Color(0xFF3A1F1A) // pilules, cartes, barre de navigation
val SillonSurface2   = Color(0xFF4A2A22) // boutons ronds, lignes internes des feuilles
val SillonInk        = Color(0xFFF5E6D3) // crème : texte, mini-lecteur
val SillonMuted      = Color(0xFFC9AE96) // métadonnées
val SillonLine       = Color(0xFF6B4A3E) // contours, filets, artiste sur crème
val SillonDim        = Color(0xFFA88B74) // paroles hors ligne courante
val SillonDanger     = Color(0xFFF2A08A) // erreurs, suppression

// Accent moutarde — piste en cours, bouton play, onglet actif, anneaux de progression.
val SillonAccent     = Color(0xFFE9A727)
val SillonOnAccent   = Color(0xFF2A1512)

// Sillons du disque (alternance de deux noirs chauds)
val SillonGrooveDark  = Color(0xFF170A08)
val SillonGrooveLight = Color(0xFF22100D)

// Étiquettes de disque — couleur stable dérivée de l'identifiant du morceau.
val SillonLabels = listOf(
    Color(0xFFE9A727),
    Color(0xFFD9643A),
    Color(0xFF7FA38F),
    Color(0xFFE3C9A8),
    Color(0xFF8FB3C9),
    Color(0xFFB07AA1),
)

/** Couleur d'étiquette déterministe pour une clé (id de morceau, nom de playlist…). */
fun sillonLabelFor(key: Any?): Color =
    SillonLabels[Math.floorMod(key.hashCode(), SillonLabels.size)]
