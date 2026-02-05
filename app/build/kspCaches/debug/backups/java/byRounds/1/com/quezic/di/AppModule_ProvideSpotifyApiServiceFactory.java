package com.quezic.di;

import com.quezic.data.remote.SpotifyApiService;
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
public final class AppModule_ProvideSpotifyApiServiceFactory implements Factory<SpotifyApiService> {
  @Override
  public SpotifyApiService get() {
    return provideSpotifyApiService();
  }

  public static AppModule_ProvideSpotifyApiServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SpotifyApiService provideSpotifyApiService() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSpotifyApiService());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideSpotifyApiServiceFactory INSTANCE = new AppModule_ProvideSpotifyApiServiceFactory();
  }
}
