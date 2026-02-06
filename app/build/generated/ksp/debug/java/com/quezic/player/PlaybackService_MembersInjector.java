package com.quezic.player;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PlaybackService_MembersInjector implements MembersInjector<PlaybackService> {
  private final Provider<PlayerController> playerControllerProvider;

  public PlaybackService_MembersInjector(Provider<PlayerController> playerControllerProvider) {
    this.playerControllerProvider = playerControllerProvider;
  }

  public static MembersInjector<PlaybackService> create(
      Provider<PlayerController> playerControllerProvider) {
    return new PlaybackService_MembersInjector(playerControllerProvider);
  }

  @Override
  public void injectMembers(PlaybackService instance) {
    injectPlayerController(instance, playerControllerProvider.get());
  }

  @InjectedFieldSignature("com.quezic.player.PlaybackService.playerController")
  public static void injectPlayerController(PlaybackService instance,
      PlayerController playerController) {
    instance.playerController = playerController;
  }
}
