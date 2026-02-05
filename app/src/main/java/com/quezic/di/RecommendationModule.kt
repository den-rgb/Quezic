package com.quezic.di

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
    fun provideRecommendationEngine(
        extractorService: MusicExtractorService
    ): RecommendationEngine {
        return RecommendationEngine(extractorService)
    }
}
