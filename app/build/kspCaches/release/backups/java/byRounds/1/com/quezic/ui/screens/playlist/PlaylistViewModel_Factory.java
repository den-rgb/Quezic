package com.quezic.ui.screens.playlist;

import com.quezic.domain.recommendation.RecommendationEngine;
import com.quezic.domain.repository.MusicRepository;
import com.quezic.domain.repository.PlaylistRepository;
import com.quezic.download.DownloadManager;
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
public final class PlaylistViewModel_Factory implements Factory<PlaylistViewModel> {
  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<RecommendationEngine> recommendationEngineProvider;

  private final Provider<DownloadManager> downloadManagerProvider;

  public PlaylistViewModel_Factory(Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<RecommendationEngine> recommendationEngineProvider,
      Provider<DownloadManager> downloadManagerProvider) {
    this.playlistRepositoryProvider = playlistRepositoryProvider;
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.recommendationEngineProvider = recommendationEngineProvider;
    this.downloadManagerProvider = downloadManagerProvider;
  }

  @Override
  public PlaylistViewModel get() {
    return newInstance(playlistRepositoryProvider.get(), musicRepositoryProvider.get(), recommendationEngineProvider.get(), downloadManagerProvider.get());
  }

  public static PlaylistViewModel_Factory create(
      Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<RecommendationEngine> recommendationEngineProvider,
      Provider<DownloadManager> downloadManagerProvider) {
    return new PlaylistViewModel_Factory(playlistRepositoryProvider, musicRepositoryProvider, recommendationEngineProvider, downloadManagerProvider);
  }

  public static PlaylistViewModel newInstance(PlaylistRepository playlistRepository,
      MusicRepository musicRepository, RecommendationEngine recommendationEngine,
      DownloadManager downloadManager) {
    return new PlaylistViewModel(playlistRepository, musicRepository, recommendationEngine, downloadManager);
  }
}
