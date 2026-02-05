package com.quezic.di

import android.content.Context
import com.quezic.data.local.ProxySettings
import com.quezic.data.local.dao.PlaylistDao
import com.quezic.data.local.dao.SongDao
import com.quezic.data.remote.InvidiousApiService
import com.quezic.data.remote.MusicExtractorService
import com.quezic.data.remote.PipedApiService
import com.quezic.data.remote.SpotifyApiService
import com.quezic.domain.service.SongMatcherService
import com.quezic.data.repository.MusicRepositoryImpl
import com.quezic.data.repository.PlaylistRepositoryImpl
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideProxySettings(
        @ApplicationContext context: Context
    ): ProxySettings {
        return ProxySettings(context)
    }

    @Provides
    @Singleton
    fun providePipedApiService(): PipedApiService {
        return PipedApiService()
    }

    @Provides
    @Singleton
    fun provideInvidiousApiService(): InvidiousApiService {
        return InvidiousApiService()
    }

    @Provides
    @Singleton
    fun provideMusicExtractorService(
        pipedApiService: PipedApiService,
        invidiousApiService: InvidiousApiService,
        proxySettings: ProxySettings
    ): MusicExtractorService {
        return MusicExtractorService(pipedApiService, invidiousApiService, proxySettings)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        songDao: SongDao,
        extractorService: MusicExtractorService,
        @ApplicationContext context: Context
    ): MusicRepository {
        return MusicRepositoryImpl(songDao, extractorService, context)
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        playlistDao: PlaylistDao,
        songDao: SongDao
    ): PlaylistRepository {
        return PlaylistRepositoryImpl(playlistDao, songDao)
    }

    @Provides
    @Singleton
    fun provideSpotifyApiService(): SpotifyApiService {
        return SpotifyApiService()
    }

    @Provides
    @Singleton
    fun provideSongMatcherService(
        musicExtractorService: MusicExtractorService
    ): SongMatcherService {
        return SongMatcherService(musicExtractorService)
    }
}
