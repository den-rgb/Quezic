package com.quezic.data.local.dao

import androidx.room.*
import com.quezic.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    
    @Query("SELECT * FROM songs ORDER BY addedAt DESC")
    fun getAllSongs(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?
    
    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongByIdFlow(id: String): Flow<SongEntity?>
    
    @Query("SELECT * FROM songs WHERE localPath IS NOT NULL ORDER BY addedAt DESC")
    fun getDownloadedSongs(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 50): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int = 50): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs ORDER BY addedAt DESC LIMIT :limit")
    fun getRecentlyAddedSongs(limit: Int = 50): Flow<List<SongEntity>>
    
    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>
    
    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>
    
    @Query("SELECT COUNT(*) FROM songs WHERE artist = :artist")
    suspend fun getSongCountByArtist(artist: String): Int
    
    @Query("SELECT DISTINCT album FROM songs WHERE album IS NOT NULL ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>
    
    @Query("SELECT * FROM songs WHERE album = :album ORDER BY title")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)
    
    @Update
    suspend fun updateSong(song: SongEntity)
    
    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFavorite: Boolean)
    
    @Query("UPDATE songs SET localPath = :localPath WHERE id = :songId")
    suspend fun updateLocalPath(songId: String, localPath: String?)
    
    @Query("UPDATE songs SET sourceType = :sourceType, sourceId = :sourceId WHERE id = :songId")
    suspend fun updateSource(songId: String, sourceType: String, sourceId: String)
    
    @Delete
    suspend fun deleteSong(song: SongEntity)
    
    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)
    
    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>
    
    @Query("SELECT COUNT(DISTINCT artist) FROM songs")
    fun getArtistCount(): Flow<Int>
    
    @Query("SELECT COUNT(DISTINCT album) FROM songs WHERE album IS NOT NULL")
    fun getAlbumCount(): Flow<Int>
}
