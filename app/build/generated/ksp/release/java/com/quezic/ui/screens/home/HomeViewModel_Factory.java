package com.quezic.ui.screens.home;

import com.quezic.domain.repository.MusicRepository;
import com.quezic.domain.repository.PlaylistRepository;
import com.quezic.player.PlayerController;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  private final Provider<PlayerController> playerControllerProvider;

  public HomeViewModel_Factory(Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<PlayerController> playerControllerProvider) {
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.playlistRepositoryProvider = playlistRepositoryProvider;
    this.playerControllerProvider = playerControllerProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(musicRepositoryProvider.get(), playlistRepositoryProvider.get(), playerControllerProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<PlayerController> playerControllerProvider) {
    return new HomeViewModel_Factory(musicRepositoryProvider, playlistRepositoryProvider, playerControllerProvider);
  }

  public static HomeViewModel newInstance(MusicRepository musicRepository,
      PlaylistRepository playlistRepository, PlayerController playerController) {
    return new HomeViewModel(musicRepository, playlistRepository, playerController);
  }
}
