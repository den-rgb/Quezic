package com.quezic.domain.recommendation;

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

  public RecommendationEngine_Factory(Provider<MusicExtractorService> extractorServiceProvider) {
    this.extractorServiceProvider = extractorServiceProvider;
  }

  @Override
  public RecommendationEngine get() {
    return newInstance(extractorServiceProvider.get());
  }

  public static RecommendationEngine_Factory create(
      Provider<MusicExtractorService> extractorServiceProvider) {
    return new RecommendationEngine_Factory(extractorServiceProvider);
  }

  public static RecommendationEngine newInstance(MusicExtractorService extractorService) {
    return new RecommendationEngine(extractorService);
  }
}
