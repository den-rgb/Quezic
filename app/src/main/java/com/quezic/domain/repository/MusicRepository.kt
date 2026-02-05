package com.quezic.domain.repository

import com.quezic.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    // Local library
    fun getAllSongs(): Flow<List<Song>>
    fun getDownloadedSongs(): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    fun getMostPlayedSongs(limit: Int = 50): Flow<List<Song>>
    fun getRecentlyPlayedSongs(limit: Int = 50): Flow<List<Song>>
    fun getRecentlyAddedSongs(limit: Int = 50): Flow<List<Song>>
    
    suspend fun getSongById(id: String): Song?
    suspend fun insertSong(song: Song)
    suspend fun deleteSong(song: Song)
    suspend fun setFavorite(songId: String, isFavorite: Boolean)
    suspend fun incrementPlayCount(songId: String)
    
    // Artists and Albums
    fun getAllArtists(): Flow<List<Artist>>
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    fun getAllAlbums(): Flow<List<Album>>
    fun getSongsByAlbum(album: String): Flow<List<Song>>
    
    // Search
    fun searchLocalSongs(query: String): Flow<List<Song>>
    suspend fun searchOnline(query: String, sources: List<SourceType>): List<SearchResult>
    
    // Stream URL
    suspend fun getStreamUrl(song: Song): String?
    
    // Download
    suspend fun downloadSong(song: Song, quality: StreamQuality): Result<String>
    suspend fun updateLocalPath(songId: String, localPath: String?)
    suspend fun deleteDownload(song: Song)
    
    // Stats
    fun getSongCount(): Flow<Int>
    fun getArtistCount(): Flow<Int>
    fun getAlbumCount(): Flow<Int>
}
