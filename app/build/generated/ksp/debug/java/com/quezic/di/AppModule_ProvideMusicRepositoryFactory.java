package com.quezic.di;

import android.content.Context;
import com.quezic.data.local.dao.SongDao;
import com.quezic.data.remote.MusicExtractorService;
import com.quezic.domain.repository.MusicRepository;
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
public final class AppModule_ProvideMusicRepositoryFactory implements Factory<MusicRepository> {
  private final Provider<SongDao> songDaoProvider;

  private final Provider<MusicExtractorService> extractorServiceProvider;

  private final Provider<Context> contextProvider;

  public AppModule_ProvideMusicRepositoryFactory(Provider<SongDao> songDaoProvider,
      Provider<MusicExtractorService> extractorServiceProvider, Provider<Context> contextProvider) {
    this.songDaoProvider = songDaoProvider;
    this.extractorServiceProvider = extractorServiceProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public MusicRepository get() {
    return provideMusicRepository(songDaoProvider.get(), extractorServiceProvider.get(), contextProvider.get());
  }

  public static AppModule_ProvideMusicRepositoryFactory create(Provider<SongDao> songDaoProvider,
      Provider<MusicExtractorService> extractorServiceProvider, Provider<Context> contextProvider) {
    return new AppModule_ProvideMusicRepositoryFactory(songDaoProvider, extractorServiceProvider, contextProvider);
  }

  public static MusicRepository provideMusicRepository(SongDao songDao,
      MusicExtractorService extractorService, Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMusicRepository(songDao, extractorService, context));
  }
}
