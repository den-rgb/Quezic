package com.quezic.ui.screens.soundcloud;

import com.quezic.data.remote.SoundCloudApiService;
import com.quezic.domain.repository.MusicRepository;
import com.quezic.domain.repository.PlaylistRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ImportSoundCloudViewModel_Factory implements Factory<ImportSoundCloudViewModel> {
  private final Provider<SoundCloudApiService> soundCloudApiServiceProvider;

  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  public ImportSoundCloudViewModel_Factory(
      Provider<SoundCloudApiService> soundCloudApiServiceProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider) {
    this.soundCloudApiServiceProvider = soundCloudApiServiceProvider;
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.playlistRepositoryProvider = playlistRepositoryProvider;
  }

  @Override
  public ImportSoundCloudViewModel get() {
    return newInstance(soundCloudApiServiceProvider.get(), musicRepositoryProvider.get(), playlistRepositoryProvider.get());
  }

  public static ImportSoundCloudViewModel_Factory create(
      Provider<SoundCloudApiService> soundCloudApiServiceProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider) {
    return new ImportSoundCloudViewModel_Factory(soundCloudApiServiceProvider, musicRepositoryProvider, playlistRepositoryProvider);
  }

  public static ImportSoundCloudViewModel newInstance(SoundCloudApiService soundCloudApiService,
      MusicRepository musicRepository, PlaylistRepository playlistRepository) {
    return new ImportSoundCloudViewModel(soundCloudApiService, musicRepository, playlistRepository);
  }
}
