package com.quezic.di;

import com.quezic.data.remote.LastFmService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class RecommendationModule_ProvideLastFmServiceFactory implements Factory<LastFmService> {
  @Override
  public LastFmService get() {
    return provideLastFmService();
  }

  public static RecommendationModule_ProvideLastFmServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LastFmService provideLastFmService() {
    return Preconditions.checkNotNullFromProvides(RecommendationModule.INSTANCE.provideLastFmService());
  }

  private static final class InstanceHolder {
    private static final RecommendationModule_ProvideLastFmServiceFactory INSTANCE = new RecommendationModule_ProvideLastFmServiceFactory();
  }
}
