package com.louis.musix.player

/** Mode de répétition de la file d'attente. */
enum class RepeatMode {
    /** Lecture normale — s'arrête en fin de file. */
    OFF,
    /** Répète le morceau courant indéfiniment. */
    ONE,
    /** Répète toute la file en boucle. */
    ALL,
}
