package com.quezic.di;

import com.quezic.data.local.ProxySettings;
import com.quezic.data.remote.InvidiousApiService;
import com.quezic.data.remote.MusicExtractorService;
import com.quezic.data.remote.PipedApiService;
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
public final class AppModule_ProvideMusicExtractorServiceFactory implements Factory<MusicExtractorService> {
  private final Provider<PipedApiService> pipedApiServiceProvider;

  private final Provider<InvidiousApiService> invidiousApiServiceProvider;

  private final Provider<ProxySettings> proxySettingsProvider;

  public AppModule_ProvideMusicExtractorServiceFactory(
      Provider<PipedApiService> pipedApiServiceProvider,
      Provider<InvidiousApiService> invidiousApiServiceProvider,
      Provider<ProxySettings> proxySettingsProvider) {
    this.pipedApiServiceProvider = pipedApiServiceProvider;
    this.invidiousApiServiceProvider = invidiousApiServiceProvider;
    this.proxySettingsProvider = proxySettingsProvider;
  }

  @Override
  public MusicExtractorService get() {
    return provideMusicExtractorService(pipedApiServiceProvider.get(), invidiousApiServiceProvider.get(), proxySettingsProvider.get());
  }

  public static AppModule_ProvideMusicExtractorServiceFactory create(
      Provider<PipedApiService> pipedApiServiceProvider,
      Provider<InvidiousApiService> invidiousApiServiceProvider,
      Provider<ProxySettings> proxySettingsProvider) {
    return new AppModule_ProvideMusicExtractorServiceFactory(pipedApiServiceProvider, invidiousApiServiceProvider, proxySettingsProvider);
  }

  public static MusicExtractorService provideMusicExtractorService(PipedApiService pipedApiService,
      InvidiousApiService invidiousApiService, ProxySettings proxySettings) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMusicExtractorService(pipedApiService, invidiousApiService, proxySettings));
  }
}
