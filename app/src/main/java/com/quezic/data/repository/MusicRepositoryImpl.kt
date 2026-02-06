package com.quezic.data.repository

import android.content.Context
import com.quezic.data.local.dao.SongDao
import com.quezic.data.local.entity.SongEntity
import com.quezic.data.remote.MusicExtractorService
import com.quezic.domain.model.*
import com.quezic.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val extractorService: MusicExtractorService,
    @ApplicationContext private val context: Context
) : MusicRepository {

    override fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDownloadedSongs(): Flow<List<Song>> {
        return songDao.getDownloadedSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteSongs(): Flow<List<Song>> {
        return songDao.getFavoriteSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMostPlayedSongs(limit: Int): Flow<List<Song>> {
        return songDao.getMostPlayedSongs(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentlyPlayedSongs(limit: Int): Flow<List<Song>> {
        return songDao.getRecentlyPlayedSongs(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentlyAddedSongs(limit: Int): Flow<List<Song>> {
        return songDao.getRecentlyAddedSongs(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSongById(id: String): Song? {
        return songDao.getSongById(id)?.toDomain()
    }

    override suspend fun insertSong(song: Song) {
        songDao.insertSong(SongEntity.fromDomain(song))
    }

    override suspend fun deleteSong(song: Song) {
        // Delete local file if exists
        song.localPath?.let { path ->
            File(path).delete()
        }
        songDao.deleteSong(SongEntity.fromDomain(song))
    }

    override suspend fun setFavorite(songId: String, isFavorite: Boolean) {
        songDao.setFavorite(songId, isFavorite)
    }

    override suspend fun incrementPlayCount(songId: String) {
        songDao.incrementPlayCount(songId)
    }

    override fun getAllArtists(): Flow<List<Artist>> {
        return songDao.getAllArtists().map { artistNames ->
            artistNames.map { name ->
                val songCount = songDao.getSongCountByArtist(name)
                Artist(
                    name = name,
                    songCount = songCount
                )
            }
        }
    }

    override fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artist).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAlbums(): Flow<List<Album>> {
        return songDao.getAllAlbums().map { albumNames ->
            albumNames.mapNotNull { name ->
                // Get first song to get album info
                Album(
                    name = name,
                    artist = "Various Artists", // TODO: Get actual artist
                    songCount = 1
                )
            }
        }
    }

    override fun getSongsByAlbum(album: String): Flow<List<Song>> {
        return songDao.getSongsByAlbum(album).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchLocalSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchOnline(query: String, sources: List<SourceType>): List<SearchResult> {
        return extractorService.search(query, sources)
    }

    override suspend fun getStreamUrl(song: Song): String? {
        // If downloaded, return local path
        song.localPath?.let { return it }
        
        // Try primary source first
        val primaryUrl = extractorService.getStreamUrl(
            sourceType = song.sourceType,
            sourceId = song.sourceId
        )
        
        if (primaryUrl != null) return primaryUrl
        
        // Primary failed — try fallback source (SoundCloud→YouTube or YouTube→SoundCloud)
        android.util.Log.d("MusicRepository", "Primary source failed for '${song.title}', trying fallback...")
        val fallback = extractorService.getDownloadUrlFromFallbackSource(
            originalSourceType = song.sourceType,
            songTitle = song.title,
            songArtist = song.artist
        )
        
        if (fallback != null) {
            android.util.Log.d("MusicRepository", "Fallback found via ${fallback.newSourceType}")
            // Update the song's source in DB so future plays use the working source
            songDao.updateSource(song.id, fallback.newSourceType.name, fallback.newSourceId)
            return fallback.streamUrl
        }
        
        return null
    }

    override suspend fun downloadSong(song: Song, quality: StreamQuality): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Get stream URL
                val streamUrl = extractorService.getStreamUrl(
                    sourceType = song.sourceType,
                    sourceId = song.sourceId,
                    quality = quality
                ) ?: return@withContext Result.failure(Exception("Could not get stream URL"))

                // Create downloads directory
                val downloadsDir = File(context.getExternalFilesDir(null), "music")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                // Create file name from song info
                val fileName = "${song.artist} - ${song.title}.mp3"
                    .replace(Regex("[^a-zA-Z0-9\\s\\-.]"), "")
                    .take(200)
                val file = File(downloadsDir, fileName)

                // In production, download the actual file
                // For now, create a placeholder
                file.createNewFile()
                file.writeText("Mock audio file for: ${song.title}")

                Result.success(file.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateLocalPath(songId: String, localPath: String?) {
        songDao.updateLocalPath(songId, localPath)
    }

    override suspend fun deleteDownload(song: Song) {
        withContext(Dispatchers.IO) {
            // Delete the local file if it exists
            song.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Clear the localPath in the database
            songDao.updateLocalPath(song.id, null)
        }
    }

    override fun getSongCount(): Flow<Int> {
        return songDao.getSongCount()
    }

    override fun getArtistCount(): Flow<Int> {
        return songDao.getArtistCount()
    }

    override fun getAlbumCount(): Flow<Int> {
        return songDao.getAlbumCount()
    }
}
