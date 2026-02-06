package com.quezic.di;

import com.quezic.data.local.dao.PlaylistDao;
import com.quezic.data.local.dao.SongDao;
import com.quezic.domain.repository.PlaylistRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvidePlaylistRepositoryFactory implements Factory<PlaylistRepository> {
  private final Provider<PlaylistDao> playlistDaoProvider;

  private final Provider<SongDao> songDaoProvider;

  public AppModule_ProvidePlaylistRepositoryFactory(Provider<PlaylistDao> playlistDaoProvider,
      Provider<SongDao> songDaoProvider) {
    this.playlistDaoProvider = playlistDaoProvider;
    this.songDaoProvider = songDaoProvider;
  }

  @Override
  public PlaylistRepository get() {
    return providePlaylistRepository(playlistDaoProvider.get(), songDaoProvider.get());
  }

  public static AppModule_ProvidePlaylistRepositoryFactory create(
      Provider<PlaylistDao> playlistDaoProvider, Provider<SongDao> songDaoProvider) {
    return new AppModule_ProvidePlaylistRepositoryFactory(playlistDaoProvider, songDaoProvider);
  }

  public static PlaylistRepository providePlaylistRepository(PlaylistDao playlistDao,
      SongDao songDao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePlaylistRepository(playlistDao, songDao));
  }
}
