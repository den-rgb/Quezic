package com.quezic.ui.screens.spotify;

import com.quezic.data.remote.SpotifyApiService;
import com.quezic.domain.repository.MusicRepository;
import com.quezic.domain.repository.PlaylistRepository;
import com.quezic.domain.service.SongMatcherService;
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
public final class ImportSpotifyViewModel_Factory implements Factory<ImportSpotifyViewModel> {
  private final Provider<SpotifyApiService> spotifyApiServiceProvider;

  private final Provider<SongMatcherService> songMatcherServiceProvider;

  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  public ImportSpotifyViewModel_Factory(Provider<SpotifyApiService> spotifyApiServiceProvider,
      Provider<SongMatcherService> songMatcherServiceProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider) {
    this.spotifyApiServiceProvider = spotifyApiServiceProvider;
    this.songMatcherServiceProvider = songMatcherServiceProvider;
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.playlistRepositoryProvider = playlistRepositoryProvider;
  }

  @Override
  public ImportSpotifyViewModel get() {
    return newInstance(spotifyApiServiceProvider.get(), songMatcherServiceProvider.get(), musicRepositoryProvider.get(), playlistRepositoryProvider.get());
  }

  public static ImportSpotifyViewModel_Factory create(
      Provider<SpotifyApiService> spotifyApiServiceProvider,
      Provider<SongMatcherService> songMatcherServiceProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider) {
    return new ImportSpotifyViewModel_Factory(spotifyApiServiceProvider, songMatcherServiceProvider, musicRepositoryProvider, playlistRepositoryProvider);
  }

  public static ImportSpotifyViewModel newInstance(SpotifyApiService spotifyApiService,
      SongMatcherService songMatcherService, MusicRepository musicRepository,
      PlaylistRepository playlistRepository) {
    return new ImportSpotifyViewModel(spotifyApiService, songMatcherService, musicRepository, playlistRepository);
  }
}
