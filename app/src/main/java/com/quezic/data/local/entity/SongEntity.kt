package com.quezic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quezic.domain.model.Song
import com.quezic.domain.model.SourceType

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val thumbnailUrl: String?,
    val localPath: String?,
    val sourceType: String,
    val sourceId: String,
    val sourceUrl: String?,
    val genre: String?,
    val addedAt: Long,
    val playCount: Int,
    val lastPlayedAt: Long?,
    val isFavorite: Boolean
) {
    fun toDomain(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        localPath = localPath,
        sourceType = SourceType.valueOf(sourceType),
        sourceId = sourceId,
        sourceUrl = sourceUrl,
        genre = genre,
        addedAt = addedAt,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            thumbnailUrl = song.thumbnailUrl,
            localPath = song.localPath,
            sourceType = song.sourceType.name,
            sourceId = song.sourceId,
            sourceUrl = song.sourceUrl,
            genre = song.genre,
            addedAt = song.addedAt,
            playCount = song.playCount,
            lastPlayedAt = song.lastPlayedAt,
            isFavorite = song.isFavorite
        )
    }
}
