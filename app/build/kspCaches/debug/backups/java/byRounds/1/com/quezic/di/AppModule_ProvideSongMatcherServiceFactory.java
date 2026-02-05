package com.quezic.di;

import com.quezic.data.remote.MusicExtractorService;
import com.quezic.domain.service.SongMatcherService;
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
public final class AppModule_ProvideSongMatcherServiceFactory implements Factory<SongMatcherService> {
  private final Provider<MusicExtractorService> musicExtractorServiceProvider;

  public AppModule_ProvideSongMatcherServiceFactory(
      Provider<MusicExtractorService> musicExtractorServiceProvider) {
    this.musicExtractorServiceProvider = musicExtractorServiceProvider;
  }

  @Override
  public SongMatcherService get() {
    return provideSongMatcherService(musicExtractorServiceProvider.get());
  }

  public static AppModule_ProvideSongMatcherServiceFactory create(
      Provider<MusicExtractorService> musicExtractorServiceProvider) {
    return new AppModule_ProvideSongMatcherServiceFactory(musicExtractorServiceProvider);
  }

  public static SongMatcherService provideSongMatcherService(
      MusicExtractorService musicExtractorService) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSongMatcherService(musicExtractorService));
  }
}
