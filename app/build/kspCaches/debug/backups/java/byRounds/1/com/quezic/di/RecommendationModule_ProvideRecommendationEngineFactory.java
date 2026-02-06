package com.quezic.di;

import com.quezic.data.remote.LastFmService;
import com.quezic.data.remote.MusicExtractorService;
import com.quezic.domain.recommendation.RecommendationEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class RecommendationModule_ProvideRecommendationEngineFactory implements Factory<RecommendationEngine> {
  private final Provider<MusicExtractorService> extractorServiceProvider;

  private final Provider<LastFmService> lastFmServiceProvider;

  public RecommendationModule_ProvideRecommendationEngineFactory(
      Provider<MusicExtractorService> extractorServiceProvider,
      Provider<LastFmService> lastFmServiceProvider) {
    this.extractorServiceProvider = extractorServiceProvider;
    this.lastFmServiceProvider = lastFmServiceProvider;
  }

  @Override
  public RecommendationEngine get() {
    return provideRecommendationEngine(extractorServiceProvider.get(), lastFmServiceProvider.get());
  }

  public static RecommendationModule_ProvideRecommendationEngineFactory create(
      Provider<MusicExtractorService> extractorServiceProvider,
      Provider<LastFmService> lastFmServiceProvider) {
    return new RecommendationModule_ProvideRecommendationEngineFactory(extractorServiceProvider, lastFmServiceProvider);
  }

  public static RecommendationEngine provideRecommendationEngine(
      MusicExtractorService extractorService, LastFmService lastFmService) {
    return Preconditions.checkNotNullFromProvides(RecommendationModule.INSTANCE.provideRecommendationEngine(extractorService, lastFmService));
  }
}
