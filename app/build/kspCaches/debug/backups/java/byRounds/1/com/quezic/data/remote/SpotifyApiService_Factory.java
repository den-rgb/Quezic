package com.quezic.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class SpotifyApiService_Factory implements Factory<SpotifyApiService> {
  @Override
  public SpotifyApiService get() {
    return newInstance();
  }

  public static SpotifyApiService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SpotifyApiService newInstance() {
    return new SpotifyApiService();
  }

  private static final class InstanceHolder {
    private static final SpotifyApiService_Factory INSTANCE = new SpotifyApiService_Factory();
  }
}
