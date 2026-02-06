package com.quezic.domain.recommendation;

import com.quezic.data.remote.LastFmService;
import com.quezic.data.remote.MusicExtractorService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RecommendationEngine_Factory implements Factory<RecommendationEngine> {
  private final Provider<MusicExtractorService> extractorServiceProvider;

  private final Provider<LastFmService> lastFmServiceProvider;

  public RecommendationEngine_Factory(Provider<MusicExtractorService> extractorServiceProvider,
      Provider<LastFmService> lastFmServiceProvider) {
    this.extractorServiceProvider = extractorServiceProvider;
    this.lastFmServiceProvider = lastFmServiceProvider;
  }

  @Override
  public RecommendationEngine get() {
    return newInstance(extractorServiceProvider.get(), lastFmServiceProvider.get());
  }

  public static RecommendationEngine_Factory create(
      Provider<MusicExtractorService> extractorServiceProvider,
      Provider<LastFmService> lastFmServiceProvider) {
    return new RecommendationEngine_Factory(extractorServiceProvider, lastFmServiceProvider);
  }

  public static RecommendationEngine newInstance(MusicExtractorService extractorService,
      LastFmService lastFmService) {
    return new RecommendationEngine(extractorService, lastFmService);
  }
}
