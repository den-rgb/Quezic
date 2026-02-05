package com.quezic.domain.model

data class SearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val sourceType: SourceType,
    val sourceId: String,
    val sourceUrl: String? = null
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        sourceType = sourceType,
        sourceId = sourceId,
        sourceUrl = sourceUrl
    )
}
