package com.quezic.data.repository;

import com.quezic.data.local.dao.PlaylistDao;
import com.quezic.data.local.dao.SongDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PlaylistRepositoryImpl_Factory implements Factory<PlaylistRepositoryImpl> {
  private final Provider<PlaylistDao> playlistDaoProvider;

  private final Provider<SongDao> songDaoProvider;

  public PlaylistRepositoryImpl_Factory(Provider<PlaylistDao> playlistDaoProvider,
      Provider<SongDao> songDaoProvider) {
    this.playlistDaoProvider = playlistDaoProvider;
    this.songDaoProvider = songDaoProvider;
  }

  @Override
  public PlaylistRepositoryImpl get() {
    return newInstance(playlistDaoProvider.get(), songDaoProvider.get());
  }

  public static PlaylistRepositoryImpl_Factory create(Provider<PlaylistDao> playlistDaoProvider,
      Provider<SongDao> songDaoProvider) {
    return new PlaylistRepositoryImpl_Factory(playlistDaoProvider, songDaoProvider);
  }

  public static PlaylistRepositoryImpl newInstance(PlaylistDao playlistDao, SongDao songDao) {
    return new PlaylistRepositoryImpl(playlistDao, songDao);
  }
}
