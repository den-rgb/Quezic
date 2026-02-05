package com.quezic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.SmartPlaylistType

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isSmartPlaylist: Boolean,
    val smartPlaylistType: String?
) {
    fun toDomain(songCount: Int = 0, totalDuration: Long = 0): Playlist = Playlist(
        id = id,
        name = name,
        description = description,
        coverUrl = coverUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        songCount = songCount,
        totalDuration = totalDuration,
        isSmartPlaylist = isSmartPlaylist,
        smartPlaylistType = smartPlaylistType?.let { SmartPlaylistType.valueOf(it) }
    )

    companion object {
        fun fromDomain(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            coverUrl = playlist.coverUrl,
            createdAt = playlist.createdAt,
            updatedAt = playlist.updatedAt,
            isSmartPlaylist = playlist.isSmartPlaylist,
            smartPlaylistType = playlist.smartPlaylistType?.name
        )
    }
}
