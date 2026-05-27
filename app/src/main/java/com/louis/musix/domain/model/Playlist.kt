package com.louis.musix.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int = 0,
)
