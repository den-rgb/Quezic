package com.quezic.data.repository

import com.quezic.data.local.dao.PlaylistDao
import com.quezic.data.local.dao.SongDao
import com.quezic.data.local.entity.PlaylistEntity
import com.quezic.data.local.entity.PlaylistSongCrossRef
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.Song
import com.quezic.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                val songCount = playlistDao.getPlaylistSongCount(entity.id)
                val totalDuration = playlistDao.getPlaylistTotalDuration(entity.id) ?: 0L
                entity.toDomain(songCount, totalDuration)
            }
        }
    }

    override fun getPlaylistById(id: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistByIdFlow(id).map { entity ->
            entity?.let {
                val songCount = playlistDao.getPlaylistSongCount(it.id)
                val totalDuration = playlistDao.getPlaylistTotalDuration(it.id) ?: 0L
                it.toDomain(songCount, totalDuration)
            }
        }
    }

    override fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createPlaylist(name: String, description: String?): Long {
        val entity = PlaylistEntity(
            name = name,
            description = description,
            coverUrl = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSmartPlaylist = false,
            smartPlaylistType = null
        )
        return playlistDao.insertPlaylist(entity)
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(PlaylistEntity.fromDomain(playlist))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        val maxPosition = playlistDao.getMaxPosition(playlistId) ?: -1
        val crossRef = PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = songId,
            position = maxPosition + 1
        )
        playlistDao.insertPlaylistSong(crossRef)
        playlistDao.updatePlaylistTimestamp(playlistId)
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistDao.removeFromPlaylist(playlistId, songId)
        playlistDao.updatePlaylistTimestamp(playlistId)
    }

    override suspend fun reorderSongs(playlistId: Long, songIds: List<String>) {
        songIds.forEachIndexed { index, songId ->
            playlistDao.updateSongPosition(playlistId, songId, index)
        }
        playlistDao.updatePlaylistTimestamp(playlistId)
    }

    override suspend fun isSongInPlaylist(playlistId: Long, songId: String): Boolean {
        return playlistDao.isSongInPlaylist(playlistId, songId)
    }
    
    override suspend fun getPlaylistsContainingSong(songId: String): List<Long> {
        return playlistDao.getPlaylistsContainingSong(songId)
    }

    override fun getPlaylistCount(): Flow<Int> {
        return playlistDao.getPlaylistCount()
    }
}
