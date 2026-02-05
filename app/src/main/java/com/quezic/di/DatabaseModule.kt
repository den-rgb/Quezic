package com.quezic.di

import android.content.Context
import androidx.room.Room
import com.quezic.data.local.QuezicDatabase
import com.quezic.data.local.dao.PlaylistDao
import com.quezic.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): QuezicDatabase {
        return Room.databaseBuilder(
            context,
            QuezicDatabase::class.java,
            "quezic_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: QuezicDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: QuezicDatabase): PlaylistDao {
        return database.playlistDao()
    }
}
