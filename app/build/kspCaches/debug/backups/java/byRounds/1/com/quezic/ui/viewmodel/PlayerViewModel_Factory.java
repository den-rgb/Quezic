package com.quezic.ui.viewmodel;

import com.quezic.domain.repository.MusicRepository;
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
public final class PlayerViewModel_Factory implements Factory<PlayerViewModel> {
  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<PlayerController> playerControllerProvider;

  public PlayerViewModel_Factory(Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlayerController> playerControllerProvider) {
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.playerControllerProvider = playerControllerProvider;
  }

  @Override
  public PlayerViewModel get() {
    return newInstance(musicRepositoryProvider.get(), playerControllerProvider.get());
  }

  public static PlayerViewModel_Factory create(Provider<MusicRepository> musicRepositoryProvider,
      Provider<PlayerController> playerControllerProvider) {
    return new PlayerViewModel_Factory(musicRepositoryProvider, playerControllerProvider);
  }

  public static PlayerViewModel newInstance(MusicRepository musicRepository,
      PlayerController playerController) {
    return new PlayerViewModel(musicRepository, playerController);
  }
}
