package com.quezic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quezic.data.local.dao.PlaylistDao
import com.quezic.data.local.dao.SongDao
import com.quezic.data.local.entity.PlaylistEntity
import com.quezic.data.local.entity.PlaylistSongCrossRef
import com.quezic.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
abstract class QuezicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}
