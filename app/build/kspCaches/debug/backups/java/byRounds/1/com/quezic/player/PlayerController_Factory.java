package com.quezic.player;

import android.content.Context;
import com.quezic.domain.repository.MusicRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PlayerController_Factory implements Factory<PlayerController> {
  private final Provider<Context> contextProvider;

  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<AudioVisualizerHelper> visualizerHelperProvider;

  public PlayerController_Factory(Provider<Context> contextProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<AudioVisualizerHelper> visualizerHelperProvider) {
    this.contextProvider = contextProvider;
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.visualizerHelperProvider = visualizerHelperProvider;
  }

  @Override
  public PlayerController get() {
    return newInstance(contextProvider.get(), musicRepositoryProvider.get(), visualizerHelperProvider.get());
  }

  public static PlayerController_Factory create(Provider<Context> contextProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<AudioVisualizerHelper> visualizerHelperProvider) {
    return new PlayerController_Factory(contextProvider, musicRepositoryProvider, visualizerHelperProvider);
  }

  public static PlayerController newInstance(Context context, MusicRepository musicRepository,
      AudioVisualizerHelper visualizerHelper) {
    return new PlayerController(context, musicRepository, visualizerHelper);
  }
}
