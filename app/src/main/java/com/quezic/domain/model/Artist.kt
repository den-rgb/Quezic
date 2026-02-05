package com.quezic.domain.model

data class Artist(
    val name: String,
    val imageUrl: String? = null,
    val songCount: Int = 0,
    val albumCount: Int = 0
)
