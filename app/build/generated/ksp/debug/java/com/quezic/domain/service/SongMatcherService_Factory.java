package com.quezic.domain.service;

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
public final class SongMatcherService_Factory implements Factory<SongMatcherService> {
  private final Provider<MusicExtractorService> musicExtractorServiceProvider;

  public SongMatcherService_Factory(Provider<MusicExtractorService> musicExtractorServiceProvider) {
    this.musicExtractorServiceProvider = musicExtractorServiceProvider;
  }

  @Override
  public SongMatcherService get() {
    return newInstance(musicExtractorServiceProvider.get());
  }

  public static SongMatcherService_Factory create(
      Provider<MusicExtractorService> musicExtractorServiceProvider) {
    return new SongMatcherService_Factory(musicExtractorServiceProvider);
  }

  public static SongMatcherService newInstance(MusicExtractorService musicExtractorService) {
    return new SongMatcherService(musicExtractorService);
  }
}
