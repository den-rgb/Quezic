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
public final class SoundCloudApiService_Factory implements Factory<SoundCloudApiService> {
  @Override
  public SoundCloudApiService get() {
    return newInstance();
  }

  public static SoundCloudApiService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SoundCloudApiService newInstance() {
    return new SoundCloudApiService();
  }

  private static final class InstanceHolder {
    private static final SoundCloudApiService_Factory INSTANCE = new SoundCloudApiService_Factory();
  }
}
