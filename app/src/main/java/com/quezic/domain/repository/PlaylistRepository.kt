package com.quezic.domain.repository

import com.quezic.domain.model.Playlist
import com.quezic.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistById(id: Long): Flow<Playlist?>
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>
    
    suspend fun createPlaylist(name: String, description: String? = null): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlistId: Long)
    
    suspend fun addSongToPlaylist(playlistId: Long, songId: String)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)
    suspend fun reorderSongs(playlistId: Long, songIds: List<String>)
    suspend fun isSongInPlaylist(playlistId: Long, songId: String): Boolean
    suspend fun getPlaylistsContainingSong(songId: String): List<Long>
    
    fun getPlaylistCount(): Flow<Int>
}
