package com.quezic.di

import android.content.Context
import com.quezic.domain.repository.MusicRepository
import com.quezic.player.PlayerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun providePlayerController(
        @ApplicationContext context: Context,
        musicRepository: MusicRepository
    ): PlayerController {
        return PlayerController(context, musicRepository)
    }
}
