package com.quezic.di

import com.quezic.data.remote.LastFmService
import com.quezic.data.remote.MusicExtractorService
import com.quezic.domain.recommendation.RecommendationEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecommendationModule {

    @Provides
    @Singleton
    fun provideLastFmService(): LastFmService {
        return LastFmService()
    }

    @Provides
    @Singleton
    fun provideRecommendationEngine(
        extractorService: MusicExtractorService,
        lastFmService: LastFmService
    ): RecommendationEngine {
        return RecommendationEngine(extractorService, lastFmService)
    }
}
