package com.quezic.ui.screens.library;

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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  private final Provider<DownloadManager> downloadManagerProvider;

  public LibraryViewModel_Factory(Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<DownloadManager> downloadManagerProvider) {
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.playlistRepositoryProvider = playlistRepositoryProvider;
    this.downloadManagerProvider = downloadManagerProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(musicRepositoryProvider.get(), playlistRepositoryProvider.get(), downloadManagerProvider.get());
  }

  public static LibraryViewModel_Factory create(Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<DownloadManager> downloadManagerProvider) {
    return new LibraryViewModel_Factory(musicRepositoryProvider, playlistRepositoryProvider, downloadManagerProvider);
  }

  public static LibraryViewModel newInstance(MusicRepository musicRepository,
      PlaylistRepository playlistRepository, DownloadManager downloadManager) {
    return new LibraryViewModel(musicRepository, playlistRepository, downloadManager);
  }
}
