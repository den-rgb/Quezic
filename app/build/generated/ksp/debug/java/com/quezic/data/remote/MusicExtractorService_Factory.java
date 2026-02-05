package com.quezic.data.remote;

import com.quezic.data.local.ProxySettings;
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
public final class MusicExtractorService_Factory implements Factory<MusicExtractorService> {
  private final Provider<PipedApiService> pipedApiServiceProvider;

  private final Provider<InvidiousApiService> invidiousApiServiceProvider;

  private final Provider<ProxySettings> proxySettingsProvider;

  public MusicExtractorService_Factory(Provider<PipedApiService> pipedApiServiceProvider,
      Provider<InvidiousApiService> invidiousApiServiceProvider,
      Provider<ProxySettings> proxySettingsProvider) {
    this.pipedApiServiceProvider = pipedApiServiceProvider;
    this.invidiousApiServiceProvider = invidiousApiServiceProvider;
    this.proxySettingsProvider = proxySettingsProvider;
  }

  @Override
  public MusicExtractorService get() {
    return newInstance(pipedApiServiceProvider.get(), invidiousApiServiceProvider.get(), proxySettingsProvider.get());
  }

  public static MusicExtractorService_Factory create(
      Provider<PipedApiService> pipedApiServiceProvider,
      Provider<InvidiousApiService> invidiousApiServiceProvider,
      Provider<ProxySettings> proxySettingsProvider) {
    return new MusicExtractorService_Factory(pipedApiServiceProvider, invidiousApiServiceProvider, proxySettingsProvider);
  }

  public static MusicExtractorService newInstance(PipedApiService pipedApiService,
      InvidiousApiService invidiousApiService, ProxySettings proxySettings) {
    return new MusicExtractorService(pipedApiService, invidiousApiService, proxySettings);
  }
}
