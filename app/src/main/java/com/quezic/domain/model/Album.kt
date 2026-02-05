package com.quezic.domain.model

data class Album(
    val name: String,
    val artist: String,
    val coverUrl: String? = null,
    val songCount: Int = 0,
    val year: Int? = null
)
