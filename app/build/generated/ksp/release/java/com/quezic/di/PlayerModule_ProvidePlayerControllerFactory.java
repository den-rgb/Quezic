package com.quezic.di;

import android.content.Context;
import com.quezic.domain.repository.MusicRepository;
import com.quezic.player.AudioVisualizerHelper;
import com.quezic.player.PlayerController;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class PlayerModule_ProvidePlayerControllerFactory implements Factory<PlayerController> {
  private final Provider<Context> contextProvider;

  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<AudioVisualizerHelper> visualizerHelperProvider;

  public PlayerModule_ProvidePlayerControllerFactory(Provider<Context> contextProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<AudioVisualizerHelper> visualizerHelperProvider) {
    this.contextProvider = contextProvider;
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.visualizerHelperProvider = visualizerHelperProvider;
  }

  @Override
  public PlayerController get() {
    return providePlayerController(contextProvider.get(), musicRepositoryProvider.get(), visualizerHelperProvider.get());
  }

  public static PlayerModule_ProvidePlayerControllerFactory create(
      Provider<Context> contextProvider, Provider<MusicRepository> musicRepositoryProvider,
      Provider<AudioVisualizerHelper> visualizerHelperProvider) {
    return new PlayerModule_ProvidePlayerControllerFactory(contextProvider, musicRepositoryProvider, visualizerHelperProvider);
  }

  public static PlayerController providePlayerController(Context context,
      MusicRepository musicRepository, AudioVisualizerHelper visualizerHelper) {
    return Preconditions.checkNotNullFromProvides(PlayerModule.INSTANCE.providePlayerController(context, musicRepository, visualizerHelper));
  }
}
