package com.quezic.data.repository;

import android.content.Context;
import com.quezic.data.local.dao.SongDao;
import com.quezic.data.remote.MusicExtractorService;
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
public final class MusicRepositoryImpl_Factory implements Factory<MusicRepositoryImpl> {
  private final Provider<SongDao> songDaoProvider;

  private final Provider<MusicExtractorService> extractorServiceProvider;

  private final Provider<Context> contextProvider;

  public MusicRepositoryImpl_Factory(Provider<SongDao> songDaoProvider,
      Provider<MusicExtractorService> extractorServiceProvider, Provider<Context> contextProvider) {
    this.songDaoProvider = songDaoProvider;
    this.extractorServiceProvider = extractorServiceProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public MusicRepositoryImpl get() {
    return newInstance(songDaoProvider.get(), extractorServiceProvider.get(), contextProvider.get());
  }

  public static MusicRepositoryImpl_Factory create(Provider<SongDao> songDaoProvider,
      Provider<MusicExtractorService> extractorServiceProvider, Provider<Context> contextProvider) {
    return new MusicRepositoryImpl_Factory(songDaoProvider, extractorServiceProvider, contextProvider);
  }

  public static MusicRepositoryImpl newInstance(SongDao songDao,
      MusicExtractorService extractorService, Context context) {
    return new MusicRepositoryImpl(songDao, extractorService, context);
  }
}
